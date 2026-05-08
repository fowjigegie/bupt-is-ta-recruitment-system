package com.bupt.tarecruitment.mo;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One MO shortlist/candidate-pool label for an application.
 */
public record MoShortlistEntry(
    String organiserId,
    String jobId,
    String applicationId,
    String applicantUserId,
    MoShortlistStatus status,
    String note,
    LocalDateTime updatedAt
) {
    public MoShortlistEntry {
        Objects.requireNonNull(organiserId);
        Objects.requireNonNull(jobId);
        Objects.requireNonNull(applicationId);
        Objects.requireNonNull(applicantUserId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(note);
        Objects.requireNonNull(updatedAt);
        note = note.trim();
    }
}
