package com.bupt.tarecruitment.application;

import com.bupt.tarecruitment.applicant.ApplicantCv;
import com.bupt.tarecruitment.applicant.ApplicantCvRepository;
import com.bupt.tarecruitment.applicant.ApplicantProfileRepository;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;
import com.bupt.tarecruitment.job.JobStatus;

import java.time.LocalDateTime;
import java.util.Objects;

public final class JobApplicationService {
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationIdGenerator applicationIdGenerator;
    private final ApplicantProfileRepository profileRepository;
    private final ApplicantCvRepository cvRepository;

    public JobApplicationService(
        JobRepository jobRepository,
        ApplicationRepository applicationRepository,
        ApplicationIdGenerator applicationIdGenerator,
        ApplicantProfileRepository profileRepository,
        ApplicantCvRepository cvRepository
    ) {
        this.jobRepository = Objects.requireNonNull(jobRepository);
        this.applicationRepository = Objects.requireNonNull(applicationRepository);
        this.applicationIdGenerator = Objects.requireNonNull(applicationIdGenerator);
        this.profileRepository = Objects.requireNonNull(profileRepository);
        this.cvRepository = Objects.requireNonNull(cvRepository);
    }

    public JobApplication applyToJobWithCv(String applicantUserId, String jobId, String cvId) {
        requireNonBlank(applicantUserId, "applicantUserId");
        requireNonBlank(jobId, "jobId");
        requireNonBlank(cvId, "cvId");

        profileRepository.findByUserId(applicantUserId)
            .orElseThrow(() -> new IllegalArgumentException("No applicant profile exists for userId: " + applicantUserId));

        JobPosting job = jobRepository.findByJobId(jobId)
            .orElseThrow(() -> new IllegalArgumentException("No job exists for jobId: " + jobId));

        if (job.status() != JobStatus.OPEN) {
            throw new IllegalArgumentException("Only OPEN jobs can be applied to.");
        }

        boolean duplicate = applicationRepository.findByApplicantUserId(applicantUserId).stream()
            .anyMatch(application -> application.jobId().equals(jobId));
        if (duplicate) {
            throw new IllegalArgumentException("Duplicate application is not allowed for this job.");
        }

        ApplicantCv cv = cvRepository.findByCvId(cvId)
            .orElseThrow(() -> new IllegalArgumentException("No CV exists for cvId: " + cvId));
        if (!cv.ownerUserId().equals(applicantUserId)) {
            throw new IllegalArgumentException("The selected CV does not belong to applicantUserId: " + applicantUserId);
        }

        JobApplication application = new JobApplication(
            applicationIdGenerator.nextApplicationId(),
            jobId,
            applicantUserId,
            cvId,
            ApplicationStatus.SUBMITTED,
            LocalDateTime.now(),
            ""
        );
        applicationRepository.save(application);
        return application;
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }
}

