package com.bupt.tarecruitment.admin;

import java.util.List;
import java.util.Objects;

// US14: 管理员端展示用的汇总对象（每个 TA 一条）。
public record WorkloadSummary(
    String applicantUserId,
    String applicantDisplayName,
    int totalWeeklyHours,
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
