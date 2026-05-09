package com.bupt.tarecruitment.applicant;

import java.util.Objects;

/**
 * 封装申请审核时需要展示的简历与画像快照。
 */
public record ApplicantCvReview(
    com.bupt.tarecruitment.application.JobApplication application,
    ApplicantCv cv,
    ApplicantProfile profile,
    String cvContent
) {
    public ApplicantCvReview {
        Objects.requireNonNull(application);
        Objects.requireNonNull(cv);
        Objects.requireNonNull(profile);
        Objects.requireNonNull(cvContent);
    }
}
