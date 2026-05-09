package com.bupt.tarecruitment.mo;

import java.util.List;
import java.util.Objects;

/**
 * MO-owned job analytics summary.
 */
public record MoJobAnalyticsSummary(
    String organiserId,
    long totalJobs,
    long openJobs,
    long closedJobs,
    long totalApplications,
    long submittedApplications,
    long shortlistedApplications,
    long acceptedApplications,
    long rejectedApplications,
    long withdrawnApplications,
    double averageApplicationsPerJob,
    List<MoJobAnalyticsRow> jobRows
) {
    public MoJobAnalyticsSummary {
        Objects.requireNonNull(organiserId);
        Objects.requireNonNull(jobRows);
        jobRows = List.copyOf(jobRows);
    }
}
