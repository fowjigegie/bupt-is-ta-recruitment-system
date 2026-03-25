package com.bupt.tarecruitment.applicant;

import java.util.Objects;

public record ApplicantCvReview(
    com.bupt.tarecruitment.application.JobApplication application,
    ApplicantProfile profile,
    String cvContent
) {
    public ApplicantCvReview {
        Objects.requireNonNull(application);
        Objects.requireNonNull(profile);
        Objects.requireNonNull(cvContent);
    }
}
