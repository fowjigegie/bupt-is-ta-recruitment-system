package com.bupt.tarecruitment.recommendation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.applicant.ApplicantProfileRepository;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;

/**
 * 根据画像和岗位要求生成技能差距反馈。
 */
public final class MissingSkillsFeedbackService {
    private final ApplicantProfileRepository profileRepository;
    private final JobRepository jobRepository;

    // US10 的 service 负责两件事：
    // 1) 根据 applicantUserId 和 jobId 把两边数据找出来
    // 2) 用规则型方式计算缺失技能反馈
    // 它不依赖外部 AI，所以结果稳定、可解释。
    public MissingSkillsFeedbackService(
        ApplicantProfileRepository profileRepository,
        JobRepository jobRepository
    ) {
        this.profileRepository = Objects.requireNonNull(profileRepository);
        this.jobRepository = Objects.requireNonNull(jobRepository);
    }

    // 这是 US10 对外的主入口。
    // 调用方只需要传 applicantUserId 和 jobId，
    // service 会自动完成"找 profile -> 找 job -> 计算技能差距"。
    public Optional<MissingSkillsFeedback> feedbackForApplicantAndJob(String applicantUserId, String jobId) {
        requireNonBlank(applicantUserId, "applicantUserId");
        requireNonBlank(jobId, "jobId");

        // 1) 先根据 userId 找到 applicant profile。
        // 如果 applicant 还没建 profile，就没有技能基础数据可供比对，因此直接返回空。
        Optional<ApplicantProfile> profile = profileRepository.findByUserId(applicantUserId.trim());
        if (profile.isEmpty()) {
            return Optional.empty();
        }

        // 2) 再根据 jobId 找到目标岗位。
        // 岗位不存在说明调用参数非法，因此这里直接抛错。
        JobPosting job = jobRepository.findByJobId(jobId.trim())
            .orElseThrow(() -> new IllegalArgumentException("No job exists for jobId: " + jobId));

        // 3) 进入规则匹配，生成页面最终要展示的技能反馈结果。
        return Optional.of(analyze(profile.get(), job));
    }

    // 真正的技能匹配逻辑在这里。
    // 做法很直接：先把 applicant skills 和 required skills 都规范化，
    // 再逐个检查 required skill 是否在 applicant 的技能集合里出现。
    MissingSkillsFeedback analyze(ApplicantProfile profile, JobPosting job) {
        Set<String> applicantSkills = normalizeSet(profile.skills());
        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();

        for (String requiredSkill : job.requiredSkills()) {
            String normalizedSkill = normalize(requiredSkill);
            if (normalizedSkill.isBlank()) {
                // 空白技能不参与计算，避免脏数据把覆盖率拉低。
                continue;
            }

            // required skill 在 applicant skills 里出现 => 匹配；
            // 没出现 => 缺失。
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

        // coveragePercent 是 UI 展示"匹配度"的核心数字。
        return new MissingSkillsFeedback(
            job.jobId(),
            matchedSkills,
            missingSkills,
            matchedSkills.size(),
            totalRequiredSkills,
            coveragePercent
        );
    }

    // 统一做 trim + lowercase，
    // 减少"Java"和" java "、"COMMUNICATION"和"communication"这种本质一样却匹配失败的情况。
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
