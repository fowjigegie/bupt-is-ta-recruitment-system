package com.bupt.tarecruitment.applicant;

import java.util.Comparator;
import java.util.Objects;

public final class ApplicantCvIdGenerator {
    private final ApplicantCvRepository repository;

    public ApplicantCvIdGenerator(ApplicantCvRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public String nextCvId() {
        int nextSequence = repository.findAll().stream()
            .map(ApplicantCv::cvId)
            .filter(cvId -> cvId.startsWith("cv"))
            .map(cvId -> cvId.substring(2))
            .filter(numberPart -> !numberPart.isBlank())
            .mapToInt(Integer::parseInt)
            .max()
            .orElse(0) + 1;

        return "cv%03d".formatted(nextSequence);
    }
}
