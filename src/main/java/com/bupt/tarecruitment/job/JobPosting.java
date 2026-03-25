package com.bupt.tarecruitment.job;

import java.util.List;
import java.util.Objects;

public record JobPosting(
    String jobId,
    String organiserId,
    String title,
    String moduleOrActivity,
    String description,
    List<String> requiredSkills,
    int weeklyHours,
    List<String> scheduleSlots,
    JobStatus status
) {
    public JobPosting {
        Objects.requireNonNull(jobId);
        Objects.requireNonNull(organiserId);
        Objects.requireNonNull(title);
        Objects.requireNonNull(moduleOrActivity);
        Objects.requireNonNull(description);
        Objects.requireNonNull(requiredSkills);
        Objects.requireNonNull(scheduleSlots);
        Objects.requireNonNull(status);

        requiredSkills = List.copyOf(requiredSkills);
        scheduleSlots = List.copyOf(scheduleSlots);
    }
}
