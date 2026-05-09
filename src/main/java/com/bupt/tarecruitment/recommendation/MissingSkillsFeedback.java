package com.bupt.tarecruitment.recommendation;

import java.util.List;
import java.util.Objects;

/**
 * 表示申请人与岗位要求之间的技能差距反馈。
 */
public record MissingSkillsFeedback(
    String jobId,
    List<String> matchedSkills,
    List<String> weaklyMatchedSkills,
    List<String> missingSkills,
    int matchedRequiredSkillCount,
    int weaklyMatchedRequiredSkillCount,
    int totalRequiredSkillCount,
    int coveragePercent
) {
    public MissingSkillsFeedback {
        Objects.requireNonNull(jobId);
        Objects.requireNonNull(matchedSkills);
        Objects.requireNonNull(weaklyMatchedSkills);
        Objects.requireNonNull(missingSkills);

        matchedSkills = List.copyOf(matchedSkills);
        weaklyMatchedSkills = List.copyOf(weaklyMatchedSkills);
        missingSkills = List.copyOf(missingSkills);
    }

    public boolean fullyMatched() {
        return weaklyMatchedSkills.isEmpty() && missingSkills.isEmpty();
    }
}
