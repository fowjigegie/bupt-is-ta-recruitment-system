package com.bupt.tarecruitment.applicant;

import java.util.Objects;

/**
 * 负责生成申请人画像编号。
 */
public final class ApplicantProfileIdGenerator {
    private final ApplicantProfileRepository repository;

    public ApplicantProfileIdGenerator(ApplicantProfileRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    // US01: 生成新的 profileId，规则是 profile001 / profile002 / ...
    // 它是全局递增，而不是"每个用户从 001 开始"。
    public String nextProfileId() {
        int maxSuffix = repository.findAll().stream()
            .map(ApplicantProfile::profileId)
            .filter(profileId -> profileId.startsWith("profile"))
            .map(profileId -> profileId.substring("profile".length()))
            .filter(suffix -> !suffix.isBlank())
            .mapToInt(this::parseSuffix)
            .max()
            .orElse(0);

        return "profile" + String.format("%03d", maxSuffix + 1);
    }

    private int parseSuffix(String suffix) {
        try {
            return Integer.parseInt(suffix);
        } catch (NumberFormatException exception) {
            // 如果旧数据里出现了格式异常的编号，直接按 0 处理，避免生成器整体报错。
            return 0;
        }
    }
}
