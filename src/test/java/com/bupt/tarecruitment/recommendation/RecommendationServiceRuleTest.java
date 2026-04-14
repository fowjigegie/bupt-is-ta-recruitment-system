package com.bupt.tarecruitment.recommendation;

import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.applicant.ApplicantProfileRepository;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;
import com.bupt.tarecruitment.job.JobStatus;

import java.util.List;
import java.util.Optional;

/**
 * 验证岗位推荐中的精确匹配与弱匹配规则。
 */
public final class RecommendationServiceRuleTest {
    private RecommendationServiceRuleTest() {
    }

    public static void main(String[] args) {
        ApplicantProfile profile = new ApplicantProfile(
            "profile980",
            "ta980",
            "231229980",
            "Recommendation Rule Applicant",
            "Computer Science",
            2,
            "Not Graduated",
            List.of("Java", "Presentation"),
            List.of("MON-09:00-11:00"),
            List.of("Teaching Assistant")
        );

        JobPosting exactMatchJob = new JobPosting(
            "job980",
            "mo980",
            "Algorithms Support",
            "COMP980",
            "Teaching Assistant for programming labs.",
            List.of("Java"),
            3,
            List.of("TUE-10:00-12:00"),
            JobStatus.OPEN
        );
        JobPosting weakMatchJob = new JobPosting(
            "job981",
            "mo981",
            "Intro Programming Support",
            "COMP981",
            "Support beginner workshops.",
            List.of("Programming", "Communication"),
            3,
            List.of("WED-10:00-12:00"),
            JobStatus.OPEN
        );
        JobPosting unrelatedJob = new JobPosting(
            "job982",
            "mo982",
            "Hardware Lab Support",
            "COMP982",
            "Support embedded labs.",
            List.of("Hardware"),
            3,
            List.of("THU-10:00-12:00"),
            JobStatus.OPEN
        );

        RecommendationService service = new RecommendationService(
            new StubProfileRepository(profile),
            new StubJobRepository(List.of(exactMatchJob, weakMatchJob, unrelatedJob))
        );

        List<RecommendationResult> results = service.recommendJobsForApplicant("ta980", 5);

        assertEquals(2, results.size(), "Only exact or weakly related jobs should be recommended.");
        assertEquals("job980", results.get(0).jobId(), "Exact matches should rank above weak matches.");
        assertEquals("job981", results.get(1).jobId(), "Weakly matched jobs should still appear after exact matches.");
        assertTrue(
            results.get(1).reasons().stream().anyMatch(reason -> reason.contains("Related skill match")),
            "Weakly matched recommendations should explain the related-skill reason."
        );

        System.out.println("RecommendationService rule test passed.");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException(message + " Expected: " + expected + ", actual: " + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static final class StubProfileRepository implements ApplicantProfileRepository {
        private final ApplicantProfile profile;

        private StubProfileRepository(ApplicantProfile profile) {
            this.profile = profile;
        }

        @Override
        public Optional<ApplicantProfile> findByUserId(String userId) {
            return profile.userId().equals(userId) ? Optional.of(profile) : Optional.empty();
        }

        @Override
        public Optional<ApplicantProfile> findByStudentId(String studentId) {
            return Optional.empty();
        }

        @Override
        public List<ApplicantProfile> findAll() {
            return List.of(profile);
        }

        @Override
        public void save(ApplicantProfile profile) {
            throw new UnsupportedOperationException("Not needed in this rule test.");
        }
    }

    private static final class StubJobRepository implements JobRepository {
        private final List<JobPosting> jobs;

        private StubJobRepository(List<JobPosting> jobs) {
            this.jobs = jobs;
        }

        @Override
        public Optional<JobPosting> findByJobId(String jobId) {
            return jobs.stream().filter(job -> job.jobId().equals(jobId)).findFirst();
        }

        @Override
        public List<JobPosting> findAll() {
            return jobs;
        }

        @Override
        public void save(JobPosting jobPosting) {
            throw new UnsupportedOperationException("Not needed in this rule test.");
        }
    }
}
