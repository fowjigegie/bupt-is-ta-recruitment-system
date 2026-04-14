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
 * 根据画像和岗位要求生成技能差距反馈。
 */
public final class MissingSkillsFeedbackService {
    private final ApplicantProfileRepository profileRepository;
    private final JobRepository jobRepository;

    public MissingSkillsFeedbackService(
        ApplicantProfileRepository profileRepository,
        JobRepository jobRepository
    ) {
        this.profileRepository = Objects.requireNonNull(profileRepository);
        this.jobRepository = Objects.requireNonNull(jobRepository);
    }

    public Optional<MissingSkillsFeedback> feedbackForApplicantAndJob(String applicantUserId, String jobId) {
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

    MissingSkillsFeedback analyze(ApplicantProfile profile, JobPosting job) {
        Set<String> applicantSkills = normalizeSet(profile.skills());
        List<String> matchedSkills = new ArrayList<>();
        List<String> weaklyMatchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();

        for (String requiredSkill : job.requiredSkills()) {
            String normalizedRequiredSkill = normalize(requiredSkill);
            if (normalizedRequiredSkill.isBlank()) {
                continue;
            }

            if (applicantSkills.contains(normalizedRequiredSkill)) {
                matchedSkills.add(requiredSkill.trim());
            } else if (isWeaklyMatched(normalizedRequiredSkill, applicantSkills)) {
                weaklyMatchedSkills.add(requiredSkill.trim());
            } else {
                missingSkills.add(requiredSkill.trim());
            }
        }

        int totalRequiredSkills = matchedSkills.size() + weaklyMatchedSkills.size() + missingSkills.size();
        int weightedMatchScore = matchedSkills.size() * 100 + weaklyMatchedSkills.size() * 50;
        int coveragePercent = totalRequiredSkills == 0
            ? 100
            : (int) Math.round(weightedMatchScore / (double) totalRequiredSkills);

        return new MissingSkillsFeedback(
            job.jobId(),
            matchedSkills,
            weaklyMatchedSkills,
            missingSkills,
            matchedSkills.size(),
            weaklyMatchedSkills.size(),
            totalRequiredSkills,
            coveragePercent
        );
    }

    private boolean isWeaklyMatched(String normalizedRequiredSkill, Set<String> applicantSkills) {
        for (String applicantSkill : applicantSkills) {
            if (SkillCatalog.areRelatedSkills(normalizedRequiredSkill, applicantSkill)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> normalizeSet(List<String> rawValues) {
        return rawValues.stream()
            .map(MissingSkillsFeedbackService::normalize)
            .filter(value -> !value.isBlank())
            .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    private static String normalize(String value) {
        return SkillCatalog.normalize(value);
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }
}
