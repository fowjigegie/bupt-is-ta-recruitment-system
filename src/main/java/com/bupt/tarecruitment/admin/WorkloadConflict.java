package com.bupt.tarecruitment.admin;

import java.util.Objects;

/**
 * 表示两个已录用岗位之间的时间冲突。
 */
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
