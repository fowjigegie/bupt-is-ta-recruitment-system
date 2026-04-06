package com.bupt.tarecruitment;

import com.bupt.tarecruitment.application.ApplicationDecisionService;
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
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;
import com.bupt.tarecruitment.job.JobStatus;
import com.bupt.tarecruitment.job.TextFileJobRepository;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public final class US13SmokeTest {
    private US13SmokeTest() {
    }

    public static void main(String[] args) {
        try {
            Path tempDataDirectory = Files.createTempDirectory("us13-smoke");
            bootstrapFiles(tempDataDirectory);

            UserRepository userRepository = new TextFileUserRepository(tempDataDirectory);
            JobRepository jobRepository = new TextFileJobRepository(tempDataDirectory);
            ApplicationRepository applicationRepository = new TextFileApplicationRepository(tempDataDirectory);

            AuthService authService = new AuthService(userRepository, new AuthValidator());
            authService.register("mo101", "pass-mo101", UserRole.MO);
            authService.register("mo201", "pass-mo201", UserRole.MO);
            authService.register("ta101", "pass-ta101", UserRole.APPLICANT);

            seedJobs(jobRepository);
            seedApplications(applicationRepository);

            ApplicationDecisionService decisionService = new ApplicationDecisionService(
                applicationRepository,
                jobRepository,
                userRepository
            );

            JobApplication shortlisted = decisionService.updateStatus("mo101", "application101", ApplicationStatus.SHORTLISTED, "Looks promising");
            assertEquals(ApplicationStatus.SHORTLISTED, shortlisted.status(), "Shortlist action should update status.");
            assertEquals("Looks promising", shortlisted.reviewerNote(), "Reviewer note should be stored.");

            JobApplication accepted = decisionService.updateStatus("mo101", "application101", ApplicationStatus.ACCEPTED, "Offer approved");
            assertEquals(ApplicationStatus.ACCEPTED, accepted.status(), "Accept action should update status.");

            JobApplication rejected = decisionService.updateStatus("mo101", "application102", ApplicationStatus.REJECTED, "Not a fit");
            assertEquals(ApplicationStatus.REJECTED, rejected.status(), "Reject action should update status.");

            expectFailure(
                () -> decisionService.updateStatus("mo101", "application201", ApplicationStatus.REJECTED, "Other organiser job"),
                "own jobs"
            );
            expectFailure(
                () -> decisionService.updateStatus("mo101", "application301", ApplicationStatus.SHORTLISTED, "Withdrawn"),
                "Withdrawn applications cannot be reviewed"
            );

            System.out.println("US13 smoke test passed.");
        } catch (Exception exception) {
            throw new IllegalStateException("US13 smoke test failed.", exception);
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

    private static void seedJobs(JobRepository jobRepository) {
        jobRepository.save(new JobPosting(
            "job101",
            "mo101",
            "TA for Software Engineering",
            "COMP101",
            "Support tutorials",
            List.of("Java"),
            3,
            List.of("MON-10:00-12:00"),
            JobStatus.OPEN
        ));
        jobRepository.save(new JobPosting(
            "job201",
            "mo201",
            "TA for Databases",
            "COMP202",
            "Support labs",
            List.of("SQL"),
            2,
            List.of("TUE-10:00-12:00"),
            JobStatus.OPEN
        ));
        jobRepository.save(new JobPosting(
            "job301",
            "mo101",
            "TA for Networks",
            "COMP303",
            "Support labs",
            List.of("Networking"),
            2,
            List.of("WED-10:00-12:00"),
            JobStatus.OPEN
        ));
    }

    private static void seedApplications(ApplicationRepository applicationRepository) {
        applicationRepository.save(buildApplication("application101", "job101", "ta101", "cv101", ApplicationStatus.SUBMITTED));
        applicationRepository.save(buildApplication("application102", "job101", "ta101", "cv102", ApplicationStatus.SUBMITTED));
        applicationRepository.save(buildApplication("application201", "job201", "ta101", "cv201", ApplicationStatus.SUBMITTED));
        applicationRepository.save(buildApplication("application301", "job301", "ta101", "cv301", ApplicationStatus.WITHDRAWN));
    }

    private static JobApplication buildApplication(
        String applicationId,
        String jobId,
        String applicantUserId,
        String cvId,
        ApplicationStatus status
    ) {
        return new JobApplication(
            applicationId,
            jobId,
            applicantUserId,
            cvId,
            status,
            LocalDateTime.parse("2026-04-06T10:00:00"),
            ""
        );
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

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException(message + " Expected: " + expected + ", actual: " + actual);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}
