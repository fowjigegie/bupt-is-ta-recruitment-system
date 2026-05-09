package com.bupt.tarecruitment.mo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Builds comparison rows from the existing ranking result plus MO shortlist labels.
 */
public final class MoCandidateComparisonService {
    private final MoApplicantRankingService rankingService;
    private final MoShortlistService shortlistService;

    public MoCandidateComparisonService(
        MoApplicantRankingService rankingService,
        MoShortlistService shortlistService
    ) {
        this.rankingService = Objects.requireNonNull(rankingService);
        this.shortlistService = Objects.requireNonNull(shortlistService);
    }

    public List<CandidateComparisonRow> compareTopCandidates(String jobId, int limit) {
        return rankingService.rankApplicantsForJob(jobId).stream()
            .sorted(Comparator
                .comparingInt(RankedApplicantCandidate::rankScore).reversed()
                .thenComparing(RankedApplicantCandidate::applicantName))
            .limit(Math.max(1, limit))
            .map(this::toComparisonRow)
            .toList();
    }

    private CandidateComparisonRow toComparisonRow(RankedApplicantCandidate candidate) {
        MoShortlistStatus shortlistStatus = shortlistService.findByApplicationId(candidate.applicationId())
            .map(MoShortlistEntry::status)
            .orElse(null);

        List<String> notes = new ArrayList<>(candidate.reasons());
        if (!candidate.missingSkills().isEmpty()) {
            notes.add("Missing skills: " + String.join(", ", candidate.missingSkills()));
        }
        if (shortlistStatus != null) {
            notes.add("MO candidate pool label: " + shortlistStatus.name());
        }

        return new CandidateComparisonRow(
            candidate.applicationId(),
            candidate.applicantUserId(),
            candidate.applicantName(),
            candidate.status(),
            candidate.rankScore(),
            candidate.skillMatchPercent(),
            candidate.availabilityFit(),
            candidate.hasCv(),
            shortlistStatus,
            candidate.matchedSkills(),
            candidate.missingSkills(),
            notes
        );
    }
}
