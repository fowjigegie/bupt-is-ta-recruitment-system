package com.bupt.tarecruitment.application;

import com.bupt.tarecruitment.auth.UserAccessPolicy;
import com.bupt.tarecruitment.auth.UserRepository;
import com.bupt.tarecruitment.auth.UserRole;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;

import java.util.Objects;
import java.util.Set;

public final class ApplicationDecisionService {
    private static final Set<ApplicationStatus> ALLOWED_TARGET_STATUSES = Set.of(
        ApplicationStatus.SHORTLISTED,
        ApplicationStatus.ACCEPTED,
        ApplicationStatus.REJECTED
    );

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserAccessPolicy userAccessPolicy;
    private final ScheduleConflictGuard scheduleConflictGuard;

    public ApplicationDecisionService(
        ApplicationRepository applicationRepository,
        JobRepository jobRepository,
        UserRepository userRepository
    ) {
        this.applicationRepository = Objects.requireNonNull(applicationRepository);
        this.jobRepository = Objects.requireNonNull(jobRepository);
        this.userAccessPolicy = new UserAccessPolicy(Objects.requireNonNull(userRepository));
        this.scheduleConflictGuard = new ScheduleConflictGuard(applicationRepository, jobRepository);
    }

    public JobApplication updateStatus(
        String organiserUserId,
        String applicationId,
        ApplicationStatus nextStatus,
        String reviewerNote
    ) {
        requireNonBlank(organiserUserId, "organiserUserId");
        requireNonBlank(applicationId, "applicationId");
        Objects.requireNonNull(nextStatus, "nextStatus must not be null.");

        if (!ALLOWED_TARGET_STATUSES.contains(nextStatus)) {
            throw new IllegalArgumentException("Unsupported target status for organiser review: " + nextStatus);
        }

        userAccessPolicy.requireActiveUserWithRole(organiserUserId, UserRole.MO);

        JobApplication existingApplication = applicationRepository.findByApplicationId(applicationId)
            .orElseThrow(() -> new IllegalArgumentException("Application not found."));

        if (existingApplication.status() == ApplicationStatus.WITHDRAWN) {
            throw new IllegalArgumentException("Withdrawn applications cannot be reviewed.");
        }

        JobPosting ownedJob = jobRepository.findByJobId(existingApplication.jobId())
            .orElseThrow(() -> new IllegalArgumentException("Job not found for application: " + existingApplication.jobId()));

        if (!ownedJob.organiserId().equals(organiserUserId)) {
            throw new IllegalArgumentException("You can only review applications for your own jobs.");
        }

        if (nextStatus == ApplicationStatus.ACCEPTED) {
            scheduleConflictGuard.requireNoConflictWithAcceptedJobs(
                existingApplication.applicantUserId(),
                existingApplication.jobId(),
                existingApplication.applicationId()
            );
        }

        JobApplication updatedApplication = new JobApplication(
            existingApplication.applicationId(),
            existingApplication.jobId(),
            existingApplication.applicantUserId(),
            existingApplication.cvId(),
            nextStatus,
            existingApplication.submittedAt(),
            normalizeNote(reviewerNote)
        );

        applicationRepository.save(updatedApplication);
        return updatedApplication;
    }

    private String normalizeNote(String reviewerNote) {
        if (reviewerNote == null) {
            return "";
        }
        return reviewerNote.trim();
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }
}
