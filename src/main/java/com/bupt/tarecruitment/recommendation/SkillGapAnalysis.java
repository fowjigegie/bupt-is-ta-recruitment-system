package com.bupt.tarecruitment.recommendation;

import java.util.List;
import java.util.Objects;

/**
 * 表示 US10 详细技能差距分析结果，供弹窗式解释界面使用。
 */
public record SkillGapAnalysis(
    String jobId,
    String readinessHeadline,
    String summary,
    MissingSkillsFeedback feedback,
    List<WeakMatchExplanation> weakMatchExplanations,
    List<PrioritySkillSuggestion> prioritySkillSuggestions,
    List<ImprovementScenario> improvementScenarios
) {
    public SkillGapAnalysis {
        Objects.requireNonNull(jobId);
        Objects.requireNonNull(readinessHeadline);
        Objects.requireNonNull(summary);
        Objects.requireNonNull(feedback);
        Objects.requireNonNull(weakMatchExplanations);
        Objects.requireNonNull(prioritySkillSuggestions);
        Objects.requireNonNull(improvementScenarios);

        weakMatchExplanations = List.copyOf(weakMatchExplanations);
        prioritySkillSuggestions = List.copyOf(prioritySkillSuggestions);
        improvementScenarios = List.copyOf(improvementScenarios);
    }

    /**
     * 解释某个 weak match 为什么成立。
     */
    public record WeakMatchExplanation(
        String requiredSkill,
        String supportingSkill,
        String explanation
    ) {
        public WeakMatchExplanation {
            Objects.requireNonNull(requiredSkill);
            Objects.requireNonNull(supportingSkill);
            Objects.requireNonNull(explanation);
        }
    }

    /**
     * 给出下一步最值得优先补足的技能建议。
     */
    public record PrioritySkillSuggestion(
        String skill,
        String reason
    ) {
        public PrioritySkillSuggestion {
            Objects.requireNonNull(skill);
            Objects.requireNonNull(reason);
        }
    }

    /**
     * 模拟“如果补上这些技能，准备度会怎样变化”。
     */
    public record ImprovementScenario(
        String title,
        List<String> addedSkills,
        List<String> newlyMatchedSkills,
        int projectedCoveragePercent,
        String explanation
    ) {
        public ImprovementScenario {
            Objects.requireNonNull(title);
            Objects.requireNonNull(addedSkills);
            Objects.requireNonNull(newlyMatchedSkills);
            Objects.requireNonNull(explanation);

            addedSkills = List.copyOf(addedSkills);
            newlyMatchedSkills = List.copyOf(newlyMatchedSkills);
        }
    }
}
