package com.bupt.tarecruitment.job;

import java.util.Objects;

public final class JobIdGenerator {
    private final JobRepository repository;

    public JobIdGenerator(JobRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public String nextJobId() {
        int nextSequence = repository.findAll().stream()
            .map(JobPosting::jobId)
            .filter(jobId -> jobId.startsWith("job"))
            .map(jobId -> jobId.substring("job".length()))
            .filter(numberPart -> !numberPart.isBlank())
            .mapToInt(Integer::parseInt)
            .max()
            .orElse(0) + 1;

        return "job%03d".formatted(nextSequence);
    }
}

