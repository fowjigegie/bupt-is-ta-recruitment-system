package com.bupt.tarecruitment;

import com.bupt.tarecruitment.auth.AuthService;
import com.bupt.tarecruitment.auth.AuthValidator;
import com.bupt.tarecruitment.auth.TextFileUserRepository;
import com.bupt.tarecruitment.auth.UserRepository;
import com.bupt.tarecruitment.auth.UserRole;
import com.bupt.tarecruitment.common.storage.DataFile;
import com.bupt.tarecruitment.job.JobIdGenerator;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobPostingService;
import com.bupt.tarecruitment.job.JobRepository;
import com.bupt.tarecruitment.job.JobStatus;
import com.bupt.tarecruitment.job.TextFileJobRepository;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class US11SmokeTest {
    private US11SmokeTest() {
    }

    public static void main(String[] args) {
        try {
            Path tempDataDirectory = Files.createTempDirectory("us11-smoke");
            bootstrapFiles(tempDataDirectory);

            UserRepository userRepository = new TextFileUserRepository(tempDataDirectory);
            JobRepository jobRepository = new TextFileJobRepository(tempDataDirectory);
            AuthService authService = new AuthService(userRepository, new AuthValidator());
            authService.register("mo101", "pass-mo101", UserRole.MO);
            authService.register("ta101", "pass-ta101", UserRole.APPLICANT);

            JobPostingService jobPostingService = new JobPostingService(
                jobRepository,
                new JobIdGenerator(jobRepository),
                userRepository
            );

            JobPosting created = jobPostingService.publish(
                "mo101",
                "TA for Software Engineering",
                "EBU6304",
                "Support tutorials and grading.",
                List.of("Java", "Communication"),
                3,
                List.of("MON-09:00-11:00")
            );

            assertEquals(JobStatus.OPEN, created.status(), "US11: Newly posted jobs should be OPEN.");
            assertEquals("mo101", created.organiserId(), "US11: Organiser should be linked correctly.");
            assertEquals("TA for Software Engineering", created.title(), "US11: Title should be saved.");

            JobPosting stored = jobRepository.findByJobId(created.jobId())
                .orElseThrow(() -> new IllegalStateException("US11: Posted job was not persisted."));
            assertEquals(created.jobId(), stored.jobId(), "US11: Persisted job id mismatch.");
            assertEquals(3, stored.weeklyHours(), "US11: Workload should be saved.");
            assertEquals(1, stored.scheduleSlots().size(), "US11: Schedule should be saved.");

            long openJobs = jobRepository.findAll().stream()
                .filter(job -> job.status() == JobStatus.OPEN)
                .count();
            assertTrue(openJobs >= 1, "US11: Posted job should be visible as an OPEN job.");

            expectFailure(
                () -> jobPostingService.publish(
                    "mo101",
                    "  ",
                    "COMP000",
                    "desc",
                    List.of("Java"),
                    2,
                    List.of("TUE-10:00-12:00")
                ),
                "title"
            );

            expectFailure(
                () -> jobPostingService.publish(
                    "ta101",
                    "TA for Databases",
                    "COMP202",
                    "desc",
                    List.of("SQL"),
                    2,
                    List.of("WED-10:00-12:00")
                ),
                "MO"
            );

            System.out.println("US11 smoke test passed.");
        } catch (Exception exception) {
            throw new IllegalStateException("US11 smoke test failed.", exception);
        }
    }

    private static void bootstrapFiles(Path tempDataDirectory) throws Exception {
        Files.write(
            tempDataDirectory.resolve(DataFile.USERS.fileName()),
            List.of(DataFile.USERS.initialLines().getFirst()),
            StandardCharsets.UTF_8
        );
        Files.write(
            tempDataDirectory.resolve(DataFile.JOBS.fileName()),
            List.of(DataFile.JOBS.initialLines().getFirst()),
            StandardCharsets.UTF_8
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

    private static void expectFailure(ThrowingRunnable runnable, String expectedMessagePart) {
        try {
            runnable.run();
            throw new IllegalStateException("Expected failure but operation succeeded.");
        } catch (IllegalArgumentException exception) {
            if (!exception.getMessage().contains(expectedMessagePart)) {
                throw new IllegalStateException("Unexpected failure message: " + exception.getMessage(), exception);
            }
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}
