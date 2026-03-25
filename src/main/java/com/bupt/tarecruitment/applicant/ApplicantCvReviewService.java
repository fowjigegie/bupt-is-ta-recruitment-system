package com.bupt.tarecruitment.applicant;

import java.util.Objects;

public final class ApplicantCvReviewService {
    private final ApplicantProfileRepository profileRepository;
    private final CvTextStorage cvStorage;

    public ApplicantCvReviewService(ApplicantProfileRepository profileRepository, CvTextStorage cvStorage) {
        this.profileRepository = Objects.requireNonNull(profileRepository);
        this.cvStorage = Objects.requireNonNull(cvStorage);
    }

    public ApplicantCvReview loadReviewByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank.");
        }

        ApplicantProfile profile = profileRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("No applicant profile exists for userId: " + userId));

        if (profile.cvFileName().isBlank()) {
            throw new IllegalArgumentException("No CV has been submitted for userId: " + userId);
        }

        String cvContent = cvStorage.loadCv(profile.cvFileName());
        return new ApplicantCvReview(profile, cvContent);
    }
}
