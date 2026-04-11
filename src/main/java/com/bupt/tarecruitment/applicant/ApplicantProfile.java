package com.bupt.tarecruitment.applicant;

import java.util.List;
import java.util.Objects;

// US01/US05: Applicant 的基础资料模型。
// 创建 profile、编辑 profile，以及后续基于 skills 做推荐/技能反馈时，都会围绕这份对象展开。
public record ApplicantProfile(
    String profileId,
    String userId,
    String studentId,
    String fullName,
    String programme,
    int yearOfStudy,
    String educationLevel,
    List<String> skills,
    List<String> availabilitySlots,
    List<String> desiredPositions
) {
    public ApplicantProfile {
        // 基础非空保护，避免把半空对象直接传进业务层或存储层。
        Objects.requireNonNull(profileId);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(studentId);
        Objects.requireNonNull(fullName);
        Objects.requireNonNull(programme);
        Objects.requireNonNull(educationLevel);
        Objects.requireNonNull(skills);
        Objects.requireNonNull(availabilitySlots);
        Objects.requireNonNull(desiredPositions);

        // 这些列表在构造时复制一份，避免外部 later 修改原 List 影响 profile 内部状态。
        skills = List.copyOf(skills);
        availabilitySlots = List.copyOf(availabilitySlots);
        desiredPositions = List.copyOf(desiredPositions);
    }
}
