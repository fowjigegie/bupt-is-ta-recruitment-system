package com.bupt.tarecruitment.application;

import com.bupt.tarecruitment.common.text.DisplayFormats;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;

import java.util.Objects;

/**
 * Blocks applications that would push an applicant above the weekly TA workload limit.
 */
final class WorkloadLimitGuard {
    static final double DEFAULT_WEEKLY_HOUR_LIMIT = 10.0;

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final double weeklyHourLimit;

    WorkloadLimitGuard(
        ApplicationRepository applicationRepository,
        JobRepository jobRepository
    ) {
        this(applicationRepository, jobRepository, DEFAULT_WEEKLY_HOUR_LIMIT);
    }

    WorkloadLimitGuard(
        ApplicationRepository applicationRepository,
        JobRepository jobRepository,
        double weeklyHourLimit
    ) {
        this.applicationRepository = Objects.requireNonNull(applicationRepository);
        this.jobRepository = Objects.requireNonNull(jobRepository);
        if (weeklyHourLimit <= 0) {
            throw new IllegalArgumentException("weeklyHourLimit must be greater than 0.");
        }
        this.weeklyHourLimit = weeklyHourLimit;
    }

    void requireWithinLimit(String applicantUserId, JobPosting targetJob) {
        requireNonBlank(applicantUserId, "applicantUserId");
        Objects.requireNonNull(targetJob);

        double acceptedWeeklyHours = applicationRepository.findByApplicantUserId(applicantUserId.trim()).stream()
            .filter(application -> application.status() == ApplicationStatus.ACCEPTED)
            .mapToDouble(application -> jobRepository.findByJobId(application.jobId())
                .map(JobPosting::weeklyHours)
                .orElseThrow(() -> new IllegalStateException(
                    "Accepted application references unknown jobId: " + application.jobId()
                )))
            .sum();

        double projectedWeeklyHours = acceptedWeeklyHours + targetJob.weeklyHours();
        if (projectedWeeklyHours > weeklyHourLimit) {
            throw new IllegalArgumentException(
                "Weekly workload limit exceeded. Current accepted workload is "
                    + DisplayFormats.formatDecimal(acceptedWeeklyHours)
                    + "h/week; this job adds "
                    + DisplayFormats.formatDecimal(targetJob.weeklyHours())
                    + "h/week; limit is "
                    + DisplayFormats.formatDecimal(weeklyHourLimit)
                    + "h/week."
            );
        }
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }
}
