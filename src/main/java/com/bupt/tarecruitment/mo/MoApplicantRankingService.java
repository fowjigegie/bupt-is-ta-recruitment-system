package com.bupt.tarecruitment.mo;

import com.bupt.tarecruitment.applicant.ApplicantCvRepository;
import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.applicant.ApplicantProfileRepository;
import com.bupt.tarecruitment.application.ApplicationRepository;
import com.bupt.tarecruitment.application.ApplicationStatus;
import com.bupt.tarecruitment.application.AvailabilityCheckResult;
import com.bupt.tarecruitment.application.ApplicantAvailabilityService;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.common.skill.SkillCatalog;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Ranks applicants for a MO job using skills, availability, CV presence, and review status.
 */
public final class MoApplicantRankingService {
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final ApplicantProfileRepository profileRepository;
    private final ApplicantCvRepository cvRepository;
    private final ApplicantAvailabilityService availabilityService;

    public MoApplicantRankingService(
        ApplicationRepository applicationRepository,
        JobRepository jobRepository,
        ApplicantProfileRepository profileRepository,
        ApplicantCvRepository cvRepository,
        ApplicantAvailabilityService availabilityService
    ) {
        this.applicationRepository = Objects.requireNonNull(applicationRepository);
        this.jobRepository = Objects.requireNonNull(jobRepository);
        this.profileRepository = Objects.requireNonNull(profileRepository);
        this.cvRepository = Objects.requireNonNull(cvRepository);
        this.availabilityService = Objects.requireNonNull(availabilityService);
    }

    public List<RankedApplicantCandidate> rankApplicantsForJob(String jobId) {
        requireNonBlank(jobId, "jobId");
        JobPosting job = jobRepository.findByJobId(jobId.trim())
            .orElseThrow(() -> new IllegalArgumentException("No job exists for jobId: " + jobId));

        return applicationRepository.findAll().stream()
            .filter(application -> application.jobId().equals(job.jobId()))
            .map(application -> rank(application, job))
            .sorted(Comparator
                .comparingInt(RankedApplicantCandidate::rankScore).reversed()
                .thenComparing(RankedApplicantCandidate::applicantName)
                .thenComparing(RankedApplicantCandidate::applicationId))
            .toList();
    }

    private RankedApplicantCandidate rank(JobApplication application, JobPosting job) {
        Optional<ApplicantProfile> profileOpt = profileRepository.findByUserId(application.applicantUserId());
        String applicantName = profileOpt.map(ApplicantProfile::fullName).orElse(application.applicantUserId());
        boolean hasCv = cvRepository.findByCvId(application.cvId()).isPresent();

        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();
        int skillMatchPercent = 0;

        if (profileOpt.isPresent()) {
            SkillMatch skillMatch = analyzeSkillMatch(profileOpt.get(), job);
            matchedSkills = skillMatch.matchedSkills();
            missingSkills = skillMatch.missingSkills();
            skillMatchPercent = skillMatch.matchPercent();
        }

        boolean availabilityFit = availabilityFits(application.applicantUserId(), job.jobId());

        List<String> reasons = new ArrayList<>();
        int score = 0;

        score += skillMatchPercent;
        if (skillMatchPercent >= 80) {
            reasons.add("Strong skill match");
        } else if (skillMatchPercent >= 50) {
            reasons.add("Partial skill match");
        } else {
            reasons.add("Low skill match");
        }

        if (availabilityFit) {
            score += 20;
            reasons.add("Availability covers job schedule");
        } else {
            score -= 15;
            reasons.add("Availability does not fully cover job schedule");
        }

        if (hasCv) {
            score += 10;
            reasons.add("CV attached");
        } else {
            score -= 20;
            reasons.add("CV record missing");
        }

        if (profileOpt.isPresent()) {
            score += 10;
            reasons.add("Profile found");
        } else {
            score -= 25;
            reasons.add("Profile missing");
        }

        if (application.status() == ApplicationStatus.SHORTLISTED) {
            score += 8;
            reasons.add("Already shortlisted");
        } else if (application.status() == ApplicationStatus.ACCEPTED) {
            score += 15;
            reasons.add("Already accepted");
        } else if (application.status() == ApplicationStatus.REJECTED || application.status() == ApplicationStatus.WITHDRAWN) {
            score -= 30;
            reasons.add("Not currently actionable");
        }

        score = Math.max(0, Math.min(100, score));

        return new RankedApplicantCandidate(
            application.applicationId(),
            application.jobId(),
            application.applicantUserId(),
            applicantName,
            application.status(),
            score,
            skillMatchPercent,
            availabilityFit,
            hasCv,
            matchedSkills,
            missingSkills,
            reasons
        );
    }

    private SkillMatch analyzeSkillMatch(ApplicantProfile profile, JobPosting job) {
        Set<String> applicantSkills = profile.skills().stream()
            .map(MoApplicantRankingService::normalize)
            .filter(value -> !value.isBlank())
            .collect(LinkedHashSet::new, Set::add, Set::addAll);

        List<String> matched = new ArrayList<>();
        List<String> weakMatched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String requiredSkill : job.requiredSkills()) {
            String normalizedRequiredSkill = normalize(requiredSkill);
            if (normalizedRequiredSkill.isBlank()) {
                continue;
            }

            if (applicantSkills.contains(normalizedRequiredSkill)) {
                matched.add(requiredSkill.trim());
            } else if (applicantSkills.stream().anyMatch(skill -> SkillCatalog.areRelatedSkills(normalizedRequiredSkill, skill))) {
                weakMatched.add(requiredSkill.trim());
            } else {
                missing.add(requiredSkill.trim());
            }
        }

        int total = matched.size() + weakMatched.size() + missing.size();
        int weighted = matched.size() * 100 + weakMatched.size() * 50;
        int percent = total == 0 ? 100 : (int) Math.round(weighted / (double) total);

        List<String> combinedMatched = new ArrayList<>(matched);
        combinedMatched.addAll(weakMatched);
        return new SkillMatch(combinedMatched, missing, percent);
    }

    private boolean availabilityFits(String applicantUserId, String jobId) {
        try {
            Optional<AvailabilityCheckResult> result = availabilityService.availabilityForApplicantAndJob(applicantUserId, jobId);
            return result.map(AvailabilityCheckResult::fitsAvailability).orElse(false);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }

    private record SkillMatch(List<String> matchedSkills, List<String> missingSkills, int matchPercent) {
        private SkillMatch {
            matchedSkills = List.copyOf(matchedSkills);
            missingSkills = List.copyOf(missingSkills);
        }
    }
}
