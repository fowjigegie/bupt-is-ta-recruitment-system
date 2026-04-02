package com.bupt.tarecruitment.admin;

import com.bupt.tarecruitment.application.ApplicationRepository;
import com.bupt.tarecruitment.application.ApplicationStatus;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.auth.UserAccount;
import com.bupt.tarecruitment.auth.UserRepository;
import com.bupt.tarecruitment.common.schedule.ScheduleSlot;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class AdminWorkloadService {
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    public AdminWorkloadService(
        ApplicationRepository applicationRepository,
        JobRepository jobRepository,
        UserRepository userRepository
    ) {
        this.applicationRepository = Objects.requireNonNull(applicationRepository);
        this.jobRepository = Objects.requireNonNull(jobRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    public List<WorkloadSummary> listAcceptedTaWorkloads(int weeklyHourLimit) {
        validateWeeklyHourLimit(weeklyHourLimit);

        Map<String, List<JobApplication>> acceptedApplicationsByApplicant = applicationRepository.findAll().stream()
            .filter(application -> application.status() == ApplicationStatus.ACCEPTED)
            .collect(Collectors.groupingBy(JobApplication::applicantUserId));

        return acceptedApplicationsByApplicant.entrySet().stream()
            .map(entry -> buildSummary(entry.getKey(), entry.getValue(), weeklyHourLimit))
            .sorted(Comparator.comparing(WorkloadSummary::applicantUserId))
            .toList();
    }

    public Optional<WorkloadSummary> getAcceptedTaWorkload(String applicantUserId, int weeklyHourLimit) {
        if (applicantUserId == null || applicantUserId.isBlank()) {
            throw new IllegalArgumentException("applicantUserId must not be blank.");
        }

        validateWeeklyHourLimit(weeklyHourLimit);

        List<JobApplication> acceptedApplications = applicationRepository.findByApplicantUserId(applicantUserId.trim()).stream()
            .filter(application -> application.status() == ApplicationStatus.ACCEPTED)
            .toList();

        if (acceptedApplications.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(buildSummary(applicantUserId.trim(), acceptedApplications, weeklyHourLimit));
    }

    private WorkloadSummary buildSummary(String applicantUserId, List<JobApplication> acceptedApplications, int weeklyHourLimit) {
        List<AcceptedAssignment> acceptedAssignments = acceptedApplications.stream()
            .map(this::toAcceptedAssignment)
            .sorted(Comparator.comparing(AcceptedAssignment::jobId))
            .toList();

        int totalWeeklyHours = acceptedAssignments.stream()
            .mapToInt(AcceptedAssignment::weeklyHours)
            .sum();

        List<WorkloadConflict> conflicts = detectConflicts(acceptedAssignments);
        String applicantDisplayName = userRepository.findByUserId(applicantUserId)
            .map(UserAccount::displayName)
            .filter(displayName -> !displayName.isBlank())
            .orElse(applicantUserId);

        return new WorkloadSummary(
            applicantUserId,
            applicantDisplayName,
            totalWeeklyHours,
            acceptedAssignments,
            conflicts,
            totalWeeklyHours > weeklyHourLimit,
            !conflicts.isEmpty()
        );
    }

    private AcceptedAssignment toAcceptedAssignment(JobApplication application) {
        JobPosting jobPosting = jobRepository.findByJobId(application.jobId())
            .orElseThrow(() -> new IllegalStateException("Accepted application references unknown jobId: " + application.jobId()));

        return new AcceptedAssignment(
            jobPosting.jobId(),
            jobPosting.title(),
            jobPosting.moduleOrActivity(),
            jobPosting.weeklyHours(),
            jobPosting.scheduleSlots()
        );
    }

    private List<WorkloadConflict> detectConflicts(List<AcceptedAssignment> acceptedAssignments) {
        Set<WorkloadConflict> conflicts = new LinkedHashSet<>();

        for (int leftIndex = 0; leftIndex < acceptedAssignments.size(); leftIndex++) {
            AcceptedAssignment leftAssignment = acceptedAssignments.get(leftIndex);
            for (int rightIndex = leftIndex + 1; rightIndex < acceptedAssignments.size(); rightIndex++) {
                AcceptedAssignment rightAssignment = acceptedAssignments.get(rightIndex);
                conflicts.addAll(findConflictsBetween(leftAssignment, rightAssignment));
            }
        }

        return conflicts.stream()
            .sorted(Comparator
                .comparing(WorkloadConflict::overlapSlot)
                .thenComparing(WorkloadConflict::jobIdA)
                .thenComparing(WorkloadConflict::jobIdB))
            .toList();
    }

    private List<WorkloadConflict> findConflictsBetween(
        AcceptedAssignment leftAssignment,
        AcceptedAssignment rightAssignment
    ) {
        List<WorkloadConflict> conflicts = new ArrayList<>();

        for (String leftSlotRaw : leftAssignment.scheduleSlots()) {
            ScheduleSlot leftSlot = ScheduleSlot.parse(leftSlotRaw);
            for (String rightSlotRaw : rightAssignment.scheduleSlots()) {
                ScheduleSlot rightSlot = ScheduleSlot.parse(rightSlotRaw);
                if (!leftSlot.overlaps(rightSlot)) {
                    continue;
                }

                ScheduleSlot overlapSlot = leftSlot.overlapWith(rightSlot);
                conflicts.add(new WorkloadConflict(
                    leftAssignment.jobId(),
                    leftAssignment.title(),
                    rightAssignment.jobId(),
                    rightAssignment.title(),
                    overlapSlot.format()
                ));
            }
        }

        return conflicts;
    }

    private void validateWeeklyHourLimit(int weeklyHourLimit) {
        if (weeklyHourLimit <= 0) {
            throw new IllegalArgumentException("weeklyHourLimit must be greater than 0.");
        }
    }
}
