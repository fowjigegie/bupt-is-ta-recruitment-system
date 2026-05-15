package com.bupt.tarecruitment.mo;

import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.applicant.ApplicantProfileRepository;
import com.bupt.tarecruitment.application.ApplicationRepository;
import com.bupt.tarecruitment.application.ApplicationStatus;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.communication.MessageService;
import com.bupt.tarecruitment.common.schedule.ScheduleSlot;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * MO interview scheduling: conflict checks, fill-in message templates, send chat message.
 * Audit trail for sends uses {@link MoDecisionLogService} only ({@code mo_decision_log.txt}, action {@code INTERVIEW_SCHEDULED}).
 */
public final class MoInterviewScheduleService {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd (EEE)", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicantProfileRepository profileRepository;
    private final MessageService messageService;
    private final MoDecisionLogService decisionLogService;

    public MoInterviewScheduleService(
        JobRepository jobRepository,
        ApplicationRepository applicationRepository,
        ApplicantProfileRepository profileRepository,
        MessageService messageService,
        MoDecisionLogService decisionLogService
    ) {
        this.jobRepository = Objects.requireNonNull(jobRepository);
        this.applicationRepository = Objects.requireNonNull(applicationRepository);
        this.profileRepository = Objects.requireNonNull(profileRepository);
        this.messageService = Objects.requireNonNull(messageService);
        this.decisionLogService = Objects.requireNonNull(decisionLogService);
    }

    public static List<InterviewMessageTemplate> defaultTemplates() {
        return List.of(
            new InterviewMessageTemplate(
                "standard",
                "Standard invitation",
                """
                    Dear {{applicantName}},

                    We would like to invite you for an interview about the TA role: {{jobTitle}}.

                    Date: {{date}}
                    Time: {{startTime}} – {{endTime}}
                    Location: {{location}}
                    {{notesBlock}}
                    Please reply in this chat if you need to reschedule.

                    Best regards,
                    {{moName}}
                    """.trim()
            ),
            new InterviewMessageTemplate(
                "short",
                "Short reminder style",
                    """
                    Hi {{applicantName}}, quick note on {{jobTitle}}: interview on {{date}} at {{startTime}}–{{endTime}}, location: {{location}}.
                    {{notesBlock}}
                    — {{moName}}
                    """.trim()
            )
        );
    }

    public ScheduleSlot toInterviewSlot(LocalDate date, LocalTime start, LocalTime end) {
        Objects.requireNonNull(date);
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        String day = dayCode(date.getDayOfWeek());
        return new ScheduleSlot(day, start, end);
    }

    public InterviewConflictReport checkConflicts(
        String organiserId,
        String jobId,
        String applicantUserId,
        ScheduleSlot interviewSlot
    ) {
        requireNonBlank(organiserId, "organiserId");
        requireNonBlank(jobId, "jobId");
        requireNonBlank(applicantUserId, "applicantUserId");
        Objects.requireNonNull(interviewSlot);

        List<String> blocking = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        JobPosting job = jobRepository.findByJobId(jobId.trim())
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        if (!job.organiserId().equals(organiserId.trim())) {
            blocking.add("You can only schedule interviews for jobs that you own.");
        }

        Optional<JobApplication> applicationOpt = findApplication(jobId.trim(), applicantUserId.trim());
        if (applicationOpt.isEmpty()) {
            warnings.add("No application record found for this applicant on this job (message will still be sent).");
        } else if (applicationOpt.get().status() == ApplicationStatus.WITHDRAWN) {
            blocking.add("This application has been withdrawn; scheduling an interview is not recommended.");
        }

        appendAcceptedJobConflicts(applicantUserId.trim(), interviewSlot, null, blocking);
        appendJobSlotConflicts(job, interviewSlot, blocking);
        appendAvailabilityWarnings(applicantUserId.trim(), interviewSlot, warnings);

        return new InterviewConflictReport(blocking, warnings);
    }

    public String renderMessage(String templateBody, InterviewTemplateVariables variables) {
        Objects.requireNonNull(templateBody);
        Objects.requireNonNull(variables);
        String notesBlock = variables.notes().isBlank()
            ? ""
            : "Notes:\n" + variables.notes().trim() + "\n\n";

        Map<String, String> map = new LinkedHashMap<>();
        map.put("applicantName", variables.applicantName());
        map.put("moName", variables.moName());
        map.put("jobTitle", variables.jobTitle());
        map.put("jobId", variables.jobId());
        map.put("date", variables.dateText());
        map.put("startTime", variables.startTimeText());
        map.put("endTime", variables.endTimeText());
        map.put("location", variables.location());
        map.put("notes", variables.notes());
        map.put("notesBlock", notesBlock);

        String result = templateBody;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return result.trim();
    }

    public void sendInterviewInvitation(
        String organiserId,
        String jobId,
        String applicantUserId,
        LocalDate date,
        LocalTime start,
        LocalTime end,
        String location,
        String notes,
        String templateBody,
        InterviewTemplateVariables variables
    ) {
        requireNonBlank(organiserId, "organiserId");
        requireNonBlank(jobId, "jobId");
        requireNonBlank(applicantUserId, "applicantUserId");
        requireNonBlank(location, "location");
        Objects.requireNonNull(date);
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        Objects.requireNonNull(templateBody);

        ScheduleSlot slot = toInterviewSlot(date, start, end);
        InterviewConflictReport report = checkConflicts(organiserId, jobId, applicantUserId, slot);
        if (report.hasBlockingIssues()) {
            throw new IllegalArgumentException(String.join(System.lineSeparator(), report.blockingErrors()));
        }

        String body = renderMessage(templateBody, variables);
        messageService.sendMessage(jobId.trim(), organiserId.trim(), applicantUserId.trim(), body);

        String applicationId = findApplication(jobId.trim(), applicantUserId.trim())
            .map(JobApplication::applicationId)
            .orElse("");

        String detail = slot.format()
            + " | "
            + date.format(DATE_FMT)
            + " "
            + start.format(TIME_FMT)
            + "-"
            + end.format(TIME_FMT)
            + " | "
            + location.trim();
        decisionLogService.record(
            organiserId.trim(),
            jobId.trim(),
            applicationId,
            applicantUserId.trim(),
            "INTERVIEW_SCHEDULED",
            detail
        );
    }

    private Optional<JobApplication> findApplication(String jobId, String applicantUserId) {
        return applicationRepository.findAll().stream()
            .filter(application -> application.jobId().equals(jobId) && application.applicantUserId().equals(applicantUserId))
            .findFirst();
    }

    private void appendAcceptedJobConflicts(
        String applicantUserId,
        ScheduleSlot interviewSlot,
        String excludedApplicationId,
        List<String> blocking
    ) {
        for (JobApplication acceptedApplication : applicationRepository.findByApplicantUserId(applicantUserId)) {
            if (acceptedApplication.status() != ApplicationStatus.ACCEPTED) {
                continue;
            }
            if (excludedApplicationId != null && excludedApplicationId.equals(acceptedApplication.applicationId())) {
                continue;
            }
            JobPosting acceptedJob = jobRepository.findByJobId(acceptedApplication.jobId()).orElse(null);
            if (acceptedJob == null) {
                continue;
            }
            for (String rawSlot : acceptedJob.scheduleSlots()) {
                ScheduleSlot acceptedSlot = tryParse(rawSlot);
                if (acceptedSlot == null) {
                    continue;
                }
                if (interviewSlot.overlaps(acceptedSlot)) {
                    blocking.add(
                        "Interview overlaps an accepted TA assignment ("
                            + acceptedJob.title()
                            + ", "
                            + interviewSlot.overlapWith(acceptedSlot).format()
                            + "). Choose another time."
                    );
                    return;
                }
            }
        }
    }

    private void appendJobSlotConflicts(JobPosting job, ScheduleSlot interviewSlot, List<String> blocking) {
        for (String rawSlot : job.scheduleSlots()) {
            ScheduleSlot jobSlot = tryParse(rawSlot);
            if (jobSlot == null) {
                continue;
            }
            if (interviewSlot.overlaps(jobSlot)) {
                blocking.add(
                    "Interview overlaps this job's posted TA session time ("
                        + jobSlot.format()
                        + "). Pick a slot outside scheduled teaching hours."
                );
                return;
            }
        }
    }

    private void appendAvailabilityWarnings(String applicantUserId, ScheduleSlot interviewSlot, List<String> warnings) {
        Optional<ApplicantProfile> profileOpt = profileRepository.findByUserId(applicantUserId);
        if (profileOpt.isEmpty()) {
            warnings.add("Applicant profile not found; availability could not be checked.");
            return;
        }
        List<ScheduleSlot> availabilitySlots = new ArrayList<>();
        for (String raw : profileOpt.get().availabilitySlots()) {
            ScheduleSlot parsed = tryParse(raw);
            if (parsed != null) {
                availabilitySlots.add(parsed);
            }
        }
        if (availabilitySlots.isEmpty()) {
            warnings.add("Applicant has no availability slots on profile; ensure they can attend.");
            return;
        }
        boolean covered = availabilitySlots.stream().anyMatch(slot -> slot.covers(interviewSlot));
        if (!covered) {
            warnings.add(
                "Interview time ("
                    + interviewSlot.format()
                    + ") is not fully covered by the applicant's saved weekly availability. Confirm with the applicant."
            );
        }
    }

    private static ScheduleSlot tryParse(String raw) {
        try {
            return ScheduleSlot.parse(raw);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String dayCode(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "MON";
            case TUESDAY -> "TUE";
            case WEDNESDAY -> "WED";
            case THURSDAY -> "THU";
            case FRIDAY -> "FRI";
            case SATURDAY -> "SAT";
            case SUNDAY -> "SUN";
        };
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }

    public record InterviewMessageTemplate(String id, String label, String body) {
    }

    public record InterviewConflictReport(List<String> blockingErrors, List<String> warnings) {
        public InterviewConflictReport {
            blockingErrors = List.copyOf(blockingErrors);
            warnings = List.copyOf(warnings);
        }

        public boolean hasBlockingIssues() {
            return !blockingErrors.isEmpty();
        }
    }

    public record InterviewTemplateVariables(
        String applicantName,
        String moName,
        String jobTitle,
        String jobId,
        String dateText,
        String startTimeText,
        String endTimeText,
        String location,
        String notes
    ) {
    }
}
