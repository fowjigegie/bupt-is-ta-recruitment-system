package com.bupt.tarecruitment.application;

import java.time.LocalDateTime;
import java.util.Objects;

import com.bupt.tarecruitment.applicant.ApplicantCv;
import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.applicant.ApplicantCvRepository;
import com.bupt.tarecruitment.applicant.ApplicantProfileRepository;
import com.bupt.tarecruitment.auth.UserAccessPolicy;
import com.bupt.tarecruitment.auth.UserRepository;
import com.bupt.tarecruitment.auth.UserRole;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;
import com.bupt.tarecruitment.job.JobStatus;

/**
 * 处理投递、撤回和查询等申请业务。
 */
public final class JobApplicationService {

    /** Repository for job postings */
    private final JobRepository jobRepository;

    /** Repository for applications */
    private final ApplicationRepository applicationRepository;

    /** Generator for application IDs */
    private final ApplicationIdGenerator applicationIdGenerator;

    /** Repository for applicant profiles */
    private final ApplicantProfileRepository profileRepository;

    /** Repository for applicant CVs */
    private final ApplicantCvRepository cvRepository;

    /** Access control policy for validating users */
    private final UserAccessPolicy userAccessPolicy;

    /** Guard to prevent schedule conflicts */
    private final ScheduleConflictGuard scheduleConflictGuard;

    /** Service to verify applicant availability covers the selected job */
    private final ApplicantAvailabilityService applicantAvailabilityService;

    /**
     * Constructor without user repository (uses no-op access policy)
     */
    public JobApplicationService(
        JobRepository jobRepository,
        ApplicationRepository applicationRepository,
        ApplicationIdGenerator applicationIdGenerator,
        ApplicantProfileRepository profileRepository,
        ApplicantCvRepository cvRepository
    ) {
        this(
            jobRepository,
            applicationRepository,
            applicationIdGenerator,
            profileRepository,
            cvRepository,
            UserAccessPolicy.noOp()
        );
    }

    /**
     * Constructor with user repository (enables access control)
     */
    public JobApplicationService(
        JobRepository jobRepository,
        ApplicationRepository applicationRepository,
        ApplicationIdGenerator applicationIdGenerator,
        ApplicantProfileRepository profileRepository,
        ApplicantCvRepository cvRepository,
        UserRepository userRepository
    ) {
        this(
            jobRepository,
            applicationRepository,
            applicationIdGenerator,
            profileRepository,
            cvRepository,
            new UserAccessPolicy(userRepository)
        );
    }

    /**
     * Internal constructor initializing all dependencies
     */
    private JobApplicationService(
        JobRepository jobRepository,
        ApplicationRepository applicationRepository,
        ApplicationIdGenerator applicationIdGenerator,
        ApplicantProfileRepository profileRepository,
        ApplicantCvRepository cvRepository,
        UserAccessPolicy userAccessPolicy
    ) {
        this.jobRepository = Objects.requireNonNull(jobRepository);
        this.applicationRepository = Objects.requireNonNull(applicationRepository);
        this.applicationIdGenerator = Objects.requireNonNull(applicationIdGenerator);
        this.profileRepository = Objects.requireNonNull(profileRepository);
        this.cvRepository = Objects.requireNonNull(cvRepository);
        this.userAccessPolicy = Objects.requireNonNull(userAccessPolicy);

        // Initialize schedule conflict guard
        this.scheduleConflictGuard = new ScheduleConflictGuard(applicationRepository, jobRepository);
        this.applicantAvailabilityService = new ApplicantAvailabilityService(profileRepository, jobRepository);
    }

    /**
     * Apply to a job using a specific CV.
     *
     * Steps:
     * 1. Validate inputs
     * 2. Check user role and active status
     * 3. Ensure applicant profile exists
     * 4. Validate job existence and status
     * 5. Prevent duplicate applications
     * 6. Verify CV ownership
     * 7. Check schedule conflicts
     * 8. Create and save application
     */
    public JobApplication applyToJobWithCv(String applicantUserId, String jobId, String cvId) {

        // Validate input parameters
        requireNonBlank(applicantUserId, "applicantUserId");
        requireNonBlank(jobId, "jobId");
        requireNonBlank(cvId, "cvId");

        // Ensure the user is an active applicant
        userAccessPolicy.requireActiveUserWithRole(applicantUserId, UserRole.APPLICANT);

        // Check if applicant profile exists
        ApplicantProfile profile = profileRepository.findByUserId(applicantUserId)
            .orElseThrow(() -> new IllegalArgumentException("No applicant profile exists for userId: " + applicantUserId));

        // Retrieve job and validate existence
        JobPosting job = jobRepository.findByJobId(jobId)
            .orElseThrow(() -> new IllegalArgumentException("No job exists for jobId: " + jobId));

        // Ensure job is open for application
        if (job.status() != JobStatus.OPEN) {
            throw new IllegalArgumentException("Only OPEN jobs can be applied to.");
        }

        // Check for duplicate applications (excluding withdrawn ones)
        boolean duplicate = applicationRepository.findByApplicantUserId(applicantUserId).stream()
            .anyMatch(application ->
                application.jobId().equals(jobId)
                    && application.status() != ApplicationStatus.WITHDRAWN
            );
        if (duplicate) {
            throw new IllegalArgumentException("Duplicate application is not allowed for this job.");
        }

        // Retrieve CV and validate ownership
        ApplicantCv cv = cvRepository.findByCvId(cvId)
            .orElseThrow(() -> new IllegalArgumentException("No CV exists for cvId: " + cvId));
        if (!cv.ownerUserId().equals(applicantUserId)) {
            throw new IllegalArgumentException("The selected CV does not belong to applicantUserId: " + applicantUserId);
        }

        // Ensure the job fits within the applicant's declared availability
        applicantAvailabilityService.requireApplicantAvailability(profile, job);

        // Check for schedule conflicts with accepted jobs
        scheduleConflictGuard.requireNoConflictWithAcceptedJobs(applicantUserId, jobId);

        // Create new application
        JobApplication application = new JobApplication(
            applicationIdGenerator.nextApplicationId(),
            jobId,
            applicantUserId,
            cvId,
            ApplicationStatus.SUBMITTED,
            LocalDateTime.now(),
            ""
        );

        // Save application
        applicationRepository.save(application);

        return application;
    }

    /**
     * Withdraw an existing application.
     *
     * Steps:
     * 1. Validate inputs
     * 2. Check user role
     * 3. Retrieve application
     * 4. Ensure ownership
     * 5. Prevent repeated withdrawal
     * 6. Update status to WITHDRAWN
     */
    public JobApplication withdrawApplication(String applicantUserId, String applicationId) {

        // Validate input parameters
        requireNonBlank(applicantUserId, "applicantUserId");
        requireNonBlank(applicationId, "applicationId");

        // Ensure the user is an active applicant
        userAccessPolicy.requireActiveUserWithRole(applicantUserId, UserRole.APPLICANT);

        // Retrieve application
        JobApplication application = applicationRepository.findByApplicationId(applicationId)
            .orElseThrow(() -> new IllegalArgumentException("Application not found."));

        // Ensure user owns the application
        if (!application.applicantUserId().equals(applicantUserId)) {
            throw new IllegalArgumentException("You can only withdraw your own application.");
        }

        // Prevent duplicate withdrawal
        if (application.status() == ApplicationStatus.WITHDRAWN) {
            throw new IllegalArgumentException("This application has already been withdrawn.");
        }

        // Create updated application with WITHDRAWN status
        JobApplication updated = new JobApplication(
            application.applicationId(),
            application.jobId(),
            application.applicantUserId(),
            application.cvId(),
            ApplicationStatus.WITHDRAWN,
            application.submittedAt(),
            application.reviewerNote()
        );

        // Save updated application
        applicationRepository.save(updated);

        return updated;
    }

    /**
     * Utility method to ensure a string is not null or blank
     */
    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }
}

