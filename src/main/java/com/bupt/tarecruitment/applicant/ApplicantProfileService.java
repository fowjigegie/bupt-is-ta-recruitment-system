package com.bupt.tarecruitment.applicant;

import com.bupt.tarecruitment.auth.UserAccessPolicy;
import com.bupt.tarecruitment.auth.UserRepository;
import com.bupt.tarecruitment.auth.UserRole;

import java.util.Objects;
import java.util.Optional;

public final class ApplicantProfileService {
    private final ApplicantProfileRepository repository;
    private final ApplicantProfileValidator validator;
    private final UserAccessPolicy userAccessPolicy;

    public ApplicantProfileService(ApplicantProfileRepository repository, ApplicantProfileValidator validator) {
        this(repository, validator, UserAccessPolicy.noOp());
    }

    public ApplicantProfileService(
        ApplicantProfileRepository repository,
        ApplicantProfileValidator validator,
        UserRepository userRepository
    ) {
        this(repository, validator, new UserAccessPolicy(userRepository));
    }

    private ApplicantProfileService(
        ApplicantProfileRepository repository,
        ApplicantProfileValidator validator,
        UserAccessPolicy userAccessPolicy
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.validator = Objects.requireNonNull(validator);
        this.userAccessPolicy = Objects.requireNonNull(userAccessPolicy);
    }

    public ApplicantProfile createProfile(ApplicantProfile profile) {
        validator.validate(profile);
        userAccessPolicy.requireActiveUserWithRole(profile.userId(), UserRole.APPLICANT);

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
        userAccessPolicy.requireActiveUserWithRole(profile.userId(), UserRole.APPLICANT);
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
