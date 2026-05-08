package com.bupt.tarecruitment.mo;

import com.bupt.tarecruitment.application.ApplicationRepository;
import com.bupt.tarecruitment.application.ApplicationStatus;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;
import com.bupt.tarecruitment.job.JobStatus;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides analytics for jobs owned by one MO.
 */
public final class MoJobAnalyticsService {
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final MoJobQualityService jobQualityService;

    public MoJobAnalyticsService(
        JobRepository jobRepository,
        ApplicationRepository applicationRepository,
        MoJobQualityService jobQualityService
    ) {
        this.jobRepository = Objects.requireNonNull(jobRepository);
        this.applicationRepository = Objects.requireNonNull(applicationRepository);
        this.jobQualityService = Objects.requireNonNull(jobQualityService);
    }

    public MoJobAnalyticsSummary summarizeForOrganiser(String organiserId) {
        requireNonBlank(organiserId, "organiserId");

        List<JobPosting> ownedJobs = jobRepository.findAll().stream()
            .filter(job -> job.organiserId().equals(organiserId.trim()))
            .sorted(Comparator.comparing(JobPosting::jobId))
            .toList();

        Set<String> ownedJobIds = ownedJobs.stream()
            .map(JobPosting::jobId)
            .collect(Collectors.toSet());

        List<JobApplication> ownedApplications = applicationRepository.findAll().stream()
            .filter(application -> ownedJobIds.contains(application.jobId()))
            .toList();

        Map<String, List<JobApplication>> applicationsByJob = ownedApplications.stream()
            .collect(Collectors.groupingBy(JobApplication::jobId));

        List<MoJobAnalyticsRow> rows = ownedJobs.stream()
            .map(job -> toRow(job, applicationsByJob.getOrDefault(job.jobId(), List.of())))
            .sorted(Comparator
                .comparingLong(MoJobAnalyticsRow::applicationCount).reversed()
                .thenComparing(MoJobAnalyticsRow::jobId))
            .toList();

        long totalJobs = ownedJobs.size();
        long openJobs = ownedJobs.stream().filter(job -> job.status() == JobStatus.OPEN).count();
        long closedJobs = ownedJobs.stream().filter(job -> job.status() == JobStatus.CLOSED).count();
        long totalApplications = ownedApplications.size();

        return new MoJobAnalyticsSummary(
            organiserId.trim(),
            totalJobs,
            openJobs,
            closedJobs,
            totalApplications,
            countStatus(ownedApplications, ApplicationStatus.SUBMITTED),
            countStatus(ownedApplications, ApplicationStatus.SHORTLISTED),
            countStatus(ownedApplications, ApplicationStatus.ACCEPTED),
            countStatus(ownedApplications, ApplicationStatus.REJECTED),
            countStatus(ownedApplications, ApplicationStatus.WITHDRAWN),
            totalJobs == 0 ? 0 : totalApplications / (double) totalJobs,
            rows
        );
    }

    private MoJobAnalyticsRow toRow(JobPosting job, List<JobApplication> applications) {
        long applicationCount = applications.size();
        long acceptedCount = countStatus(applications, ApplicationStatus.ACCEPTED);
        int acceptanceRate = applicationCount == 0 ? 0 : (int) Math.round(acceptedCount * 100.0 / applicationCount);
        int qualityScore = jobQualityService.analyze(job).qualityScore();

        return new MoJobAnalyticsRow(
            job.jobId(),
            job.title(),
            job.status(),
            applicationCount,
            acceptedCount,
            acceptanceRate,
            job.weeklyHours(),
            qualityScore
        );
    }

    private static long countStatus(List<JobApplication> applications, ApplicationStatus status) {
        return applications.stream().filter(application -> application.status() == status).count();
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }
}
