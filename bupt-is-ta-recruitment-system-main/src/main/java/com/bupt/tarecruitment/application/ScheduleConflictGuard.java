package com.bupt.tarecruitment.application;

import com.bupt.tarecruitment.common.schedule.ScheduleSlot;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 检查申请人与岗位排期之间的时间冲突。
 */
public final class ScheduleConflictGuard {
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;

    public ScheduleConflictGuard(
        ApplicationRepository applicationRepository,
        JobRepository jobRepository
    ) {
        this.applicationRepository = Objects.requireNonNull(applicationRepository);
        this.jobRepository = Objects.requireNonNull(jobRepository);
    }

    public void requireNoConflictWithAcceptedJobs(String applicantUserId, String targetJobId) {
        requireNoConflictWithAcceptedJobs(applicantUserId, targetJobId, null);
    }

    public void requireNoConflictWithAcceptedJobs(String applicantUserId, String targetJobId, String excludedApplicationId) {
        requireNonBlank(applicantUserId, "applicantUserId");
        requireNonBlank(targetJobId, "targetJobId");

        JobPosting targetJob = jobRepository.findByJobId(targetJobId.trim())
            .orElseThrow(() -> new IllegalArgumentException("No job exists for jobId: " + targetJobId));

        List<ScheduleSlot> targetSlots = parseSlots(targetJob);
        if (targetSlots.isEmpty()) {
            return;
        }

        List<String> conflicts = new ArrayList<>();
        for (JobApplication acceptedApplication : applicationRepository.findByApplicantUserId(applicantUserId.trim())) {
            if (acceptedApplication.status() != ApplicationStatus.ACCEPTED) {
                continue;
            }
            if (excludedApplicationId != null && excludedApplicationId.equals(acceptedApplication.applicationId())) {
                continue;
            }

            JobPosting acceptedJob = jobRepository.findByJobId(acceptedApplication.jobId())
                .orElseThrow(() -> new IllegalStateException(
                    "Accepted application references unknown jobId: " + acceptedApplication.jobId()
                ));

            List<ScheduleSlot> acceptedSlots = parseSlots(acceptedJob);
            for (ScheduleSlot targetSlot : targetSlots) {
                for (ScheduleSlot acceptedSlot : acceptedSlots) {
                    if (!targetSlot.overlaps(acceptedSlot)) {
                        continue;
                    }

                    conflicts.add(
                        acceptedJob.jobId()
                            + " | "
                            + acceptedJob.title()
                            + " | "
                            + targetSlot.overlapWith(acceptedSlot).format()
                    );
                }
            }
        }

        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException(
                "Schedule conflict with accepted TA assignment(s): " + String.join("; ", conflicts)
            );
        }
    }

    private List<ScheduleSlot> parseSlots(JobPosting jobPosting) {
        List<ScheduleSlot> parsedSlots = new ArrayList<>();
        for (String rawSlot : jobPosting.scheduleSlots()) {
            try {
                parsedSlots.add(ScheduleSlot.parse(rawSlot));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                    "Job "
                        + jobPosting.jobId()
                        + " contains invalid schedule data: "
                        + rawSlot
                );
            }
        }
        return parsedSlots;
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }
}
