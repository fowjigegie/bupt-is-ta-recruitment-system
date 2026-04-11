package com.bupt.tarecruitment.application;

import java.time.LocalDateTime;
import java.util.Objects;

// 申请记录本身。
// US06 查看状态页面展示的，就是这些申请记录里保存的 status / submittedAt / reviewerNote 等信息。
public record JobApplication(
    String applicationId,
    String jobId,
    String applicantUserId,
    String cvId,
    ApplicationStatus status,
    LocalDateTime submittedAt,
    String reviewerNote
) {
    public JobApplication {
        Objects.requireNonNull(applicationId);
        Objects.requireNonNull(jobId);
        Objects.requireNonNull(applicantUserId);
        Objects.requireNonNull(cvId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(submittedAt);
        Objects.requireNonNull(reviewerNote);
    }
}
