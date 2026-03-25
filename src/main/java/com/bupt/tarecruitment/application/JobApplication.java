package com.bupt.tarecruitment.application;

import java.time.LocalDateTime;
import java.util.Objects;

public record JobApplication(
    String applicationId,
    String jobId,
    String applicantUserId,
    String cvFileName,
    ApplicationStatus status,
    LocalDateTime submittedAt,
    String reviewerNote
) {
    public JobApplication {
        Objects.requireNonNull(applicationId);
        Objects.requireNonNull(jobId);
        Objects.requireNonNull(applicantUserId);
        Objects.requireNonNull(cvFileName);
        Objects.requireNonNull(status);
        Objects.requireNonNull(submittedAt);
        Objects.requireNonNull(reviewerNote);
    }
}
