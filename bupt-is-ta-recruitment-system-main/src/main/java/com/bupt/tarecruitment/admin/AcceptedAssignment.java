package com.bupt.tarecruitment.admin;

import java.util.List;
import java.util.Objects;

/**
 * 表示管理员工作量视图中的已录用岗位摘要。
 */
public record AcceptedAssignment(
    String jobId,
    String title,
    String moduleOrActivity,
    double weeklyHours,
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
