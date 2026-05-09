package com.bupt.tarecruitment.application;

import java.util.List;
import java.util.Objects;

/**
 * 表示申请人可用时间与岗位时间的覆盖检查结果。
 */
public record AvailabilityCheckResult(
    String jobId,
    List<String> coveredJobSlots,
    List<String> uncoveredJobSlots
) {
    public AvailabilityCheckResult {
        Objects.requireNonNull(jobId);
        Objects.requireNonNull(coveredJobSlots);
        Objects.requireNonNull(uncoveredJobSlots);

        coveredJobSlots = List.copyOf(coveredJobSlots);
        uncoveredJobSlots = List.copyOf(uncoveredJobSlots);
    }

    public boolean fitsAvailability() {
        return uncoveredJobSlots.isEmpty();
    }
}
