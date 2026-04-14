package com.bupt.tarecruitment.ui;

import java.util.List;

/**
 * 保存 Resume 页面未提交的表单草稿，便于跨页面返回时恢复。
 */
public record ResumeDraftState(
    String selectedCvId,
    String fullName,
    String gradeLabel,
    String programme,
    String studentId,
    List<String> availabilitySlots,
    String cvTitle,
    String cvContent,
    List<String> skills,
    String desiredPositionsText
) {
    public ResumeDraftState {
        availabilitySlots = List.copyOf(availabilitySlots);
        skills = List.copyOf(skills);
    }

    public ResumeDraftState withSkills(List<String> nextSkills) {
        return new ResumeDraftState(
            selectedCvId,
            fullName,
            gradeLabel,
            programme,
            studentId,
            availabilitySlots,
            cvTitle,
            cvContent,
            nextSkills,
            desiredPositionsText
        );
    }
}
