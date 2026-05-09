package com.bupt.tarecruitment.mo;

import java.util.Objects;

/**
 * One quality issue found in a MO job posting.
 */
public record JobQualityIssue(
    String severity,
    String code,
    String message
) {
    public JobQualityIssue {
        Objects.requireNonNull(severity);
        Objects.requireNonNull(code);
        Objects.requireNonNull(message);
    }
}
