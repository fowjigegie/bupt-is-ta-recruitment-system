package com.bupt.tarecruitment.admin;

import java.util.List;
import java.util.Objects;

/**
 * 汇总单个 TA 的已录用岗位、总工时和风险状态。
 */
public record WorkloadSummary(
    String applicantUserId,
    String applicantDisplayName,
    double totalWeeklyHours,
    List<AcceptedAssignment> acceptedAssignments,
    List<WorkloadConflict> conflicts,
    List<String> invalidScheduleEntries,
    boolean overloaded,
    boolean hasConflict,
    boolean hasInvalidScheduleData
) {
    public WorkloadSummary {
        Objects.requireNonNull(applicantUserId);
        Objects.requireNonNull(applicantDisplayName);
        Objects.requireNonNull(acceptedAssignments);
        Objects.requireNonNull(conflicts);
        Objects.requireNonNull(invalidScheduleEntries);

        acceptedAssignments = List.copyOf(acceptedAssignments);
        conflicts = List.copyOf(conflicts);
        invalidScheduleEntries = List.copyOf(invalidScheduleEntries);
    }
}
