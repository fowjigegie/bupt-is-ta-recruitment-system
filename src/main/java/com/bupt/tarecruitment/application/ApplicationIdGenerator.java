package com.bupt.tarecruitment.application;

import java.util.Objects;

public final class ApplicationIdGenerator {
    private final ApplicationRepository repository;

    public ApplicationIdGenerator(ApplicationRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public String nextApplicationId() {
        int nextSequence = repository.findAll().stream()
            .map(JobApplication::applicationId)
            .filter(applicationId -> applicationId.startsWith("application"))
            .map(applicationId -> applicationId.substring("application".length()))
            .filter(numberPart -> !numberPart.isBlank())
            .mapToInt(Integer::parseInt)
            .max()
            .orElse(0) + 1;

        return "application%03d".formatted(nextSequence);
    }
}

