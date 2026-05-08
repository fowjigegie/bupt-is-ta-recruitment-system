package com.bupt.tarecruitment.mo;

import java.util.List;
import java.util.Objects;

/**
 * Quality report for one job posting.
 */
public record JobQualityReport(
    String jobId,
    String title,
    int qualityScore,
    boolean readyForPublishing,
    List<JobQualityIssue> issues
) {
    public JobQualityReport {
        Objects.requireNonNull(jobId);
        Objects.requireNonNull(title);
        Objects.requireNonNull(issues);
        issues = List.copyOf(issues);
    }
}
