package com.bupt.tarecruitment.applicant;

import java.time.LocalDateTime;
import java.util.Objects;

public record ApplicantCv(
    String cvId,
    String ownerUserId,
    String title,
    String fileName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public ApplicantCv {
        Objects.requireNonNull(cvId);
        Objects.requireNonNull(ownerUserId);
        Objects.requireNonNull(title);
        Objects.requireNonNull(fileName);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(updatedAt);
    }
}
