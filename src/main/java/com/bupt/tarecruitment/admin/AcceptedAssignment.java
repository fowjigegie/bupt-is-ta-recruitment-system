package com.bupt.tarecruitment.admin;

import java.util.List;
import java.util.Objects;

public record AcceptedAssignment(
    String jobId,
    String title,
    String moduleOrActivity,
    int weeklyHours,
    List<String> scheduleSlots
) {
    public AcceptedAssignment {
        Objects.requireNonNull(jobId);
        Objects.requireNonNull(title);
        Objects.requireNonNull(moduleOrActivity);
        Objects.requireNonNull(scheduleSlots);

        scheduleSlots = List.copyOf(scheduleSlots);
    }
}
