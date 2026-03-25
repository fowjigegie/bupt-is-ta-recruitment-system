package com.bupt.tarecruitment.applicant;

import com.bupt.tarecruitment.application.ApplicationRepository;
import com.bupt.tarecruitment.application.JobApplication;

import java.util.Objects;

public final class ApplicantCvReviewService {
    private final ApplicationRepository applicationRepository;
    private final ApplicantProfileRepository profileRepository;
    private final CvTextStorage cvStorage;

    public ApplicantCvReviewService(
        ApplicationRepository applicationRepository,
        ApplicantProfileRepository profileRepository,
        CvTextStorage cvStorage
    ) {
        this.applicationRepository = Objects.requireNonNull(applicationRepository);
        this.profileRepository = Objects.requireNonNull(profileRepository);
        this.cvStorage = Objects.requireNonNull(cvStorage);
    }

    public ApplicantCvReview loadReviewByApplicationId(String applicationId) {
        if (applicationId == null || applicationId.isBlank()) {
            throw new IllegalArgumentException("applicationId must not be blank.");
        }

        JobApplication application = applicationRepository.findByApplicationId(applicationId)
            .orElseThrow(() -> new IllegalArgumentException("No application exists for applicationId: " + applicationId));

        ApplicantProfile profile = profileRepository.findByUserId(application.applicantUserId())
            .orElseThrow(() -> new IllegalArgumentException("No applicant profile exists for userId: " + application.applicantUserId()));

        if (application.cvFileName().isBlank()) {
            throw new IllegalArgumentException("No CV has been submitted for applicationId: " + applicationId);
        }

        String cvContent = cvStorage.loadCv(application.cvFileName());
        return new ApplicantCvReview(application, profile, cvContent);
    }
}
