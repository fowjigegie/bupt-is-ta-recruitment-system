package com.bupt.tarecruitment.job;

import java.util.List;
import java.util.Objects;

/**
 * 表示一条助教岗位发布信息。
 */
public record JobPosting(
    String jobId,
    String organiserId,
    String title,
    String moduleOrActivity,
    String description,
    List<String> requiredSkills,
    double weeklyHours,
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
