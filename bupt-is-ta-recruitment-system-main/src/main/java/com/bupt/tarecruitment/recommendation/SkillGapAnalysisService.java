package com.bupt.tarecruitment.recommendation;

import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.applicant.ApplicantProfileRepository;
import com.bupt.tarecruitment.common.skill.SkillCatalog;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 在基础缺失技能结果之上，补充 weak match 解释、优先提升建议和 what-if 模拟。
 */
public final class SkillGapAnalysisService {
    private final ApplicantProfileRepository profileRepository;
    private final JobRepository jobRepository;
    private final MissingSkillsFeedbackService feedbackService;

    public SkillGapAnalysisService(
        ApplicantProfileRepository profileRepository,
        JobRepository jobRepository
    ) {
        this.profileRepository = Objects.requireNonNull(profileRepository);
        this.jobRepository = Objects.requireNonNull(jobRepository);
        this.feedbackService = new MissingSkillsFeedbackService(profileRepository, jobRepository);
    }

    public Optional<SkillGapAnalysis> analysisForApplicantAndJob(String applicantUserId, String jobId) {
        requireNonBlank(applicantUserId, "applicantUserId");
        requireNonBlank(jobId, "jobId");

        Optional<ApplicantProfile> profile = profileRepository.findByUserId(applicantUserId.trim());
        if (profile.isEmpty()) {
            return Optional.empty();
        }

        JobPosting job = jobRepository.findByJobId(jobId.trim())
            .orElseThrow(() -> new IllegalArgumentException("No job exists for jobId: " + jobId));

        return Optional.of(analyze(profile.get(), job));
    }

    SkillGapAnalysis analyze(ApplicantProfile profile, JobPosting job) {
        MissingSkillsFeedback feedback = feedbackService.analyze(profile, job);
        List<SkillGapAnalysis.WeakMatchExplanation> weakMatchExplanations = buildWeakMatchExplanations(profile, feedback);
        List<SkillGapAnalysis.PrioritySkillSuggestion> prioritySuggestions = buildPrioritySuggestions(feedback);
        List<SkillGapAnalysis.ImprovementScenario> improvementScenarios = buildImprovementScenarios(
            profile,
            job,
            feedback,
            prioritySuggestions
        );

        return new SkillGapAnalysis(
            job.jobId(),
            buildReadinessHeadline(feedback),
            buildSummary(feedback),
            feedback,
            weakMatchExplanations,
            prioritySuggestions,
            improvementScenarios
        );
    }

    private List<SkillGapAnalysis.WeakMatchExplanation> buildWeakMatchExplanations(
        ApplicantProfile profile,
        MissingSkillsFeedback feedback
    ) {
        List<SkillGapAnalysis.WeakMatchExplanation> explanations = new ArrayList<>();

        for (String weakSkill : feedback.weaklyMatchedSkills()) {
            String supportingSkill = profile.skills().stream()
                .map(String::trim)
                .filter(skill -> !skill.isBlank())
                .filter(skill -> SkillCatalog.areRelatedSkills(weakSkill, skill))
                .findFirst()
                .orElse("related experience");

            explanations.add(new SkillGapAnalysis.WeakMatchExplanation(
                weakSkill,
                supportingSkill,
                supportingSkill
                    + " gives you related background for "
                    + weakSkill
                    + ", so the system treats it as transferable evidence rather than a direct exact match."
            ));
        }

        return explanations;
    }

    private List<SkillGapAnalysis.PrioritySkillSuggestion> buildPrioritySuggestions(MissingSkillsFeedback feedback) {
        List<SkillGapAnalysis.PrioritySkillSuggestion> suggestions = new ArrayList<>();
        LinkedHashSet<String> orderedTargets = new LinkedHashSet<>();
        orderedTargets.addAll(feedback.missingSkills());
        orderedTargets.addAll(feedback.weaklyMatchedSkills());

        for (String skill : orderedTargets) {
            if (suggestions.size() >= 3) {
                break;
            }

            boolean directGap = feedback.missingSkills().contains(skill);
            suggestions.add(new SkillGapAnalysis.PrioritySkillSuggestion(
                skill,
                directGap
                    ? skill + " is a direct listed requirement for this job and would raise your readiness immediately."
                    : "Adding a direct " + skill + " label would strengthen a current weak match into a clearer fit."
            ));
        }

        return suggestions;
    }

    private List<SkillGapAnalysis.ImprovementScenario> buildImprovementScenarios(
        ApplicantProfile profile,
        JobPosting job,
        MissingSkillsFeedback currentFeedback,
        List<SkillGapAnalysis.PrioritySkillSuggestion> prioritySuggestions
    ) {
        if (prioritySuggestions.isEmpty()) {
            return List.of();
        }

        List<SkillGapAnalysis.ImprovementScenario> scenarios = new ArrayList<>();
        List<String> firstSkill = List.of(prioritySuggestions.get(0).skill());
        scenarios.add(simulateScenario(profile, job, currentFeedback, firstSkill));

        if (prioritySuggestions.size() >= 2) {
            List<String> topTwoSkills = List.of(
                prioritySuggestions.get(0).skill(),
                prioritySuggestions.get(1).skill()
            );
            scenarios.add(simulateScenario(profile, job, currentFeedback, topTwoSkills));
        }

        return scenarios;
    }

    private SkillGapAnalysis.ImprovementScenario simulateScenario(
        ApplicantProfile profile,
        JobPosting job,
        MissingSkillsFeedback currentFeedback,
        List<String> addedSkills
    ) {
        ApplicantProfile simulatedProfile = withAddedSkills(profile, addedSkills);
        MissingSkillsFeedback projectedFeedback = feedbackService.analyze(simulatedProfile, job);
        Set<String> currentMatched = normalizeSet(currentFeedback.matchedSkills());
        List<String> newlyMatchedSkills = projectedFeedback.matchedSkills().stream()
            .filter(skill -> !currentMatched.contains(SkillCatalog.normalize(skill)))
            .toList();

        String title = addedSkills.size() == 1
            ? "If you add " + addedSkills.get(0)
            : "If you add " + String.join(" + ", addedSkills);

        String explanation = "Readiness could rise from "
            + currentFeedback.coveragePercent()
            + "% to "
            + projectedFeedback.coveragePercent()
            + "%"
            + (newlyMatchedSkills.isEmpty()
                ? "."
                : ", with direct improvement in " + String.join(", ", newlyMatchedSkills) + ".");

        return new SkillGapAnalysis.ImprovementScenario(
            title,
            addedSkills,
            newlyMatchedSkills,
            projectedFeedback.coveragePercent(),
            explanation
        );
    }

    private ApplicantProfile withAddedSkills(ApplicantProfile profile, List<String> addedSkills) {
        LinkedHashSet<String> mergedSkills = new LinkedHashSet<>();
        for (String existingSkill : profile.skills()) {
            if (existingSkill != null && !existingSkill.isBlank()) {
                mergedSkills.add(existingSkill.trim());
            }
        }
        for (String addedSkill : addedSkills) {
            if (addedSkill != null && !addedSkill.isBlank()) {
                mergedSkills.add(addedSkill.trim());
            }
        }

        return new ApplicantProfile(
            profile.profileId(),
            profile.userId(),
            profile.studentId(),
            profile.fullName(),
            profile.programme(),
            profile.yearOfStudy(),
            profile.educationLevel(),
            new ArrayList<>(mergedSkills),
            profile.availabilitySlots(),
            profile.desiredPositions(),
            profile.avatarPath()
        );
    }

    private String buildReadinessHeadline(MissingSkillsFeedback feedback) {
        if (feedback.fullyMatched()) {
            return "Strong fit for this role";
        }
        if (feedback.coveragePercent() >= 75) {
            return "Close to ready for this role";
        }
        if (feedback.coveragePercent() >= 40) {
            return "Developing fit with clear next steps";
        }
        return "Early fit with several skills to build";
    }

    private String buildSummary(MissingSkillsFeedback feedback) {
        if (feedback.totalRequiredSkillCount() == 0) {
            return "This job does not list required skills, so there is no skill gap to analyze.";
        }

        return "You directly match "
            + feedback.matchedRequiredSkillCount()
            + " listed skill(s), show related evidence for "
            + feedback.weaklyMatchedRequiredSkillCount()
            + ", and still need "
            + feedback.missingSkills().size()
            + " more to close the gap.";
    }

    private Set<String> normalizeSet(List<String> values) {
        return values.stream()
            .map(SkillCatalog::normalize)
            .filter(value -> !value.isBlank())
            .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }
}
