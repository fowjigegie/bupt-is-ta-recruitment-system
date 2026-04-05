package com.bupt.tarecruitment;

import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.applicant.ApplicantProfileRepository;
import com.bupt.tarecruitment.applicant.TextFileApplicantProfileRepository;
import com.bupt.tarecruitment.common.storage.DataFile;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;
import com.bupt.tarecruitment.job.JobStatus;
import com.bupt.tarecruitment.job.TextFileJobRepository;
import com.bupt.tarecruitment.recommendation.RecommendationResult;
import com.bupt.tarecruitment.recommendation.RecommendationService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class US09SmokeTest {
    private US09SmokeTest() {
    }

    public static void main(String[] args) {
        try {
            Path tempDataDirectory = Files.createTempDirectory("us09-smoke");
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

            ApplicantProfileRepository profileRepository = new TextFileApplicantProfileRepository(tempDataDirectory);
            JobRepository jobRepository = new TextFileJobRepository(tempDataDirectory);

            profileRepository.save(new ApplicantProfile(
                "profile900",
                "ta900",
                "23120001",
                "Recommendation Demo",
                "Computer Science",
                2,
                "Second-year graduate",
                List.of("Java", "Python", "Communication"),
                List.of("MON-09:00-11:00"),
                List.of("Teaching Assistant", "Lab Support")
            ));

            jobRepository.save(job(
                "job100",
                "mo100",
                "TA for Algorithms",
                "COMP303",
                "Teaching Assistant for algorithm labs.",
                List.of("Java", "Communication"),
                JobStatus.OPEN
            ));
            jobRepository.save(job(
                "job101",
                "mo101",
                "TA for Statistics",
                "STAT210",
                "Lab Support for probability tutorials.",
                List.of("Python"),
                JobStatus.OPEN
            ));
            jobRepository.save(job(
                "job102",
                "mo102",
                "TA for Networks",
                "COMP260",
                "Network configuration practices.",
                List.of("Networking"),
                JobStatus.OPEN
            ));
            jobRepository.save(job(
                "job103",
                "mo103",
                "Closed TA Role",
                "COMP999",
                "Should not be recommended.",
                List.of("Java", "Communication"),
                JobStatus.CLOSED
            ));

            RecommendationService recommendationService = new RecommendationService(profileRepository, jobRepository);
            List<RecommendationResult> results = recommendationService.recommendJobsForApplicant("ta900", 3);

            assertEquals(2, results.size(), "Only matching OPEN jobs should be recommended.");
            assertEquals("job100", results.getFirst().jobId(), "Highest match score should rank first.");
            assertEquals("job101", results.get(1).jobId(), "Lower but valid match should rank second.");
            assertTrue(
                results.getFirst().matchScore() > results.get(1).matchScore(),
                "Top recommendation should have a higher score."
            );
            assertTrue(
                results.getFirst().reasons().stream().anyMatch(reason -> reason.contains("Matches skill")),
                "Top recommendation should explain the skill match."
            );
            assertTrue(
                results.stream().noneMatch(result -> result.jobId().equals("job102") || result.jobId().equals("job103")),
                "Non-matching or CLOSED jobs must not appear in recommendations."
            );

            System.out.println("US09 smoke test passed.");
            System.out.println("Temp data directory: " + tempDataDirectory);
        } catch (Exception exception) {
            throw new IllegalStateException("US09 smoke test failed.", exception);
        }
    }

    private static JobPosting job(
        String jobId,
        String organiserId,
        String title,
        String moduleOrActivity,
        String description,
        List<String> requiredSkills,
        JobStatus status
    ) {
        return new JobPosting(
            jobId,
            organiserId,
            title,
            moduleOrActivity,
            description,
            requiredSkills,
            3,
            List.of("THU-10:00-12:00"),
            status
        );
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
}
