package com.bupt.tarecruitment.application;

/**
 * 定义岗位申请的业务状态。
 */
public enum ApplicationStatus {
    // 刚提交，MO 还没开始处理。
    SUBMITTED,
    // 已进入候选名单，但还不是最终录用。
    SHORTLISTED,
    // 已被正式录用。
    ACCEPTED,
    // 已被拒绝。
    REJECTED,
    // applicant 主动撤回申请。
    WITHDRAWN
}
