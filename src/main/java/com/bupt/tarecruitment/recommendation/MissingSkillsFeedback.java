package com.bupt.tarecruitment.recommendation;

import java.util.List;
import java.util.Objects;

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

        matchedSkills = List.copyOf(matchedSkills);
        missingSkills = List.copyOf(missingSkills);
    }

    public boolean fullyMatched() {
        return missingSkills.isEmpty();
    }
}
