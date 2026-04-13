package com.bupt.tarecruitment.recommendation;

import java.util.List;
import java.util.Objects;

/**
 * 表示申请人与岗位要求之间的技能差距反馈。
 */
public record MissingSkillsFeedback(
    String jobId,
    List<String> matchedSkills,
    List<String> missingSkills,
    int matchedRequiredSkillCount,
    int totalRequiredSkillCount,
    int coveragePercent
) {
    public MissingSkillsFeedback {
        Objects.requireNonNull(jobId);
        Objects.requireNonNull(matchedSkills);
        Objects.requireNonNull(missingSkills);

        // 复制成不可变列表，避免外部继续改原始 List，导致结果对象内容被意外篡改。
        matchedSkills = List.copyOf(matchedSkills);
        missingSkills = List.copyOf(missingSkills);
    }

    // UI 常用的快捷判断：
    // 如果缺失技能列表为空，就说明这个 applicant 对当前岗位是 fully matched。
    public boolean fullyMatched() {
        return missingSkills.isEmpty();
    }
}
