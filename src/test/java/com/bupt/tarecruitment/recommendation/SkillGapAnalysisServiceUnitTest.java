package com.bupt.tarecruitment.recommendation;

import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.applicant.ApplicantProfileRepository;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;
import com.bupt.tarecruitment.job.JobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)

/**
 * 验证详细技能差距分析服务的解释、建议和 what-if 模拟。
 */
class SkillGapAnalysisServiceUnitTest {

    @Mock
    private ApplicantProfileRepository profileRepository;

    @Mock
    private JobRepository jobRepository;

    private SkillGapAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new SkillGapAnalysisService(profileRepository, jobRepository);
    }

    @Test
    void shouldBuildDetailedInsightsFromWeakMatchesAndMissingSkills() {
        ApplicantProfile profile = applicantProfile(
            "ta991",
            List.of("Java", "Communication")
        );
        JobPosting job = jobPosting(
            "job991",
            List.of("Programming", "Presentation", "SQL")
        );

        when(profileRepository.findByUserId("ta991")).thenReturn(Optional.of(profile));
        when(jobRepository.findByJobId("job991")).thenReturn(Optional.of(job));

        SkillGapAnalysis analysis = service.analysisForApplicantAndJob("ta991", "job991").orElseThrow();

        assertEquals("job991", analysis.jobId());
        assertEquals("Early fit with several skills to build", analysis.readinessHeadline());
        assertEquals(2, analysis.weakMatchExplanations().size());
        assertEquals("Programming", analysis.weakMatchExplanations().get(0).requiredSkill());
        assertEquals("Java", analysis.weakMatchExplanations().get(0).supportingSkill());
        assertEquals(
            List.of("SQL", "Programming", "Presentation"),
            analysis.prioritySkillSuggestions().stream()
                .map(SkillGapAnalysis.PrioritySkillSuggestion::skill)
                .toList()
        );
        assertEquals(2, analysis.improvementScenarios().size());
        assertEquals("If you add SQL", analysis.improvementScenarios().get(0).title());
        assertEquals(67, analysis.improvementScenarios().get(0).projectedCoveragePercent());
        assertEquals(List.of("SQL"), analysis.improvementScenarios().get(0).newlyMatchedSkills());
        assertEquals("If you add SQL + Programming", analysis.improvementScenarios().get(1).title());
        assertEquals(83, analysis.improvementScenarios().get(1).projectedCoveragePercent());
    }

    @Test
    void shouldReturnReadyStateWhenJobHasNoRequiredSkills() {
        ApplicantProfile profile = applicantProfile(
            "ta992",
            List.of("Java", "Communication")
        );
        JobPosting job = jobPosting(
            "job992",
            List.of()
        );

        when(profileRepository.findByUserId("ta992")).thenReturn(Optional.of(profile));
        when(jobRepository.findByJobId("job992")).thenReturn(Optional.of(job));

        SkillGapAnalysis analysis = service.analysisForApplicantAndJob("ta992", "job992").orElseThrow();

        assertEquals("Strong fit for this role", analysis.readinessHeadline());
        assertTrue(analysis.summary().contains("does not list required skills"));
        assertTrue(analysis.prioritySkillSuggestions().isEmpty());
        assertTrue(analysis.improvementScenarios().isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenApplicantHasNoProfile() {
        when(profileRepository.findByUserId("ta993")).thenReturn(Optional.empty());

        Optional<SkillGapAnalysis> analysis = service.analysisForApplicantAndJob("ta993", "job993");

        assertTrue(analysis.isEmpty());
    }

    private static ApplicantProfile applicantProfile(String userId, List<String> skills) {
        return new ApplicantProfile(
            "profile-" + userId,
            userId,
            "231229" + userId.substring(userId.length() - 3),
            "Insight Test Applicant",
            "Computer Science",
            3,
            "Not Graduated",
            skills,
            List.of("MON-09:00-11:00"),
            List.of("Teaching Assistant")
        );
    }

    private static JobPosting jobPosting(String jobId, List<String> requiredSkills) {
        return new JobPosting(
            jobId,
            "mo-test",
            "Testing Support",
            "COMP991",
            "Support workshops and grading.",
            requiredSkills,
            3,
            List.of("TUE-10:00-12:00"),
            JobStatus.OPEN
        );
    }
}
