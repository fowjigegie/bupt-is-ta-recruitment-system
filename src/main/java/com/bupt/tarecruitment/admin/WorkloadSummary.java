package com.bupt.tarecruitment.admin;

import java.util.List;
import java.util.Objects;

public record WorkloadSummary(
    String applicantUserId,
    String applicantDisplayName,
    int totalWeeklyHours,
    List<AcceptedAssignment> acceptedAssignments,
    List<WorkloadConflict> conflicts,
    boolean overloaded,
    boolean hasConflict
) {
    public WorkloadSummary {
        Objects.requireNonNull(applicantUserId);
        Objects.requireNonNull(applicantDisplayName);
        Objects.requireNonNull(acceptedAssignments);
        Objects.requireNonNull(conflicts);

        acceptedAssignments = List.copyOf(acceptedAssignments);
        conflicts = List.copyOf(conflicts);
    }
}
