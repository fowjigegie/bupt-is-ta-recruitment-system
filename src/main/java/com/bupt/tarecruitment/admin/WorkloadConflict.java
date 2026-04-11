package com.bupt.tarecruitment.admin;

import java.util.Objects;

// US14: 两个已录用岗位的排期冲突记录。
public record WorkloadConflict(
    String jobIdA,
    String jobTitleA,
    String jobIdB,
    String jobTitleB,
    String overlapSlot
) {
    public WorkloadConflict {
        Objects.requireNonNull(jobIdA);
        Objects.requireNonNull(jobTitleA);
        Objects.requireNonNull(jobIdB);
        Objects.requireNonNull(jobTitleB);
        Objects.requireNonNull(overlapSlot);
    }
}
