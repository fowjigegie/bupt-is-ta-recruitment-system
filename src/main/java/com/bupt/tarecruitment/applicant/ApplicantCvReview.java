package com.bupt.tarecruitment.applicant;

import java.util.Objects;

public record ApplicantCvReview(
    ApplicantProfile profile,
    String cvContent
) {
    public ApplicantCvReview {
        Objects.requireNonNull(profile);
        Objects.requireNonNull(cvContent);
    }
}
