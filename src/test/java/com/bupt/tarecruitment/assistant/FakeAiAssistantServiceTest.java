package com.bupt.tarecruitment.assistant;

import com.bupt.tarecruitment.application.ApplicantAvailabilityService;
import com.bupt.tarecruitment.application.AvailabilityCheckResult;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;
import com.bupt.tarecruitment.job.JobStatus;
import com.bupt.tarecruitment.recommendation.MissingSkillsFeedback;
import com.bupt.tarecruitment.recommendation.RecommendationResult;
import com.bupt.tarecruitment.recommendation.RecommendationService;
import com.bupt.tarecruitment.recommendation.SkillGapAnalysis;
import com.bupt.tarecruitment.recommendation.SkillGapAnalysisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the fake AI assistant's FAQ and dynamic business-backed answers.
 */
class FakeAiAssistantServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldReturnPreparedAnswerForExactQuestion() {
        FakeAiAssistantService service = new FakeAiAssistantService(tempDirectory);

        String answer = service.answer("what is weakly matched");

        assertEquals(
            "Weakly matched means your profile shows related experience, but not a direct skill-by-skill match yet.",
            answer
        );
    }

    @Test
    void shouldReturnPreparedAnswerForSimilarQuestion() {
        FakeAiAssistantService service = new FakeAiAssistantService(tempDirectory);

        String answer = service.answer("Could you tell me how skill matching works?");

        assertTrue(answer.contains("Skill matching is rule-based"));
    }

    @Test
    void shouldReturnRecommendationAnswerThatMentionsAvailability() {
        RecommendationService recommendationService = mock(RecommendationService.class);
        SkillGapAnalysisService skillGapAnalysisService = mock(SkillGapAnalysisService.class);
        ApplicantAvailabilityService applicantAvailabilityService = mock(ApplicantAvailabilityService.class);
        JobRepository jobRepository = mock(JobRepository.class);

        when(jobRepository.findAll()).thenReturn(List.of(
            jobPosting("job001", "TA for Software Engineering"),
            jobPosting("job002", "TA for Computer Science")
        ));
        when(recommendationService.recommendJobsForApplicant("ta900", 12)).thenReturn(List.of(
            new RecommendationResult("job001", 8, List.of("Matches skill: Java", "Related skill match: Programming")),
            new RecommendationResult("job002", 6, List.of("Matches skill: Communication"))
        ));
        when(jobRepository.findByJobId("job001")).thenReturn(Optional.of(jobPosting("job001", "TA for Software Engineering")));
        when(jobRepository.findByJobId("job002")).thenReturn(Optional.of(jobPosting("job002", "TA for Computer Science")));
        when(applicantAvailabilityService.availabilityForApplicantAndJob("ta900", "job001")).thenReturn(
            Optional.of(new AvailabilityCheckResult("job001", List.of("MON-09:50-11:25"), List.of()))
        );
        when(applicantAvailabilityService.availabilityForApplicantAndJob("ta900", "job002")).thenReturn(
            Optional.of(new AvailabilityCheckResult("job002", List.of(), List.of("TUE-09:50-11:25")))
        );

        FakeAiAssistantService service = new FakeAiAssistantService(
            new TextFileFakeAiRepository(tempDirectory),
            recommendationService,
            skillGapAnalysisService,
            applicantAvailabilityService,
            jobRepository
        );

        String answer = service.answer("recommend the best jobs for me", "ta900", null);

        assertTrue(answer.contains("Based on your current profile skills and available time"));
        assertTrue(answer.contains("TA for Software Engineering"));
        assertTrue(answer.contains("Fits your current availability."));
        assertTrue(answer.contains("Conflicts with your current availability"));
    }

    @Test
    void shouldResolveMissingSkillsByNamedJobInQuestion() {
        RecommendationService recommendationService = mock(RecommendationService.class);
        SkillGapAnalysisService skillGapAnalysisService = mock(SkillGapAnalysisService.class);
        ApplicantAvailabilityService applicantAvailabilityService = mock(ApplicantAvailabilityService.class);
        JobRepository jobRepository = mock(JobRepository.class);

        JobPosting job = jobPosting("job001", "TA for Software Engineering");
        MissingSkillsFeedback feedback = new MissingSkillsFeedback(
            "job001",
            List.of("Java"),
            List.of("Programming"),
            List.of("SQL"),
            1,
            1,
            3,
            50
        );
        SkillGapAnalysis analysis = new SkillGapAnalysis(
            "job001",
            "Developing fit with clear next steps",
            "You directly match 1 listed skill, show related evidence for 1, and still need 1 more to close the gap.",
            feedback,
            List.of(new SkillGapAnalysis.WeakMatchExplanation(
                "Programming",
                "Java",
                "Java gives you related background for Programming."
            )),
            List.of(new SkillGapAnalysis.PrioritySkillSuggestion(
                "SQL",
                "SQL is a direct listed requirement."
            )),
            List.of(new SkillGapAnalysis.ImprovementScenario(
                "If you add SQL",
                List.of("SQL"),
                List.of("SQL"),
                67,
                "Readiness could rise from 50% to 67%, with direct improvement in SQL."
            ))
        );

        when(jobRepository.findAll()).thenReturn(List.of(job));
        when(jobRepository.findByJobId("job001")).thenReturn(Optional.of(job));
        when(skillGapAnalysisService.analysisForApplicantAndJob("ta900", "job001")).thenReturn(Optional.of(analysis));

        FakeAiAssistantService service = new FakeAiAssistantService(
            new TextFileFakeAiRepository(tempDirectory),
            recommendationService,
            skillGapAnalysisService,
            applicantAvailabilityService,
            jobRepository
        );

        String answer = service.answer("What skills am I missing for TA for Software Engineering?", "ta900", null);

        assertTrue(answer.contains("TA for Software Engineering"));
        assertTrue(answer.contains("Missing: SQL"));
        assertTrue(answer.contains("Best next skills to improve"));
        assertTrue(answer.contains("What-if"));
    }

    @Test
    void shouldFallbackWhenNoPreparedAnswerExists() {
        FakeAiAssistantService service = new FakeAiAssistantService(tempDirectory);

        String answer = service.answer("tell me something about moon rocks");

        assertTrue(answer.contains("I'm sorry"));
    }

    private static JobPosting jobPosting(String jobId, String title) {
        return new JobPosting(
            jobId,
            "mo001",
            title,
            "COMP001",
            "Demo description",
            List.of("Java"),
            2,
            List.of("MON-09:50-11:25"),
            JobStatus.OPEN
        );
    }
}
