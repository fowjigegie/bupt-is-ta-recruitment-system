package com.bupt.tarecruitment.applicant;

import java.util.List;
import java.util.Objects;

/**
 * 表示申请人的个人画像、技能和求职偏好。
 */
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
    List<String> desiredPositions,
    String avatarPath
) {
    public ApplicantProfile(
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
        this(
            profileId,
            userId,
            studentId,
            fullName,
            programme,
            yearOfStudy,
            educationLevel,
            skills,
            availabilitySlots,
            desiredPositions,
            ""
        );
    }

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
        Objects.requireNonNull(avatarPath);

        // 这些列表在构造时复制一份，避免外部 later 修改原 List 影响 profile 内部状态。
        skills = List.copyOf(skills);
        availabilitySlots = List.copyOf(availabilitySlots);
        desiredPositions = List.copyOf(desiredPositions);
        avatarPath = avatarPath.trim();
    }
}
