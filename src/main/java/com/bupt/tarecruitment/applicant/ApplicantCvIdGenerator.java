package com.bupt.tarecruitment.applicant;

import java.util.Comparator;
import java.util.Objects;

/**
 * 负责生成申请人简历编号。
 */
public final class ApplicantCvIdGenerator {
    private final ApplicantCvRepository repository;

    public ApplicantCvIdGenerator(ApplicantCvRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    // US02: CV 编号是全局递增的，而不是"每个 applicant 从 001 重新开始"。
    // 这样可以保证整个系统里的 cvId 都唯一，后续在申请表里引用时更简单。
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
