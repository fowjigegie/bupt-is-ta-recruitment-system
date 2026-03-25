package com.bupt.tarecruitment.applicant;

import com.bupt.tarecruitment.application.ApplicationRepository;
import com.bupt.tarecruitment.application.JobApplication;

import java.util.Objects;
import java.util.Optional;

public final class ApplicantCvService {
    private final ApplicationRepository applicationRepository;
    private final ApplicantCvRepository cvRepository;
    private final CvTextStorage cvStorage;

    public ApplicantCvService(
        ApplicationRepository applicationRepository,
        ApplicantCvRepository cvRepository,
        CvTextStorage cvStorage
    ) {
        this.applicationRepository = Objects.requireNonNull(applicationRepository);
        this.cvRepository = Objects.requireNonNull(cvRepository);
        this.cvStorage = Objects.requireNonNull(cvStorage);
    }

    public JobApplication attachCvToApplication(String applicationId, String cvId) {
        requireNonBlank(applicationId, "applicationId");
        requireNonBlank(cvId, "cvId");

        JobApplication existingApplication = requireExistingApplication(applicationId);
        ApplicantCv applicantCv = requireExistingCv(cvId);
        if (!applicantCv.ownerUserId().equals(existingApplication.applicantUserId())) {
            throw new IllegalArgumentException("The selected CV does not belong to applicantUserId: " + existingApplication.applicantUserId());
        }

        JobApplication updatedApplication = new JobApplication(
            existingApplication.applicationId(),
            existingApplication.jobId(),
            existingApplication.applicantUserId(),
            applicantCv.cvId(),
            existingApplication.status(),
            existingApplication.submittedAt(),
            existingApplication.reviewerNote()
        );

        applicationRepository.save(updatedApplication);
        return updatedApplication;
    }

    public Optional<String> getAssignedCvId(String applicationId) {
        requireNonBlank(applicationId, "applicationId");

        return applicationRepository.findByApplicationId(applicationId)
            .map(JobApplication::cvId)
            .filter(cvId -> !cvId.isBlank());
    }

    public ApplicantCv getAssignedCv(String applicationId) {
        requireNonBlank(applicationId, "applicationId");

        JobApplication application = requireExistingApplication(applicationId);
        if (application.cvId().isBlank()) {
            throw new IllegalArgumentException("No CV has been submitted for applicationId: " + applicationId);
        }

        return requireExistingCv(application.cvId());
    }

    public String loadCvContentByApplicationId(String applicationId) {
        ApplicantCv applicantCv = getAssignedCv(applicationId);
        return cvStorage.loadCv(applicantCv.fileName());
    }

    private JobApplication requireExistingApplication(String applicationId) {
        return applicationRepository.findByApplicationId(applicationId)
            .orElseThrow(() -> new IllegalArgumentException("No application exists for applicationId: " + applicationId));
    }

    private ApplicantCv requireExistingCv(String cvId) {
        return cvRepository.findByCvId(cvId)
            .orElseThrow(() -> new IllegalArgumentException("No CV exists for cvId: " + cvId));
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }
}
