package com.bupt.tarecruitment.mo;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Audit-style record for MO actions inside application review and candidate tools.
 */
public record MoDecisionLogEntry(
    LocalDateTime timestamp,
    String organiserId,
    String jobId,
    String applicationId,
    String applicantUserId,
    String action,
    String detail
) {
    public MoDecisionLogEntry {
        Objects.requireNonNull(timestamp);
        Objects.requireNonNull(organiserId);
        Objects.requireNonNull(jobId);
        Objects.requireNonNull(applicationId);
        Objects.requireNonNull(applicantUserId);
        Objects.requireNonNull(action);
        Objects.requireNonNull(detail);
        detail = detail.trim();
    }
}
