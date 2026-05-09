package com.bupt.tarecruitment.applicant;

import java.util.List;
import java.util.Optional;

/**
 * 定义申请人画像的持久化接口。
 */
public interface ApplicantProfileRepository {
    // 按 userId 查 profile。
    // 当前设计里一个 applicant 只允许有一条 profile 记录。
    Optional<ApplicantProfile> findByUserId(String userId);

    // 按 studentId 查重。
    // studentId 在系统里要求全局唯一，所以 create/update 时会用它防止撞号。
    Optional<ApplicantProfile> findByStudentId(String studentId);

    // 读取全部 profile，主要给编号生成器、调试或测试使用。
    List<ApplicantProfile> findAll();

    // 保存或更新 profile。
    // "同 userId 覆盖还是追加"由具体实现决定。
    void save(ApplicantProfile profile);
}
