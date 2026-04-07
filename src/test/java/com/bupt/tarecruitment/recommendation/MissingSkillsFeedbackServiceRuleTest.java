package com.bupt.tarecruitment.recommendation;

import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.applicant.ApplicantProfileRepository;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;
import com.bupt.tarecruitment.job.JobStatus;

import java.util.List;
import java.util.Optional;

public final class MissingSkillsFeedbackServiceRuleTest {
    private MissingSkillsFeedbackServiceRuleTest() {
    }

    public static void main(String[] args) {
        MissingSkillsFeedbackService service = new MissingSkillsFeedbackService(
            new NoOpProfileRepository(),
            new NoOpJobRepository()
        );

        ApplicantProfile applicantProfile = new ApplicantProfile(
            "profile951",
            "ta951",
            "231229951",
            "Feedback Rule Applicant",
            "Computer Science",
            3,
            "Not Graduated",
            List.of(" java ", "COMMUNICATION", "Teamwork"),
            List.of("MON-09:00-11:00"),
            List.of("Teaching Assistant")
        );

        JobPosting mixedSkillJob = new JobPosting(
            "job951",
            "mo951",
            "Software Testing Support",
            "COMP951",
            "Support workshops and grading.",
            List.of("Java", " communication ", "SQL", " "),
            3,
            List.of("TUE-10:00-12:00"),
            JobStatus.OPEN
        );

        MissingSkillsFeedback feedback = service.analyze(applicantProfile, mixedSkillJob);
        assertEquals(List.of("Java", "communication"), feedback.matchedSkills(), "Matched skills should be normalized.");
        assertEquals(List.of("SQL"), feedback.missingSkills(), "Only real unmatched skills should remain.");
        assertEquals(2, feedback.matchedRequiredSkillCount(), "Matched required skill count is incorrect.");
        assertEquals(3, feedback.totalRequiredSkillCount(), "Blank required skills should be ignored.");
        assertEquals(67, feedback.coveragePercent(), "Coverage percentage is incorrect.");

        JobPosting noSkillJob = new JobPosting(
            "job952",
            "mo951",
            "General Support",
            "General",
            "Help with logistics.",
            List.of(" ", ""),
            2,
            List.of("WED-14:00-16:00"),
            JobStatus.OPEN
        );

        MissingSkillsFeedback noSkillFeedback = service.analyze(applicantProfile, noSkillJob);
        assertEquals(List.of(), noSkillFeedback.matchedSkills(), "Jobs with blank required skills should not list matches.");
        assertEquals(List.of(), noSkillFeedback.missingSkills(), "Jobs with blank required skills should not list missing skills.");
        assertEquals(0, noSkillFeedback.totalRequiredSkillCount(), "Blank required skills should not count.");
        assertEquals(100, noSkillFeedback.coveragePercent(), "No-skill jobs should report full coverage.");

        System.out.println("MissingSkillsFeedbackService rule test passed.");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException(message + " Expected: " + expected + ", actual: " + actual);
        }
    }

    private static final class NoOpProfileRepository implements ApplicantProfileRepository {
        @Override
        public Optional<ApplicantProfile> findByUserId(String userId) {
            return Optional.empty();
        }

        @Override
        public Optional<ApplicantProfile> findByStudentId(String studentId) {
            return Optional.empty();
        }

        @Override
        public List<ApplicantProfile> findAll() {
            return List.of();
        }

        @Override
        public void save(ApplicantProfile profile) {
            throw new UnsupportedOperationException("Not needed in this rule test.");
        }
    }

    private static final class NoOpJobRepository implements JobRepository {
        @Override
        public Optional<JobPosting> findByJobId(String jobId) {
            return Optional.empty();
        }

        @Override
        public List<JobPosting> findAll() {
            return List.of();
        }

        @Override
        public void save(JobPosting jobPosting) {
            throw new UnsupportedOperationException("Not needed in this rule test.");
        }
    }
}
