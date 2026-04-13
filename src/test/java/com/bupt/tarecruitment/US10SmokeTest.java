package com.bupt.tarecruitment;

import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.applicant.TextFileApplicantProfileRepository;
import com.bupt.tarecruitment.auth.AuthService;
import com.bupt.tarecruitment.auth.AuthValidator;
import com.bupt.tarecruitment.auth.TextFileUserRepository;
import com.bupt.tarecruitment.auth.UserRole;
import com.bupt.tarecruitment.common.storage.DataFile;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobStatus;
import com.bupt.tarecruitment.job.TextFileJobRepository;
import com.bupt.tarecruitment.recommendation.MissingSkillsFeedback;
import com.bupt.tarecruitment.recommendation.MissingSkillsFeedbackService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 覆盖 US10 场景的冒烟测试。
 */
public final class US10SmokeTest {
    private US10SmokeTest() {
    }

    // 这组 smoke test 关注的是 US10 对外故事是否跑通：
    // 准备 applicant profile 和 job 数据后，
    // 直接验证页面最终会依赖的几个结果：matched skills、missing skills、coveragePercent。
    public static void main(String[] args) {
        try {
            Path tempDataDirectory = Files.createTempDirectory("us10-smoke");
            Files.write(
                tempDataDirectory.resolve(DataFile.USERS.fileName()),
                List.of(DataFile.USERS.initialLines().getFirst()),
                StandardCharsets.UTF_8
            );
            Files.write(
                tempDataDirectory.resolve(DataFile.PROFILES.fileName()),
                List.of(DataFile.PROFILES.initialLines().getFirst()),
                StandardCharsets.UTF_8
            );
            Files.write(
                tempDataDirectory.resolve(DataFile.JOBS.fileName()),
                List.of(DataFile.JOBS.initialLines().getFirst()),
                StandardCharsets.UTF_8
            );

            AuthService authService = new AuthService(
                new TextFileUserRepository(tempDataDirectory),
                new AuthValidator()
            );
            authService.register("ta401", "pass401", UserRole.APPLICANT);
            authService.register("ta402", "pass402", UserRole.APPLICANT);

            TextFileApplicantProfileRepository profileRepository = new TextFileApplicantProfileRepository(tempDataDirectory);
            profileRepository.save(new ApplicantProfile(
                "profile401",
                "ta401",
                "231224401",
                "Skill Gap Demo",
                "Computer Science",
                3,
                "Not Graduated",
                List.of("Java", "Communication"),
                List.of("MON-09:00-11:00"),
                List.of("Teaching Assistant")
            ));
            profileRepository.save(new ApplicantProfile(
                "profile403",
                "ta402",
                "231224402",
                "Normalized Skill Demo",
                "Computer Science",
                2,
                "Not Graduated",
                List.of(" java ", "COMMUNICATION", "Teamwork"),
                List.of("TUE-09:00-11:00"),
                List.of("Lab Support")
            ));

            TextFileJobRepository jobRepository = new TextFileJobRepository(tempDataDirectory);
            jobRepository.save(new JobPosting(
                "job401",
                "mo401",
                "TA for Software Testing",
                "COMP401",
                "Support testing workshops and grading.",
                List.of("Java", "SQL", "Communication"),
                3,
                List.of("TUE-10:00-12:00"),
                JobStatus.OPEN
            ));
            jobRepository.save(new JobPosting(
                "job402",
                "mo401",
                "Invigilation Support",
                "General Support",
                "Help monitor assessments.",
                List.of(),
                2,
                List.of("WED-14:00-16:00"),
                JobStatus.OPEN
            ));
            jobRepository.save(new JobPosting(
                "job403",
                "mo402",
                "Tutorial Support",
                "COMP402",
                "Support tutorials and office hours.",
                List.of("Java", "communication", " SQL "),
                2,
                List.of("THU-10:00-12:00"),
                JobStatus.OPEN
            ));

            MissingSkillsFeedbackService feedbackService = new MissingSkillsFeedbackService(
                profileRepository,
                jobRepository
            );

            MissingSkillsFeedback partialFeedback = feedbackService.feedbackForApplicantAndJob("ta401", "job401")
                .orElseThrow(() -> new IllegalStateException("Expected skill-gap feedback for ta401 / job401."));
            assertEquals(List.of("Java", "Communication"), partialFeedback.matchedSkills(), "Matched skills did not line up.");
            assertEquals(List.of("SQL"), partialFeedback.missingSkills(), "Missing skills did not line up.");
            assertEquals(67, partialFeedback.coveragePercent(), "Unexpected coverage percentage.");
            assertTrue(!partialFeedback.fullyMatched(), "job401 should still have a skill gap.");

            MissingSkillsFeedback noGapFeedback = feedbackService.feedbackForApplicantAndJob("ta401", "job402")
                .orElseThrow(() -> new IllegalStateException("Expected feedback for job402."));
            assertEquals(List.of(), noGapFeedback.matchedSkills(), "Jobs with no required skills should not list matches.");
            assertEquals(List.of(), noGapFeedback.missingSkills(), "Jobs with no required skills should not list missing items.");
            assertEquals(100, noGapFeedback.coveragePercent(), "Jobs with no required skills should show 100% coverage.");
            assertTrue(noGapFeedback.fullyMatched(), "Jobs with no required skills should count as fully matched.");

            MissingSkillsFeedback normalizedFeedback = feedbackService.feedbackForApplicantAndJob("ta402", "job403")
                .orElseThrow(() -> new IllegalStateException("Expected normalized skill feedback for ta402 / job403."));
            assertEquals(
                List.of("Java", "communication"),
                normalizedFeedback.matchedSkills(),
                "Skill matching should ignore case and surrounding spaces."
            );
            assertEquals(List.of("SQL"), normalizedFeedback.missingSkills(), "Only unmatched normalized skills should remain.");
            assertEquals(67, normalizedFeedback.coveragePercent(), "Normalized feedback coverage percentage is incorrect.");

            expectFeedbackFailure(
                feedbackService,
                "ta401",
                "missing-job",
                "No job exists for jobId"
            );

            expectFeedbackFailure(
                feedbackService,
                "   ",
                "job401",
                "applicantUserId must not be blank."
            );

            expectFeedbackFailure(
                feedbackService,
                "ta401",
                "   ",
                "jobId must not be blank."
            );

            System.out.println("US10 smoke test passed.");
            System.out.println("Temp data directory: " + tempDataDirectory);
        } catch (Exception exception) {
            throw new IllegalStateException("US10 smoke test failed.", exception);
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

    private static void expectFeedbackFailure(
        MissingSkillsFeedbackService feedbackService,
        String applicantUserId,
        String jobId,
        String expectedMessagePart
    ) {
        try {
            feedbackService.feedbackForApplicantAndJob(applicantUserId, jobId);
            throw new IllegalStateException(
                "Expected missing-skills feedback lookup to fail for applicantUserId=%s, jobId=%s"
                    .formatted(applicantUserId, jobId)
            );
        } catch (IllegalArgumentException exception) {
            if (!exception.getMessage().contains(expectedMessagePart)) {
                throw new IllegalStateException("Unexpected feedback failure message: " + exception.getMessage(), exception);
            }
        }
    }
}
