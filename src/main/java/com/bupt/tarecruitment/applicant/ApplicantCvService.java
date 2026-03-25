package com.bupt.tarecruitment.applicant;

import java.util.Objects;
import java.util.Optional;

public final class ApplicantCvService {
    private final ApplicantProfileRepository profileRepository;
    private final ApplicantProfileService profileService;
    private final CvTextStorage cvStorage;

    public ApplicantCvService(
        ApplicantProfileRepository profileRepository,
        ApplicantProfileService profileService,
        CvTextStorage cvStorage
    ) {
        this.profileRepository = Objects.requireNonNull(profileRepository);
        this.profileService = Objects.requireNonNull(profileService);
        this.cvStorage = Objects.requireNonNull(cvStorage);
    }

    public ApplicantProfile submitCv(String userId, String cvContent) {
        requireNonBlank(userId, "userId");
        requireNonBlank(cvContent, "cvContent");

        ApplicantProfile existingProfile = requireExistingProfile(userId);
        String cvReference = cvStorage.saveCv(userId, cvContent);
        ApplicantProfile updatedProfile = new ApplicantProfile(
            existingProfile.profileId(),
            existingProfile.userId(),
            existingProfile.studentId(),
            existingProfile.fullName(),
            existingProfile.programme(),
            existingProfile.yearOfStudy(),
            existingProfile.educationLevel(),
            existingProfile.skills(),
            existingProfile.availabilitySlots(),
            existingProfile.desiredPositions(),
            cvReference
        );

        return profileService.updateProfile(updatedProfile);
    }

    public Optional<String> getCvReferenceByUserId(String userId) {
        requireNonBlank(userId, "userId");

        return profileRepository.findByUserId(userId)
            .map(ApplicantProfile::cvFileName)
            .filter(reference -> !reference.isBlank());
    }

    public String loadCvContentByUserId(String userId) {
        requireNonBlank(userId, "userId");

        ApplicantProfile profile = requireExistingProfile(userId);
        if (profile.cvFileName().isBlank()) {
            throw new IllegalArgumentException("No CV has been submitted for userId: " + userId);
        }

        return cvStorage.loadCv(profile.cvFileName());
    }

    private ApplicantProfile requireExistingProfile(String userId) {
        return profileRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("No applicant profile exists for userId: " + userId));
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }
}
