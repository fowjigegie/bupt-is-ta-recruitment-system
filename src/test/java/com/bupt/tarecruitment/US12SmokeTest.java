package com.bupt.tarecruitment;

import com.bupt.tarecruitment.application.ApplicationRepository;
import com.bupt.tarecruitment.application.ApplicationStatus;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.application.TextFileApplicationRepository;
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
import java.time.LocalDateTime;
import java.util.List;

/**
 * 覆盖 US12 场景的冒烟测试。
 */
public final class US12SmokeTest {
    private US12SmokeTest() {
    }

    public static void main(String[] args) {
        try {
            Path tempDataDirectory = Files.createTempDirectory("us12-smoke");
            bootstrapFiles(tempDataDirectory);

            UserRepository userRepository = new TextFileUserRepository(tempDataDirectory);
            JobRepository jobRepository = new TextFileJobRepository(tempDataDirectory);
            ApplicationRepository applicationRepository = new TextFileApplicationRepository(tempDataDirectory);
            AuthService authService = new AuthService(userRepository, new AuthValidator());
            authService.register("mo101", "pass-mo101", UserRole.MO);
            authService.register("ta101", "pass-ta101", UserRole.APPLICANT);

            // Existing applications must remain visible even if the job is later closed.
            applicationRepository.save(new JobApplication(
                "application901",
                "job001",
                "ta101",
                "cv001",
                ApplicationStatus.SUBMITTED,
                LocalDateTime.parse("2026-04-08T10:00:00"),
                ""
            ));
            long applicationsBeforeClose = applicationRepository.findAll().stream()
                .filter(application -> application.jobId().equals("job001"))
                .count();

            JobPostingService jobPostingService = new JobPostingService(
                jobRepository,
                new JobIdGenerator(jobRepository),
                userRepository
            );

            JobPosting created = jobPostingService.publish(
                "mo101",
                "TA for Algorithms",
                "COMP201",
                "Support weekly tutorials",
                List.of("Algorithms", "Communication"),
                3,
                List.of("MON-10:00-12:00")
            );
            assertEquals(JobStatus.OPEN, created.status(), "New jobs should be OPEN.");

            JobPosting edited = new JobPosting(
                created.jobId(),
                created.organiserId(),
                "TA for Advanced Algorithms",
                created.moduleOrActivity(),
                "Support tutorials and office hours",
                List.of("Algorithms", "Communication", "Problem Solving"),
                4,
                List.of("MON-10:00-12:00", "WED-14:00-16:00"),
                JobStatus.OPEN
            );
            jobPostingService.publish(edited);

            JobPosting storedEdited = jobRepository.findByJobId(created.jobId())
                .orElseThrow(() -> new IllegalStateException("Edited job not found."));
            assertEquals("TA for Advanced Algorithms", storedEdited.title(), "Edited title should be saved.");
            assertEquals(4, storedEdited.weeklyHours(), "Edited weekly hours should be saved.");
            assertEquals(3, storedEdited.requiredSkills().size(), "Edited skills should be saved.");
            assertEquals(2, storedEdited.scheduleSlots().size(), "Edited schedule slots should be saved.");

            JobPosting closed = new JobPosting(
                storedEdited.jobId(),
                storedEdited.organiserId(),
                storedEdited.title(),
                storedEdited.moduleOrActivity(),
                storedEdited.description(),
                storedEdited.requiredSkills(),
                storedEdited.weeklyHours(),
                storedEdited.scheduleSlots(),
                JobStatus.CLOSED
            );
            jobPostingService.publish(closed);

            JobPosting storedClosed = jobRepository.findByJobId(created.jobId())
                .orElseThrow(() -> new IllegalStateException("Closed job not found."));
            assertEquals(JobStatus.CLOSED, storedClosed.status(), "Closed status should be saved.");

            // US12 AC#3: closed jobs should no longer be available as OPEN jobs.
            boolean appearsInOpenJobs = jobRepository.findAll().stream()
                .anyMatch(job -> job.jobId().equals(storedClosed.jobId()) && job.status() == JobStatus.OPEN);
            assertTrue(!appearsInOpenJobs, "Closed job should not appear in OPEN job list.");

            long applicationsAfterClose = applicationRepository.findAll().stream()
                .filter(application -> application.jobId().equals("job001"))
                .count();
            assertEquals(applicationsBeforeClose, applicationsAfterClose, "Existing applications should remain visible after closing.");

            System.out.println("US12 smoke test passed.");
        } catch (Exception exception) {
            throw new IllegalStateException("US12 smoke test failed.", exception);
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
        Files.write(
            tempDataDirectory.resolve(DataFile.APPLICATIONS.fileName()),
            List.of(DataFile.APPLICATIONS.initialLines().getFirst()),
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
}
