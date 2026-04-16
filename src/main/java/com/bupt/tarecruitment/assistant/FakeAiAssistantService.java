package com.bupt.tarecruitment.assistant;

import com.bupt.tarecruitment.application.ApplicantAvailabilityService;
import com.bupt.tarecruitment.application.AvailabilityCheckResult;
import com.bupt.tarecruitment.common.schedule.ScheduleSlot;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;
import com.bupt.tarecruitment.job.JobStatus;
import com.bupt.tarecruitment.recommendation.MissingSkillsFeedback;
import com.bupt.tarecruitment.recommendation.RecommendationResult;
import com.bupt.tarecruitment.recommendation.RecommendationService;
import com.bupt.tarecruitment.recommendation.SkillGapAnalysis;
import com.bupt.tarecruitment.recommendation.SkillGapAnalysisService;

import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Returns fake AI replies backed by local FAQ data and real rule-based business services.
 */
public final class FakeAiAssistantService {
    private static final String FALLBACK_ANSWER =
        "I'm sorry, I don't have a prepared answer for that question yet. Try asking about recommended jobs, missing skills for a named job, the schedule table, or application status.";

    private final TextFileFakeAiRepository repository;
    private final RecommendationService recommendationService;
    private final SkillGapAnalysisService skillGapAnalysisService;
    private final ApplicantAvailabilityService applicantAvailabilityService;
    private final JobRepository jobRepository;

    public FakeAiAssistantService(Path dataDirectory) {
        this(new TextFileFakeAiRepository(dataDirectory), null, null, null, null);
    }

    public FakeAiAssistantService(
        Path dataDirectory,
        RecommendationService recommendationService,
        SkillGapAnalysisService skillGapAnalysisService,
        ApplicantAvailabilityService applicantAvailabilityService,
        JobRepository jobRepository
    ) {
        this(
            new TextFileFakeAiRepository(dataDirectory),
            recommendationService,
            skillGapAnalysisService,
            applicantAvailabilityService,
            jobRepository
        );
    }

    public FakeAiAssistantService(
        TextFileFakeAiRepository repository,
        RecommendationService recommendationService,
        SkillGapAnalysisService skillGapAnalysisService,
        ApplicantAvailabilityService applicantAvailabilityService,
        JobRepository jobRepository
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.recommendationService = recommendationService;
        this.skillGapAnalysisService = skillGapAnalysisService;
        this.applicantAvailabilityService = applicantAvailabilityService;
        this.jobRepository = jobRepository;
    }

    public String answer(String rawQuestion) {
        return answer(rawQuestion, null, null);
    }

    public String answer(String rawQuestion, String applicantUserId, String selectedJobId) {
        String normalizedQuestion = normalize(rawQuestion);
        if (normalizedQuestion.isBlank()) {
            return "Please enter a question first.";
        }

        if (isMissingSkillsQuestion(normalizedQuestion) && hasMissingSkillsContext(applicantUserId)) {
            return buildMissingSkillsAnswer(applicantUserId, selectedJobId, rawQuestion);
        }

        if (isRecommendationQuestion(normalizedQuestion) && hasRecommendationContext(applicantUserId)) {
            return buildRecommendationAnswer(applicantUserId);
        }

        Optional<FakeAiAnswer> exact = repository.findAll().stream()
            .filter(answer -> answer.questions().stream().anyMatch(question -> normalize(question).equals(normalizedQuestion)))
            .findFirst();
        if (exact.isPresent()) {
            return exact.get().answer();
        }

        return repository.findAll().stream()
            .map(answer -> new ScoredAnswer(answer, score(answer, normalizedQuestion)))
            .filter(scored -> scored.score() > 0)
            .max(Comparator.comparingInt(ScoredAnswer::score))
            .map(scored -> scored.answer().answer())
            .orElse(FALLBACK_ANSWER);
    }

    public Path answersFilePath() {
        return repository.answersFilePath();
    }

    private String buildRecommendationAnswer(String applicantUserId) {
        if (recommendationService == null || jobRepository == null) {
            return FALLBACK_ANSWER;
        }

        int poolSize = Math.max(jobRepository.findAll().size(), 12);
        List<RecommendationResult> recommendations = recommendationService.recommendJobsForApplicant(applicantUserId, poolSize);
        if (recommendations.isEmpty()) {
            return "I couldn't generate personalized job recommendations yet. Please create or update your profile skills first, then ask again.";
        }

        List<RecommendedJobView> rankedJobs = recommendations.stream()
            .map(recommendation -> toRecommendedJobView(applicantUserId, recommendation))
            .filter(view -> view.job() != null)
            .sorted(Comparator
                .comparing(RecommendedJobView::fitsAvailability)
                .reversed()
                .thenComparing((RecommendedJobView view) -> view.recommendation().matchScore())
                .reversed()
                .thenComparing(view -> view.job().jobId()))
            .limit(3)
            .toList();

        if (rankedJobs.isEmpty()) {
            return "I couldn't find any open jobs that fit your current profile yet.";
        }

        StringBuilder answer = new StringBuilder("Based on your current profile skills and available time, these are the best jobs for you:");
        for (int index = 0; index < rankedJobs.size(); index++) {
            RecommendedJobView view = rankedJobs.get(index);
            answer.append(System.lineSeparator())
                .append(index + 1)
                .append(". ")
                .append(view.job().title())
                .append(" (")
                .append(view.job().jobId())
                .append(")")
                .append(System.lineSeparator())
                .append("   Why: ")
                .append(view.recommendation().reasons().isEmpty()
                    ? "This job has one of the strongest skill overlaps with your profile."
                    : String.join(" | ", view.recommendation().reasons()))
                .append(System.lineSeparator())
                .append("   Availability: ")
                .append(view.availabilityNote());
        }

        if (rankedJobs.stream().allMatch(RecommendedJobView::fitsAvailability)) {
            answer.append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("These recommendations were ranked by both skill overlap and whether the job fits your current availability.");
        } else {
            answer.append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("Jobs that fit your current availability are ranked first. If you want one of the conflicting jobs, update your availability in Resume Database first.");
        }
        return answer.toString();
    }

    private RecommendedJobView toRecommendedJobView(String applicantUserId, RecommendationResult recommendation) {
        JobPosting job = jobRepository.findByJobId(recommendation.jobId()).orElse(null);
        if (job == null) {
            return new RecommendedJobView(null, recommendation, false, "Job data could not be loaded.");
        }

        if (applicantAvailabilityService == null) {
            return new RecommendedJobView(job, recommendation, true, "Availability check is not enabled in this demo.");
        }

        try {
            Optional<AvailabilityCheckResult> availability = applicantAvailabilityService.availabilityForApplicantAndJob(applicantUserId, job.jobId());
            if (availability.isEmpty()) {
                return new RecommendedJobView(job, recommendation, false, "Create or update your profile availability to check schedule fit.");
            }
            AvailabilityCheckResult result = availability.get();
            if (result.fitsAvailability()) {
                return new RecommendedJobView(job, recommendation, true, "Fits your current availability.");
            }
            return new RecommendedJobView(
                job,
                recommendation,
                false,
                "Conflicts with your current availability on " + formatSlots(result.uncoveredJobSlots()) + "."
            );
        } catch (RuntimeException exception) {
            return new RecommendedJobView(job, recommendation, false, "Availability could not be verified from the current data.");
        }
    }

    private String buildMissingSkillsAnswer(String applicantUserId, String selectedJobId, String rawQuestion) {
        if (skillGapAnalysisService == null || jobRepository == null) {
            return FALLBACK_ANSWER;
        }

        Optional<JobPosting> job = resolveTargetJob(rawQuestion, selectedJobId);
        if (job.isEmpty()) {
            return "Tell me the job title, module code, or job ID you want to ask about. For example: \"What skills am I missing for TA for Software Engineering?\"";
        }

        Optional<SkillGapAnalysis> analysis = skillGapAnalysisService.analysisForApplicantAndJob(applicantUserId, job.get().jobId());
        if (analysis.isEmpty()) {
            return "I couldn't analyze your missing skills yet. Please create or update your applicant profile first.";
        }

        SkillGapAnalysis skillGapAnalysis = analysis.get();
        MissingSkillsFeedback feedback = skillGapAnalysis.feedback();

        if (feedback.totalRequiredSkillCount() == 0) {
            return "For " + job.get().title() + ", there are no required skills listed, so there is no skill gap to analyze.";
        }

        StringBuilder answer = new StringBuilder("For ")
            .append(job.get().title())
            .append(" (")
            .append(job.get().jobId())
            .append("), ")
            .append(skillGapAnalysis.summary())
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("Matched: ")
            .append(formatList(feedback.matchedSkills()))
            .append(System.lineSeparator())
            .append("Weakly matched: ")
            .append(formatList(feedback.weaklyMatchedSkills()))
            .append(System.lineSeparator())
            .append("Missing: ")
            .append(formatList(feedback.missingSkills()));

        if (!skillGapAnalysis.prioritySkillSuggestions().isEmpty()) {
            answer.append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("Best next skills to improve: ")
                .append(skillGapAnalysis.prioritySkillSuggestions().stream()
                    .map(SkillGapAnalysis.PrioritySkillSuggestion::skill)
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("(none)"));
        }

        if (!skillGapAnalysis.improvementScenarios().isEmpty()) {
            SkillGapAnalysis.ImprovementScenario scenario = skillGapAnalysis.improvementScenarios().getFirst();
            answer.append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("What-if: ")
                .append(scenario.explanation());
        }

        return answer.toString();
    }

    private Optional<JobPosting> resolveTargetJob(String rawQuestion, String selectedJobId) {
        if (selectedJobId != null && !selectedJobId.isBlank()) {
            Optional<JobPosting> selectedJob = jobRepository.findByJobId(selectedJobId.trim());
            if (selectedJob.isPresent()) {
                return selectedJob;
            }
        }

        String normalizedQuestion = normalize(rawQuestion);
        return jobRepository.findAll().stream()
            .filter(job -> job.status() == JobStatus.OPEN)
            .map(job -> new ScoredJobCandidate(job, scoreJobMention(job, normalizedQuestion)))
            .filter(candidate -> candidate.score() > 0)
            .max(Comparator
                .comparingInt(ScoredJobCandidate::score)
                .thenComparing(candidate -> candidate.job().jobId()))
            .map(ScoredJobCandidate::job);
    }

    private int scoreJobMention(JobPosting job, String normalizedQuestion) {
        int score = 0;
        String normalizedJobId = normalize(job.jobId());
        String normalizedTitle = normalize(job.title());
        String normalizedModule = normalize(job.moduleOrActivity());

        if (normalizedQuestion.contains(normalizedJobId)) {
            score += 10;
        }
        if (!normalizedTitle.isBlank() && normalizedQuestion.contains(normalizedTitle)) {
            score += 8;
        }
        if (!normalizedModule.isBlank() && normalizedQuestion.contains(normalizedModule)) {
            score += 6;
        }

        List<String> jobTokens = meaningfulTokens(normalizedJobId + " " + normalizedTitle + " " + normalizedModule);
        for (String token : meaningfulTokens(normalizedQuestion)) {
            if (jobTokens.contains(token)) {
                score++;
            }
        }
        return score;
    }

    private boolean hasRecommendationContext(String applicantUserId) {
        return recommendationService != null
            && jobRepository != null
            && applicantUserId != null
            && !applicantUserId.isBlank();
    }

    private boolean hasMissingSkillsContext(String applicantUserId) {
        return skillGapAnalysisService != null
            && jobRepository != null
            && applicantUserId != null
            && !applicantUserId.isBlank();
    }

    private static boolean isRecommendationQuestion(String normalizedQuestion) {
        return containsAny(
            normalizedQuestion,
            "recommend",
            "recommended jobs",
            "best jobs",
            "best job for me",
            "jobs for me",
            "suitable jobs",
            "推荐",
            "最适合我的工作",
            "最适合我的岗位",
            "适合我的工作",
            "适合我的岗位"
        );
    }

    private static boolean isMissingSkillsQuestion(String normalizedQuestion) {
        boolean asksAboutGap = containsAny(
            normalizedQuestion,
            "missing skills",
            "missing skill",
            "what am i missing",
            "what skills am i missing",
            "lack skills",
            "skill gap",
            "缺什么技能",
            "还缺什么技能",
            "缺乏什么技能",
            "缺哪些技能",
            "技能差距"
        );
        boolean mentionsJob = containsAny(
            normalizedQuestion,
            "job",
            "role",
            "岗位",
            "工作",
            "职业",
            "ta for",
            "comp",
            "ebu"
        );
        return asksAboutGap || (mentionsJob && containsAny(normalizedQuestion, "缺", "missing", "lack"));
    }

    private static int score(FakeAiAnswer answer, String normalizedQuestion) {
        int bestScore = 0;
        for (String candidateQuestion : answer.questions()) {
            String normalizedCandidate = normalize(candidateQuestion);
            if (normalizedQuestion.contains(normalizedCandidate) || normalizedCandidate.contains(normalizedQuestion)) {
                bestScore = Math.max(bestScore, 3);
            }

            List<String> candidateTokens = meaningfulTokens(normalizedCandidate);
            int overlap = 0;
            for (String token : meaningfulTokens(normalizedQuestion)) {
                if (candidateTokens.contains(token)) {
                    overlap++;
                }
            }
            bestScore = Math.max(bestScore, overlap);
        }
        return bestScore;
    }

    private static List<String> meaningfulTokens(String value) {
        return List.of(value.split("\\s+")).stream()
            .map(String::trim)
            .filter(token -> token.length() >= 2)
            .toList();
    }

    private static boolean containsAny(String text, String... candidates) {
        for (String candidate : candidates) {
            if (text.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static String formatList(List<String> values) {
        return values == null || values.isEmpty()
            ? "(none)"
            : String.join(", ", values);
    }

    private static String formatSlots(List<String> slots) {
        return slots == null || slots.isEmpty()
            ? "(none)"
            : slots.stream().map(FakeAiAssistantService::formatSlot).reduce((left, right) -> left + ", " + right).orElse("(none)");
    }

    private static String formatSlot(String rawSlot) {
        try {
            ScheduleSlot slot = ScheduleSlot.parse(rawSlot);
            String day = switch (slot.dayCode()) {
                case "MON" -> "Mon";
                case "TUE" -> "Tue";
                case "WED" -> "Wed";
                case "THU" -> "Thu";
                case "FRI" -> "Fri";
                case "SAT" -> "Sat";
                case "SUN" -> "Sun";
                default -> slot.dayCode();
            };
            return day + " " + slot.startTime() + "-" + slot.endTime();
        } catch (IllegalArgumentException exception) {
            return rawSlot;
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT)
            .replaceAll("[\\p{Punct}，。！？；：“”‘’（）?]+", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private record ScoredAnswer(FakeAiAnswer answer, int score) {
    }

    private record ScoredJobCandidate(JobPosting job, int score) {
    }

    private record RecommendedJobView(
        JobPosting job,
        RecommendationResult recommendation,
        boolean fitsAvailability,
        String availabilityNote
    ) {
    }
}
