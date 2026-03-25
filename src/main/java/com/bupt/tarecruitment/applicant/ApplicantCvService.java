package com.bupt.tarecruitment.applicant;

import com.bupt.tarecruitment.application.ApplicationRepository;
import com.bupt.tarecruitment.application.JobApplication;

import java.util.Objects;
import java.util.Optional;

public final class ApplicantCvService {
    private final ApplicationRepository applicationRepository;
    private final ApplicantProfileRepository profileRepository;
    private final CvTextStorage cvStorage;

    public ApplicantCvService(
        ApplicationRepository applicationRepository,
        ApplicantProfileRepository profileRepository,
        CvTextStorage cvStorage
    ) {
        this.applicationRepository = Objects.requireNonNull(applicationRepository);
        this.profileRepository = Objects.requireNonNull(profileRepository);
        this.cvStorage = Objects.requireNonNull(cvStorage);
    }

    public JobApplication submitCv(String applicationId, String cvContent) {
        requireNonBlank(applicationId, "applicationId");
        requireNonBlank(cvContent, "cvContent");

        JobApplication existingApplication = requireExistingApplication(applicationId);
        requireExistingProfile(existingApplication.applicantUserId());

        String cvReference = cvStorage.saveApplicationCv(
            existingApplication.applicantUserId(),
            existingApplication.applicationId(),
            cvContent
        );
        JobApplication updatedApplication = new JobApplication(
            existingApplication.applicationId(),
            existingApplication.jobId(),
            existingApplication.applicantUserId(),
            cvReference,
            existingApplication.status(),
            existingApplication.submittedAt(),
            existingApplication.reviewerNote()
        );

        applicationRepository.save(updatedApplication);
        return updatedApplication;
    }

    public Optional<String> getCvReferenceByApplicationId(String applicationId) {
        requireNonBlank(applicationId, "applicationId");

        return applicationRepository.findByApplicationId(applicationId)
            .map(JobApplication::cvFileName)
            .filter(reference -> !reference.isBlank());
    }

    public String loadCvContentByApplicationId(String applicationId) {
        requireNonBlank(applicationId, "applicationId");

        JobApplication application = requireExistingApplication(applicationId);
        if (application.cvFileName().isBlank()) {
            throw new IllegalArgumentException("No CV has been submitted for applicationId: " + applicationId);
        }

        return cvStorage.loadCv(application.cvFileName());
    }

    private ApplicantProfile requireExistingProfile(String userId) {
        return profileRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("No applicant profile exists for userId: " + userId));
    }

    private JobApplication requireExistingApplication(String applicationId) {
        return applicationRepository.findByApplicationId(applicationId)
            .orElseThrow(() -> new IllegalArgumentException("No application exists for applicationId: " + applicationId));
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }
}
