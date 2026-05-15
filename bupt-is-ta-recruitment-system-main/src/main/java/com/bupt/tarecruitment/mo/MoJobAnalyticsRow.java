package com.bupt.tarecruitment.mo;

import com.bupt.tarecruitment.job.JobStatus;

import java.util.Objects;

/**
 * Analytics row for one MO-owned job.
 */
public record MoJobAnalyticsRow(
    String jobId,
    String title,
    JobStatus status,
    long applicationCount,
    long acceptedCount,
    int acceptanceRatePercent,
    double weeklyHours,
    int qualityScore
) {
    public MoJobAnalyticsRow {
        Objects.requireNonNull(jobId);
        Objects.requireNonNull(title);
        Objects.requireNonNull(status);
    }
}
