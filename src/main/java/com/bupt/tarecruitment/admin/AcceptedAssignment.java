package com.bupt.tarecruitment.admin;

import java.util.List;
import java.util.Objects;

// US14: 每条 ACCEPTED 申请会展开成一个可展示的 assignment（岗位信息 + 排期）
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
