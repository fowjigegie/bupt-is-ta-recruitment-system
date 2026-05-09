package com.bupt.tarecruitment.mo;

import com.bupt.tarecruitment.application.ApplicationStatus;

import java.util.List;
import java.util.Objects;

/**
 * Comparison-ready applicant row for MO decision support.
 */
public record CandidateComparisonRow(
    String applicationId,
    String applicantUserId,
    String applicantName,
    ApplicationStatus applicationStatus,
    int rankScore,
    int skillMatchPercent,
    boolean availabilityFit,
    boolean hasCv,
    MoShortlistStatus shortlistStatus,
    List<String> matchedSkills,
    List<String> missingSkills,
    List<String> decisionNotes
) {
    public CandidateComparisonRow {
        Objects.requireNonNull(applicationId);
        Objects.requireNonNull(applicantUserId);
        Objects.requireNonNull(applicantName);
        Objects.requireNonNull(applicationStatus);
        Objects.requireNonNull(matchedSkills);
        Objects.requireNonNull(missingSkills);
        Objects.requireNonNull(decisionNotes);
        matchedSkills = List.copyOf(matchedSkills);
        missingSkills = List.copyOf(missingSkills);
        decisionNotes = List.copyOf(decisionNotes);
    }
}
