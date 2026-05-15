package com.bupt.tarecruitment.mo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.bupt.tarecruitment.application.ApplicationRepository;
import com.bupt.tarecruitment.common.schedule.ScheduleSlot;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;
import com.bupt.tarecruitment.job.JobStatus;

/**
 * Audits MO job postings before/after publishing and gives quality scores.
 */
public final class MoJobQualityService {
    private static final double IDEAL_WEEKLY_HOURS = 3.0;
    private static final double MODERATE_WEEKLY_HOURS = 4.5;
    private static final double HIGH_WEEKLY_HOURS = 6.0;
    private static final double MAX_WEEKLY_HOURS = 7.5;

    private static final int GOOD_DESCRIPTION_LENGTH = 20;
    private static final int BASIC_DESCRIPTION_LENGTH = 15;
    private static final int LIMITED_DESCRIPTION_LENGTH = 10;
    private static final int SHORT_DESCRIPTION_LENGTH = 5;

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
        return analyze(job, true);
    }

    /**
     * Analyzes an unsaved draft from the Post Vacancies form.
     * Demand-dependent checks are skipped because the job does not exist in the repository yet.
     */
    public JobQualityReport analyzeDraft(JobPosting job) {
        return analyze(job, false);
    }

    private JobQualityReport analyze(JobPosting job, boolean includeDemandChecks) {
        Objects.requireNonNull(job);
        List<JobQualityIssue> issues = new ArrayList<>();

        if (job.title().isBlank()) {
            issues.add(new JobQualityIssue("CRITICAL", "MISSING_TITLE", "Job title is missing."));
        }

        if (job.moduleOrActivity().isBlank()) {
            issues.add(new JobQualityIssue("CRITICAL", "MISSING_MODULE", "Module or activity is missing."));
        }

        int descriptionLength = job.description().trim().length();
        if (descriptionLength < SHORT_DESCRIPTION_LENGTH) {
            issues.add(new JobQualityIssue(
                "CRITICAL",
                "VERY_SHORT_DESCRIPTION",
                "Description is too short; add clear duties, expectations, and assessment details."
            ));
        } else if (descriptionLength < LIMITED_DESCRIPTION_LENGTH) {
            issues.add(new JobQualityIssue(
                "MAJOR",
                "SHORT_DESCRIPTION",
                "Description needs more detail about duties and expectations."
            ));
        } else if (descriptionLength < BASIC_DESCRIPTION_LENGTH) {
            issues.add(new JobQualityIssue(
                "WARNING",
                "LIMITED_DESCRIPTION",
                "Description could include more responsibilities and requirements."
            ));
        } else if (descriptionLength < GOOD_DESCRIPTION_LENGTH) {
            issues.add(new JobQualityIssue(
                "MINOR",
                "BASIC_DESCRIPTION",
                "Description is acceptable but could be improved with more detail."
            ));
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
            issues.add(new JobQualityIssue(
                "CRITICAL",
                "INVALID_WEEKLY_HOURS",
                "Weekly hours must be greater than 0."
            ));
        } else if (job.weeklyHours() > MAX_WEEKLY_HOURS) {
            issues.add(new JobQualityIssue(
                "CRITICAL",
                "EXCESSIVE_WEEKLY_HOURS",
                "Weekly hours exceed the maximum allowed limit of 7.5 hours."
            ));
        } else if (job.weeklyHours() > HIGH_WEEKLY_HOURS) {
            issues.add(new JobQualityIssue(
                "MAJOR",
                "VERY_HIGH_WEEKLY_HOURS",
                "Weekly workload is very high for a TA role."
            ));
        } else if (job.weeklyHours() > MODERATE_WEEKLY_HOURS) {
            issues.add(new JobQualityIssue(
                "WARNING",
                "HIGH_WEEKLY_HOURS",
                "Weekly workload may reduce applicant availability."
            ));
        } else if (job.weeklyHours() > IDEAL_WEEKLY_HOURS) {
            issues.add(new JobQualityIssue(
                "MINOR",
                "MODERATE_WEEKLY_HOURS",
                "Weekly workload is slightly above the ideal range."
            ));
        }

        if (includeDemandChecks && job.status() == JobStatus.OPEN && applicationCount(job.jobId()) == 0) {
            issues.add(new JobQualityIssue("INFO", "NO_APPLICATIONS", "Open job currently has no applications."));
        }

        int score = 100;
        for (JobQualityIssue issue : issues) {
            score -= switch (issue.severity()) {
                case "CRITICAL" -> 25;
                case "MAJOR" -> 12;
                case "WARNING" -> 8;
                case "MINOR" -> 4;
                default -> 0;
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
