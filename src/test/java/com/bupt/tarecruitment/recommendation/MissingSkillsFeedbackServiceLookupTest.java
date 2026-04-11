package com.bupt.tarecruitment.recommendation;

import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.applicant.ApplicantProfileRepository;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;
import com.bupt.tarecruitment.job.JobStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MissingSkillsFeedbackServiceLookupTest {
    private MissingSkillsFeedbackServiceLookupTest() {
    }

    // 这组测试关注 US10 对外入口的 lookup 行为：
    // service 能否根据 applicantUserId 和 jobId 正确找到两边数据，并对非法参数给出预期错误。
    public static void main(String[] args) {
        InMemoryProfileRepository profileRepository = new InMemoryProfileRepository();
        InMemoryJobRepository jobRepository = new InMemoryJobRepository();
        MissingSkillsFeedbackService service = new MissingSkillsFeedbackService(profileRepository, jobRepository);

        profileRepository.save(new ApplicantProfile(
            "profile961",
            "ta961",
            "231229961",
            "Lookup Demo Applicant",
            "Computer Science",
            3,
            "Not Graduated",
            List.of("Java", "Communication"),
            List.of("MON-09:00-11:00"),
            List.of("Teaching Assistant")
        ));

        jobRepository.save(new JobPosting(
            "job961",
            "mo961",
            "Testing Support",
            "COMP961",
            "Support tests and marking.",
            List.of("Java", "SQL"),
            2,
            List.of("TUE-10:00-12:00"),
            JobStatus.OPEN
        ));

        MissingSkillsFeedback feedback = service.feedbackForApplicantAndJob("ta961", "job961")
            .orElseThrow(() -> new IllegalStateException("Expected feedback for existing applicant/job."));
        assertEquals("job961", feedback.jobId(), "Lookup should return feedback for the requested job.");
        assertEquals(List.of("Java"), feedback.matchedSkills(), "Lookup should return matched skills.");
        assertEquals(List.of("SQL"), feedback.missingSkills(), "Lookup should return missing skills.");

        assertTrue(
            service.feedbackForApplicantAndJob("unknownApplicant", "job961").isEmpty(),
            "Applicants without a profile should return Optional.empty."
        );

        expectLookupFailure(
            service,
            "ta961",
            "missing-job",
            "No job exists for jobId"
        );

        expectLookupFailure(
            service,
            "   ",
            "job961",
            "applicantUserId must not be blank."
        );

        expectLookupFailure(
            service,
            "ta961",
            "   ",
            "jobId must not be blank."
        );

        System.out.println("MissingSkillsFeedbackService lookup test passed.");
    }

    private static void expectLookupFailure(
        MissingSkillsFeedbackService service,
        String applicantUserId,
        String jobId,
        String expectedMessagePart
    ) {
        try {
            service.feedbackForApplicantAndJob(applicantUserId, jobId);
            throw new IllegalStateException("Expected feedback lookup to fail.");
        } catch (IllegalArgumentException exception) {
            if (!exception.getMessage().contains(expectedMessagePart)) {
                throw new IllegalStateException("Unexpected lookup failure message: " + exception.getMessage(), exception);
            }
        }
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

    private static final class InMemoryProfileRepository implements ApplicantProfileRepository {
        private final Map<String, ApplicantProfile> profiles = new LinkedHashMap<>();

        @Override
        public Optional<ApplicantProfile> findByUserId(String userId) {
            return Optional.ofNullable(profiles.get(userId));
        }

        @Override
        public Optional<ApplicantProfile> findByStudentId(String studentId) {
            return profiles.values().stream()
                .filter(profile -> profile.studentId().equals(studentId))
                .findFirst();
        }

        @Override
        public List<ApplicantProfile> findAll() {
            return List.copyOf(profiles.values());
        }

        @Override
        public void save(ApplicantProfile profile) {
            profiles.put(profile.userId(), profile);
        }
    }

    private static final class InMemoryJobRepository implements JobRepository {
        private final Map<String, JobPosting> jobs = new LinkedHashMap<>();

        @Override
        public Optional<JobPosting> findByJobId(String jobId) {
            return Optional.ofNullable(jobs.get(jobId));
        }

        @Override
        public List<JobPosting> findAll() {
            return List.copyOf(jobs.values());
        }

        @Override
        public void save(JobPosting jobPosting) {
            jobs.put(jobPosting.jobId(), jobPosting);
        }
    }
}
