package com.bupt.tarecruitment.recommendation;

import java.util.List;
import java.util.Objects;

public record RecommendationResult(
    String jobId,
    int matchScore,
    List<String> reasons
) {
    public RecommendationResult {
        Objects.requireNonNull(jobId);
        Objects.requireNonNull(reasons);
        reasons = List.copyOf(reasons);
    }
}
