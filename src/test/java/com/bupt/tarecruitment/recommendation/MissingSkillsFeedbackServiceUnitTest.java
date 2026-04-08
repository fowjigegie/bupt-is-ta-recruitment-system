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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissingSkillsFeedbackServiceUnitTest {

    @Mock
    private ApplicantProfileRepository profileRepository;

    @Mock
    private JobRepository jobRepository;

    private MissingSkillsFeedbackService service;

    @BeforeEach
    void setUp() {
        service = new MissingSkillsFeedbackService(profileRepository, jobRepository);
    }

    @Test
    void shouldAnalyzeSkillCoverageIgnoringCaseAndWhitespace() {
        ApplicantProfile profile = applicantProfile(
            "ta981",
            List.of(" java ", "COMMUNICATION", "Teamwork")
        );
        JobPosting job = jobPosting(
            "job981",
            List.of("Java", " communication ", "SQL", " ")
        );

        MissingSkillsFeedback feedback = service.analyze(profile, job);

        assertEquals("job981", feedback.jobId());
        assertEquals(List.of("Java", "communication"), feedback.matchedSkills());
        assertEquals(List.of("SQL"), feedback.missingSkills());
        assertEquals(2, feedback.matchedRequiredSkillCount());
        assertEquals(3, feedback.totalRequiredSkillCount());
        assertEquals(67, feedback.coveragePercent());
        assertFalse(feedback.fullyMatched());
    }

    @Test
    void shouldTreatJobsWithNoRealRequiredSkillsAsFullyMatched() {
        ApplicantProfile profile = applicantProfile(
            "ta982",
            List.of("Java", "Communication")
        );
        JobPosting job = jobPosting(
            "job982",
            List.of(" ", "")
        );

        MissingSkillsFeedback feedback = service.analyze(profile, job);

        assertEquals(List.of(), feedback.matchedSkills());
        assertEquals(List.of(), feedback.missingSkills());
        assertEquals(0, feedback.totalRequiredSkillCount());
        assertEquals(100, feedback.coveragePercent());
        assertTrue(feedback.fullyMatched());
    }

    @Test
    void shouldReturnEmptyWhenApplicantHasNoProfile() {
        when(profileRepository.findByUserId("ta983")).thenReturn(Optional.empty());

        Optional<MissingSkillsFeedback> feedback = service.feedbackForApplicantAndJob("ta983", "job983");

        assertTrue(feedback.isEmpty());
    }

    @Test
    void shouldLookupApplicantAndJobThenReturnFeedback() {
        ApplicantProfile profile = applicantProfile(
            "ta984",
            List.of("Java", "Communication")
        );
        JobPosting job = jobPosting(
            "job984",
            List.of("Java", "SQL")
        );

        when(profileRepository.findByUserId("ta984")).thenReturn(Optional.of(profile));
        when(jobRepository.findByJobId("job984")).thenReturn(Optional.of(job));

        MissingSkillsFeedback feedback = service.feedbackForApplicantAndJob("ta984", "job984")
            .orElseThrow();

        assertEquals("job984", feedback.jobId());
        assertEquals(List.of("Java"), feedback.matchedSkills());
        assertEquals(List.of("SQL"), feedback.missingSkills());
        assertEquals(50, feedback.coveragePercent());
    }

    @Test
    void shouldRejectBlankApplicantUserId() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.feedbackForApplicantAndJob("   ", "job985")
        );

        assertEquals("applicantUserId must not be blank.", exception.getMessage());
    }

    @Test
    void shouldRejectBlankJobId() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.feedbackForApplicantAndJob("ta985", "   ")
        );

        assertEquals("jobId must not be blank.", exception.getMessage());
    }

    @Test
    void shouldRejectMissingJobDuringLookup() {
        ApplicantProfile profile = applicantProfile(
            "ta986",
            List.of("Java", "Communication")
        );

        when(profileRepository.findByUserId("ta986")).thenReturn(Optional.of(profile));
        when(jobRepository.findByJobId("missing-job")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.feedbackForApplicantAndJob("ta986", "missing-job")
        );

        assertEquals("No job exists for jobId: missing-job", exception.getMessage());
    }

    private static ApplicantProfile applicantProfile(String userId, List<String> skills) {
        return new ApplicantProfile(
            "profile-" + userId,
            userId,
            "231229" + userId.substring(userId.length() - 3),
            "Feedback Test Applicant",
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
            "COMP981",
            "Support workshops and grading.",
            requiredSkills,
            3,
            List.of("TUE-10:00-12:00"),
            JobStatus.OPEN
        );
    }
}
