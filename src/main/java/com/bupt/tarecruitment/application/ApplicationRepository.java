package com.bupt.tarecruitment.application;

import java.util.List;
import java.util.Optional;

/**
 * 定义岗位申请数据的持久化接口。
 */
public interface ApplicationRepository {
    // 按 applicationId 取单条申请，适合详情、审核、更新状态等场景。
    Optional<JobApplication> findByApplicationId(String applicationId);

    // US06 主要就靠这个方法：查出当前 applicant 的全部申请，再在页面里按时间倒序展示。
    List<JobApplication> findByApplicantUserId(String applicantUserId);

    // 管理端、审核页、汇总页等场景会读取全部申请。
    List<JobApplication> findAll();

    // 统一的新增/覆盖保存入口。
    void save(JobApplication application);
}
