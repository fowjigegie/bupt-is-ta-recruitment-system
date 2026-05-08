package com.bupt.tarecruitment.mo;

import com.bupt.tarecruitment.application.ApplicationStatus;

import java.util.List;
import java.util.Objects;

/**
 * MO applicant ranking result for one job application.
 */
public record RankedApplicantCandidate(
    String applicationId,
    String jobId,
    String applicantUserId,
    String applicantName,
    ApplicationStatus status,
    int rankScore,
    int skillMatchPercent,
    boolean availabilityFit,
    boolean hasCv,
    List<String> matchedSkills,
    List<String> missingSkills,
    List<String> reasons
) {
    public RankedApplicantCandidate {
        Objects.requireNonNull(applicationId);
        Objects.requireNonNull(jobId);
        Objects.requireNonNull(applicantUserId);
        Objects.requireNonNull(applicantName);
        Objects.requireNonNull(status);
        Objects.requireNonNull(matchedSkills);
        Objects.requireNonNull(missingSkills);
        Objects.requireNonNull(reasons);

        matchedSkills = List.copyOf(matchedSkills);
        missingSkills = List.copyOf(missingSkills);
        reasons = List.copyOf(reasons);
    }
}
