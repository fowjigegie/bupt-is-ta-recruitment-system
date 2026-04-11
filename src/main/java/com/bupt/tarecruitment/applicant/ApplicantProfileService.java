package com.bupt.tarecruitment.applicant;

import com.bupt.tarecruitment.auth.UserAccessPolicy;
import com.bupt.tarecruitment.auth.UserRepository;
import com.bupt.tarecruitment.auth.UserRole;

import java.util.Objects;
import java.util.Optional;

public final class ApplicantProfileService {
    private final ApplicantProfileRepository repository;
    private final ApplicantProfileValidator validator;
    private final UserAccessPolicy userAccessPolicy;

    public ApplicantProfileService(ApplicantProfileRepository repository, ApplicantProfileValidator validator) {
        this(repository, validator, UserAccessPolicy.noOp());
    }

    public ApplicantProfileService(
        ApplicantProfileRepository repository,
        ApplicantProfileValidator validator,
        UserRepository userRepository
    ) {
        this(repository, validator, new UserAccessPolicy(userRepository));
    }

    private ApplicantProfileService(
        ApplicantProfileRepository repository,
        ApplicantProfileValidator validator,
        UserAccessPolicy userAccessPolicy
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.validator = Objects.requireNonNull(validator);
        this.userAccessPolicy = Objects.requireNonNull(userAccessPolicy);
    }

    // US01/US05 的核心服务：
    // 负责创建 profile、读取 profile、更新 profile，并组合“字段校验 + 用户权限 + 学号唯一性”这几类规则。

    // US01: 首次创建 applicant profile。
    // 这里既要校验字段，也要确认用户是有效 applicant，
    // 并且这个 user 之前还没有创建过 profile。
    public ApplicantProfile createProfile(ApplicantProfile profile) {
        // 1) 字段合法性校验（格式/必填）
        validator.validate(profile);
        // 2) 确保当前 user 存在且角色是 APPLICANT
        userAccessPolicy.requireActiveUserWithRole(profile.userId(), UserRole.APPLICANT);

        if (repository.findByUserId(profile.userId()).isPresent()) {
            throw new IllegalArgumentException("A profile already exists for userId: " + profile.userId());
        }

        // 3) 学号在系统里必须唯一（不同 applicant 不能冲突）
        ensureStudentIdIsUnique(profile);

        // 4) 落库保存
        repository.save(profile);
        return profile;
    }

    public Optional<ApplicantProfile> getProfileByUserId(String userId) {
        // UI、控制台 demo、测试代码想读取当前 applicant 的资料时，统一从这里进入。
        return repository.findByUserId(userId);
    }

    // US05: 编辑已有 profile。
    // 更新时必须先找到旧记录，并要求 profileId 和旧记录一致，
    // 避免把“编辑”误做成“新建另一条资料”。
    public ApplicantProfile updateProfile(ApplicantProfile profile) {
        // 1) 先做字段合法性校验
        validator.validate(profile);
        // 2) 只有有效 applicant 才能编辑
        userAccessPolicy.requireActiveUserWithRole(profile.userId(), UserRole.APPLICANT);

        ApplicantProfile existingProfile = repository.findByUserId(profile.userId())
            .orElseThrow(() -> new IllegalArgumentException("No profile exists for userId: " + profile.userId()));

        // 3) 必须保证更新的是“同一条 profile 记录”
        if (!existingProfile.profileId().equals(profile.profileId())) {
            throw new IllegalArgumentException(
                "profileId does not match the existing profile for userId: " + profile.userId()
            );
        }

        // 4) 学号仍然需要保持全局唯一
        ensureStudentIdIsUnique(profile);
        // 5) 落库保存
        repository.save(profile);
        return profile;
    }

    // studentId 在系统里必须全局唯一。
    // 但同一个 applicant 编辑自己资料时，允许继续保留原 studentId。
    private void ensureStudentIdIsUnique(ApplicantProfile profile) {
        Optional<ApplicantProfile> existingProfile = repository.findByStudentId(profile.studentId());
        if (existingProfile.isPresent() && !existingProfile.get().userId().equals(profile.userId())) {
            throw new IllegalArgumentException("studentId is already used by another applicant: " + profile.studentId());
        }
    }
}
