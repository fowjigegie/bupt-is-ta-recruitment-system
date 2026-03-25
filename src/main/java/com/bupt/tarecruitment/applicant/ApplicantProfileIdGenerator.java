package com.bupt.tarecruitment.applicant;

import java.util.Objects;

public final class ApplicantProfileIdGenerator {
    private final ApplicantProfileRepository repository;

    public ApplicantProfileIdGenerator(ApplicantProfileRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public String nextProfileId() {
        int maxSuffix = repository.findAll().stream()
            .map(ApplicantProfile::profileId)
            .filter(profileId -> profileId.startsWith("profile"))
            .map(profileId -> profileId.substring("profile".length()))
            .filter(suffix -> !suffix.isBlank())
            .mapToInt(this::parseSuffix)
            .max()
            .orElse(0);

        return "profile" + String.format("%03d", maxSuffix + 1);
    }

    private int parseSuffix(String suffix) {
        try {
            return Integer.parseInt(suffix);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
