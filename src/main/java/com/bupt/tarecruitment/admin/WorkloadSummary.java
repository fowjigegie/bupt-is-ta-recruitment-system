package com.bupt.tarecruitment.admin;

import java.util.List;
import java.util.Objects;

public record WorkloadSummary(
    String applicantUserId,
    int totalWeeklyHours,
    List<String> acceptedJobIds,
    List<String> scheduleSlots,
    boolean overloaded,
    boolean hasConflict
) {
    public WorkloadSummary {
        Objects.requireNonNull(applicantUserId);
        Objects.requireNonNull(acceptedJobIds);
        Objects.requireNonNull(scheduleSlots);

        acceptedJobIds = List.copyOf(acceptedJobIds);
        scheduleSlots = List.copyOf(scheduleSlots);
    }
}
