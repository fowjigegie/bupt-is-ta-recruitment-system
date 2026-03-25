package com.bupt.tarecruitment.applicant;

import java.util.Objects;
import java.util.Optional;

public final class ApplicantProfileService {
    private final ApplicantProfileRepository repository;
    private final ApplicantProfileValidator validator;

    public ApplicantProfileService(ApplicantProfileRepository repository, ApplicantProfileValidator validator) {
        this.repository = Objects.requireNonNull(repository);
        this.validator = Objects.requireNonNull(validator);
    }

    public ApplicantProfile createProfile(ApplicantProfile profile) {
        validator.validate(profile);

        if (repository.findByUserId(profile.userId()).isPresent()) {
            throw new IllegalArgumentException("A profile already exists for userId: " + profile.userId());
        }

        ensureStudentIdIsUnique(profile);

        repository.save(profile);
        return profile;
    }

    public Optional<ApplicantProfile> getProfileByUserId(String userId) {
        return repository.findByUserId(userId);
    }

    public ApplicantProfile updateProfile(ApplicantProfile profile) {
        validator.validate(profile);
        ensureStudentIdIsUnique(profile);
        repository.save(profile);
        return profile;
    }

    private void ensureStudentIdIsUnique(ApplicantProfile profile) {
        Optional<ApplicantProfile> existingProfile = repository.findByStudentId(profile.studentId());
        if (existingProfile.isPresent() && !existingProfile.get().userId().equals(profile.userId())) {
            throw new IllegalArgumentException("studentId is already used by another applicant: " + profile.studentId());
        }
    }
}
