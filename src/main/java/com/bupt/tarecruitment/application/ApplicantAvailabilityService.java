package com.bupt.tarecruitment.application;

import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.applicant.ApplicantProfileRepository;
import com.bupt.tarecruitment.common.schedule.ScheduleSlot;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 检查申请人的可用时间是否覆盖岗位排期。
 */
public final class ApplicantAvailabilityService {
    private final ApplicantProfileRepository profileRepository;
    private final JobRepository jobRepository;

    public ApplicantAvailabilityService(
        ApplicantProfileRepository profileRepository,
        JobRepository jobRepository
    ) {
        this.profileRepository = Objects.requireNonNull(profileRepository);
        this.jobRepository = Objects.requireNonNull(jobRepository);
    }

    public Optional<AvailabilityCheckResult> availabilityForApplicantAndJob(String applicantUserId, String jobId) {
        requireNonBlank(applicantUserId, "applicantUserId");
        requireNonBlank(jobId, "jobId");

        Optional<ApplicantProfile> profile = profileRepository.findByUserId(applicantUserId.trim());
        if (profile.isEmpty()) {
            return Optional.empty();
        }

        JobPosting job = jobRepository.findByJobId(jobId.trim())
            .orElseThrow(() -> new IllegalArgumentException("No job exists for jobId: " + jobId));

        return Optional.of(analyze(profile.get(), job));
    }

    AvailabilityCheckResult analyze(ApplicantProfile profile, JobPosting job) {
        List<ScheduleSlot> availabilitySlots = parseAvailabilitySlots(profile);
        List<String> coveredJobSlots = new ArrayList<>();
        List<String> uncoveredJobSlots = new ArrayList<>();

        for (String rawJobSlot : job.scheduleSlots()) {
            ScheduleSlot jobSlot = parseJobSlot(job.jobId(), rawJobSlot);
            boolean covered = availabilitySlots.stream().anyMatch(slot -> slot.covers(jobSlot));
            if (covered) {
                coveredJobSlots.add(jobSlot.format());
            } else {
                uncoveredJobSlots.add(jobSlot.format());
            }
        }

        return new AvailabilityCheckResult(job.jobId(), coveredJobSlots, uncoveredJobSlots);
    }

    public void requireApplicantAvailability(ApplicantProfile profile, JobPosting job) {
        AvailabilityCheckResult result = analyze(profile, job);
        if (!result.fitsAvailability()) {
            throw new IllegalArgumentException(
                "Job schedule is outside your current availability: "
                    + String.join(", ", result.uncoveredJobSlots())
                    + ". Update your profile in Resume Database before applying."
            );
        }
    }

    private List<ScheduleSlot> parseAvailabilitySlots(ApplicantProfile profile) {
        List<ScheduleSlot> parsedSlots = new ArrayList<>();
        for (String rawSlot : profile.availabilitySlots()) {
            try {
                parsedSlots.add(ScheduleSlot.parse(rawSlot));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                    "Applicant profile contains invalid availability data: " + rawSlot
                );
            }
        }
        return parsedSlots;
    }

    private static ScheduleSlot parseJobSlot(String jobId, String rawSlot) {
        try {
            return ScheduleSlot.parse(rawSlot);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Job " + jobId + " contains invalid schedule data: " + rawSlot);
        }
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }
}
