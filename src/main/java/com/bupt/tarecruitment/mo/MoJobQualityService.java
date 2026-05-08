package com.bupt.tarecruitment.mo;

import com.bupt.tarecruitment.application.ApplicationRepository;
import com.bupt.tarecruitment.common.schedule.ScheduleSlot;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;
import com.bupt.tarecruitment.job.JobStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Audits MO job postings before/after publishing and gives quality scores.
 */
public final class MoJobQualityService {
    private static final double HIGH_WEEKLY_HOURS = 10.0;
    private static final int MIN_DESCRIPTION_LENGTH = 40;

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;

    public MoJobQualityService(JobRepository jobRepository, ApplicationRepository applicationRepository) {
        this.jobRepository = Objects.requireNonNull(jobRepository);
        this.applicationRepository = Objects.requireNonNull(applicationRepository);
    }

    public JobQualityReport analyzeJob(String jobId) {
        requireNonBlank(jobId, "jobId");
        JobPosting job = jobRepository.findByJobId(jobId.trim())
            .orElseThrow(() -> new IllegalArgumentException("No job exists for jobId: " + jobId));
        return analyze(job);
    }

    public List<JobQualityReport> analyzeJobsForOrganiser(String organiserId) {
        requireNonBlank(organiserId, "organiserId");
        return jobRepository.findAll().stream()
            .filter(job -> job.organiserId().equals(organiserId.trim()))
            .map(this::analyze)
            .sorted(Comparator.comparingInt(JobQualityReport::qualityScore).thenComparing(JobQualityReport::jobId))
            .toList();
    }

    public JobQualityReport analyze(JobPosting job) {
        Objects.requireNonNull(job);
        List<JobQualityIssue> issues = new ArrayList<>();

        if (job.title().isBlank()) {
            issues.add(new JobQualityIssue("CRITICAL", "MISSING_TITLE", "Job title is missing."));
        }
        if (job.moduleOrActivity().isBlank()) {
            issues.add(new JobQualityIssue("CRITICAL", "MISSING_MODULE", "Module or activity is missing."));
        }
        if (job.description().trim().length() < MIN_DESCRIPTION_LENGTH) {
            issues.add(new JobQualityIssue("WARNING", "SHORT_DESCRIPTION", "Description is short; add duties, expectations, and assessment details."));
        }
        if (job.requiredSkills().isEmpty()) {
            issues.add(new JobQualityIssue("WARNING", "NO_REQUIRED_SKILLS", "No required skills are listed."));
        }
        if (job.scheduleSlots().isEmpty()) {
            issues.add(new JobQualityIssue("CRITICAL", "MISSING_SCHEDULE", "No schedule slots are configured."));
        }
        for (String rawSlot : job.scheduleSlots()) {
            try {
                ScheduleSlot.parse(rawSlot);
            } catch (IllegalArgumentException exception) {
                issues.add(new JobQualityIssue("CRITICAL", "INVALID_SCHEDULE", "Invalid schedule slot: " + rawSlot));
            }
        }
        if (job.weeklyHours() <= 0) {
            issues.add(new JobQualityIssue("CRITICAL", "INVALID_WEEKLY_HOURS", "Weekly hours must be greater than 0."));
        } else if (job.weeklyHours() > HIGH_WEEKLY_HOURS) {
            issues.add(new JobQualityIssue("WARNING", "HIGH_WEEKLY_HOURS", "Weekly hours are high and may reduce applicant availability."));
        }
        if (job.status() == JobStatus.OPEN && applicationCount(job.jobId()) == 0) {
            issues.add(new JobQualityIssue("INFO", "NO_APPLICATIONS", "Open job currently has no applications."));
        }

        int score = 100;
        for (JobQualityIssue issue : issues) {
            score -= switch (issue.severity()) {
                case "CRITICAL" -> 25;
                case "WARNING" -> 12;
                default -> 5;
            };
        }
        score = Math.max(0, score);

        boolean ready = issues.stream().noneMatch(issue -> issue.severity().equals("CRITICAL"));
        return new JobQualityReport(job.jobId(), job.title(), score, ready, issues);
    }

    private long applicationCount(String jobId) {
        return applicationRepository.findAll().stream()
            .filter(application -> application.jobId().equals(jobId))
            .count();
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }
}
