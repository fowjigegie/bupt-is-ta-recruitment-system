package com.bupt.tarecruitment.recommendation;

import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.applicant.ApplicantProfileRepository;
import com.bupt.tarecruitment.common.skill.SkillCatalog;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;
import com.bupt.tarecruitment.job.JobStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 根据申请人画像生成岗位推荐列表。
 */
public final class RecommendationService {
    private final ApplicantProfileRepository profileRepository;
    private final JobRepository jobRepository;

    public RecommendationService(
        ApplicantProfileRepository profileRepository,
        JobRepository jobRepository
    ) {
        this.profileRepository = Objects.requireNonNull(profileRepository);
        this.jobRepository = Objects.requireNonNull(jobRepository);
    }

    public List<RecommendationResult> recommendJobsForApplicant(String applicantUserId, int limit) {
        requireNonBlank(applicantUserId, "applicantUserId");
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be greater than 0.");
        }

        Optional<ApplicantProfile> profile = profileRepository.findByUserId(applicantUserId.trim());
        if (profile.isEmpty()) {
            return List.of();
        }

        return jobRepository.findAll().stream()
            .filter(job -> job.status() == JobStatus.OPEN)
            .map(job -> scoreJob(profile.get(), job))
            .filter(scoredJob -> scoredJob.result().matchScore() > 0)
            .sorted(Comparator
                .comparingInt((ScoredJob scoredJob) -> scoredJob.result().matchScore())
                .reversed()
                .thenComparing(scoredJob -> scoredJob.job().jobId()))
            .limit(limit)
            .map(scoredJob -> scoredJob.result())
            .toList();
    }

    private ScoredJob scoreJob(ApplicantProfile profile, JobPosting job) {
        Set<String> applicantSkills = normalizeSet(profile.skills());
        Set<String> matchedSkills = new LinkedHashSet<>();
        Set<String> relatedSkills = new LinkedHashSet<>();
        List<String> reasons = new ArrayList<>();
        int score = 0;

        for (String requiredSkill : job.requiredSkills()) {
            String normalizedRequiredSkill = normalize(requiredSkill);
            if (applicantSkills.contains(normalizedRequiredSkill)) {
                matchedSkills.add(requiredSkill.trim());
            } else if (applicantSkills.stream().anyMatch(skill -> SkillCatalog.areRelatedSkills(normalizedRequiredSkill, skill))) {
                relatedSkills.add(requiredSkill.trim());
            }
        }

        if (!matchedSkills.isEmpty()) {
            score += matchedSkills.size() * 3;
            reasons.add("Matches skill: " + String.join(", ", matchedSkills));
        }

        if (!relatedSkills.isEmpty()) {
            score += relatedSkills.size();
            reasons.add("Related skill match: " + String.join(", ", relatedSkills));
        }

        String jobText = (job.title() + " " + job.moduleOrActivity() + " " + job.description()).toLowerCase(Locale.ROOT);
        List<String> alignedPreferences = profile.desiredPositions().stream()
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .filter(preference -> jobText.contains(preference.toLowerCase(Locale.ROOT)))
            .distinct()
            .toList();

        if (!alignedPreferences.isEmpty()) {
            score += alignedPreferences.size() * 2;
            reasons.add("Aligned with desired position: " + String.join(", ", alignedPreferences));
        }

        if (jobText.contains(profile.programme().toLowerCase(Locale.ROOT))) {
            score += 1;
            reasons.add("Related to your programme: " + profile.programme());
        }

        return new ScoredJob(
            job,
            new RecommendationResult(job.jobId(), score, reasons)
        );
    }

    private Set<String> normalizeSet(List<String> rawValues) {
        return rawValues.stream()
            .map(RecommendationService::normalize)
            .filter(value -> !value.isBlank())
            .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }

    private record ScoredJob(JobPosting job, RecommendationResult result) {
        private ScoredJob {
            Objects.requireNonNull(job);
            Objects.requireNonNull(result);
        }
    }
}
