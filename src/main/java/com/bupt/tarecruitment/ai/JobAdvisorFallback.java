package com.bupt.tarecruitment.ai;

import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.job.JobPosting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Simple offline suggestions when no AI API key is configured or the network call fails.
 */
public final class JobAdvisorFallback {
    private JobAdvisorFallback() {
    }

    public static String localAdvice(Optional<ApplicantProfile> profile, List<JobPosting> openJobs) {
        if (openJobs.isEmpty()) {
            return "No OPEN postings.";
        }
        if (profile.isEmpty()) {
            return "No profile in Resume DB — add one first.\nOPEN jobs: " + openJobs.size() + ".";
        }

        ApplicantProfile p = profile.get();
        Set<String> userSkillTokens = p.skills().stream()
            .map(s -> s.toLowerCase(Locale.ROOT).trim())
            .filter(s -> !s.isBlank())
            .collect(Collectors.toSet());

        record JobScore(JobPosting posting, int score, List<String> matchedSkills) {
        }

        List<JobScore> scored = new ArrayList<>();
        for (JobPosting posting : openJobs) {
            List<String> matched = new ArrayList<>();
            int score = 0;
            for (String req : posting.requiredSkills()) {
                if (req == null || req.isBlank()) {
                    continue;
                }
                String r = req.toLowerCase(Locale.ROOT).trim();
                for (String us : userSkillTokens) {
                    if (us.contains(r) || r.contains(us)) {
                        score += 2;
                        matched.add(req);
                        break;
                    }
                }
            }
            String haystack = (posting.title() + " " + posting.description() + " " + posting.moduleOrActivity())
                .toLowerCase(Locale.ROOT);
            for (String want : p.desiredPositions()) {
                if (want != null && !want.isBlank() && haystack.contains(want.toLowerCase(Locale.ROOT).trim())) {
                    score += 1;
                }
            }
            if (p.programme() != null && haystack.contains(p.programme().toLowerCase(Locale.ROOT))) {
                score += 1;
            }
            scored.add(new JobScore(posting, score, matched.stream().distinct().toList()));
        }

        scored.sort(Comparator.comparingInt(JobScore::score).reversed().thenComparing(s -> s.posting.jobId()));

        StringBuilder out = new StringBuilder();
        out.append("(Offline) Skill match vs. required skills:\n\n");
        out.append("Applicant: ").append(p.fullName()).append(" | Programme: ").append(p.programme()).append("\n\n");

        int limit = Math.min(5, scored.size());
        for (int i = 0; i < limit; i++) {
            JobScore s = scored.get(i);
            out.append(i + 1).append(". ").append(s.posting.title()).append(" (").append(s.posting.jobId()).append(")\n");
            out.append("   Heuristic score: ").append(s.score).append("\n");
            if (s.matchedSkills.isEmpty()) {
                out.append("   Low skill overlap — check the posting.\n");
            } else {
                out.append("   Overlap: ").append(String.join(", ", s.matchedSkills)).append("\n");
            }
            out.append("   Weekly hours: ").append(s.posting.weeklyHours()).append("h | Module: ")
                .append(s.posting.moduleOrActivity()).append("\n\n");
        }

        out.append("Tip: OPENAI_API_KEY → fuller AI replies.");
        return out.toString();
    }
}
