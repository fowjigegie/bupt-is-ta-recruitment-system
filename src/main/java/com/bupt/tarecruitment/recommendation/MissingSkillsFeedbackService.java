package com.bupt.tarecruitment.recommendation;

import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.applicant.ApplicantProfileRepository;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
        List<String> missingSkills = new ArrayList<>();

        for (String requiredSkill : job.requiredSkills()) {
            String normalizedSkill = normalize(requiredSkill);
            if (normalizedSkill.isBlank()) {
                continue;
            }

            if (applicantSkills.contains(normalizedSkill)) {
                matchedSkills.add(requiredSkill.trim());
            } else {
                missingSkills.add(requiredSkill.trim());
            }
        }

        int totalRequiredSkills = matchedSkills.size() + missingSkills.size();
        int coveragePercent = totalRequiredSkills == 0
            ? 100
            : (int) Math.round((matchedSkills.size() * 100.0) / totalRequiredSkills);

        return new MissingSkillsFeedback(
            job.jobId(),
            matchedSkills,
            missingSkills,
            matchedSkills.size(),
            totalRequiredSkills,
            coveragePercent
        );
    }

    private Set<String> normalizeSet(List<String> rawValues) {
        return rawValues.stream()
            .map(MissingSkillsFeedbackService::normalize)
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
}
