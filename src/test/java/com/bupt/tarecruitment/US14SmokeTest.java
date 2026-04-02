package com.bupt.tarecruitment;

import com.bupt.tarecruitment.admin.AdminWorkloadService;
import com.bupt.tarecruitment.admin.WorkloadConflict;
import com.bupt.tarecruitment.admin.WorkloadSummary;
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

public final class US14SmokeTest {
    private US14SmokeTest() {
    }

    public static void main(String[] args) {
        try {
            Path tempDataDirectory = Files.createTempDirectory("us14-smoke");
            bootstrapDataFiles(tempDataDirectory);

            UserRepository userRepository = new TextFileUserRepository(tempDataDirectory);
            ApplicationRepository applicationRepository = new TextFileApplicationRepository(tempDataDirectory);
            JobRepository jobRepository = new TextFileJobRepository(tempDataDirectory);

            AuthService authService = new AuthService(userRepository, new AuthValidator());
            authService.register("admin101", "pass-admin101", UserRole.ADMIN);
            authService.register("mo101", "pass-mo101", UserRole.MO);
            authService.register("mo201", "pass-mo201", UserRole.MO);
            authService.register("ta101", "pass-ta101", UserRole.APPLICANT);
            authService.register("ta102", "pass-ta102", UserRole.APPLICANT);

            seedJobs(jobRepository);
            seedApplications(applicationRepository);

            ApplicationDecisionService decisionService = new ApplicationDecisionService(
                applicationRepository,
                jobRepository,
                userRepository
            );
            AdminWorkloadService workloadService = new AdminWorkloadService(
                applicationRepository,
                jobRepository,
                userRepository
            );

            decisionService.updateStatus("mo101", "application101", ApplicationStatus.ACCEPTED, "Core tutorial support");
            decisionService.updateStatus("mo101", "application102", ApplicationStatus.ACCEPTED, "Lab slot accepted");
            decisionService.updateStatus("mo101", "application103", ApplicationStatus.ACCEPTED, "Edge-touch accepted");
            decisionService.updateStatus("mo101", "application104", ApplicationStatus.ACCEPTED, "Contained overlap");
            decisionService.updateStatus("mo101", "application105", ApplicationStatus.ACCEPTED, "Exact overlap");
            decisionService.updateStatus("mo101", "application106", ApplicationStatus.SHORTLISTED, "Wait list");
            decisionService.updateStatus("mo101", "application201", ApplicationStatus.ACCEPTED, "Second TA accepted");

            expectDecisionFailure(
                () -> decisionService.updateStatus("mo101", "application301", ApplicationStatus.ACCEPTED, "Should fail"),
                "own jobs"
            );
            expectDecisionFailure(
                () -> decisionService.updateStatus("mo101", "application999", ApplicationStatus.REJECTED, "Missing"),
                "Application not found"
            );
            expectDecisionFailure(
                () -> decisionService.updateStatus("mo101", "application401", ApplicationStatus.REJECTED, "Withdrawn"),
                "Withdrawn applications cannot be reviewed"
            );

            List<WorkloadSummary> summaries = workloadService.listAcceptedTaWorkloads(10);
            assertEquals(2, summaries.size(), "Only accepted TAs should appear in the workload list.");

            WorkloadSummary ta101Summary = workloadService.getAcceptedTaWorkload("ta101", 10)
                .orElseThrow(() -> new IllegalStateException("Missing workload summary for ta101."));
            assertEquals("ta101", ta101Summary.applicantUserId(), "Unexpected applicant userId.");
            assertEquals(18, ta101Summary.totalWeeklyHours(), "Unexpected total weekly hours for ta101.");
            assertTrue(ta101Summary.overloaded(), "ta101 should be flagged as overloaded.");
            assertTrue(ta101Summary.hasConflict(), "ta101 should have schedule conflicts.");
            assertEquals(5, ta101Summary.acceptedAssignments().size(), "Only ACCEPTED assignments should be counted.");
            assertTrue(
                ta101Summary.acceptedAssignments().stream().noneMatch(assignment -> assignment.jobId().equals("job106")),
                "SHORTLISTED jobs must not be included in workload results."
            );
            assertConflictExists(ta101Summary.conflicts(), "job101", "job102", "MON-10:00-11:00");
            assertConflictExists(ta101Summary.conflicts(), "job101", "job104", "MON-09:30-10:30");
            assertConflictExists(ta101Summary.conflicts(), "job101", "job105", "MON-09:00-11:00");
            assertTrue(
                ta101Summary.conflicts().stream().noneMatch(conflict -> conflict.overlapSlot().equals("MON-12:00-12:00")),
                "Edge-touching slots must not be treated as conflicts."
            );

            WorkloadSummary ta102Summary = workloadService.getAcceptedTaWorkload("ta102", 10)
                .orElseThrow(() -> new IllegalStateException("Missing workload summary for ta102."));
            assertEquals(4, ta102Summary.totalWeeklyHours(), "Unexpected total weekly hours for ta102.");
            assertTrue(!ta102Summary.overloaded(), "ta102 should not be overloaded.");
            assertTrue(!ta102Summary.hasConflict(), "ta102 should not have schedule conflicts.");

            decisionService.updateStatus("mo101", "application102", ApplicationStatus.REJECTED, "Reduce load");
            decisionService.updateStatus("mo101", "application104", ApplicationStatus.REJECTED, "Reduce load");
            decisionService.updateStatus("mo101", "application105", ApplicationStatus.REJECTED, "Reduce load");

            WorkloadSummary refreshedTa101Summary = workloadService.getAcceptedTaWorkload("ta101", 10)
                .orElseThrow(() -> new IllegalStateException("Missing refreshed workload summary for ta101."));
            assertEquals(7, refreshedTa101Summary.totalWeeklyHours(), "Accepted workload should refresh after status change.");
            assertTrue(!refreshedTa101Summary.overloaded(), "ta101 should no longer be overloaded after rejections.");
            assertTrue(!refreshedTa101Summary.hasConflict(), "ta101 should no longer have conflicts after rejections.");

            System.out.println("US14 smoke test passed.");
            System.out.println("Temp data directory: " + tempDataDirectory);
        } catch (Exception exception) {
            throw new IllegalStateException("US14 smoke test failed.", exception);
        }
    }

    private static void bootstrapDataFiles(Path tempDataDirectory) throws Exception {
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
        jobRepository.save(buildJob("job101", "mo101", 4, List.of("MON-09:00-11:00")));
        jobRepository.save(buildJob("job102", "mo101", 5, List.of("MON-10:00-12:00")));
        jobRepository.save(buildJob("job103", "mo101", 3, List.of("MON-12:00-14:00")));
        jobRepository.save(buildJob("job104", "mo101", 2, List.of("MON-09:30-10:30")));
        jobRepository.save(buildJob("job105", "mo101", 4, List.of("MON-09:00-11:00")));
        jobRepository.save(buildJob("job106", "mo101", 2, List.of("TUE-09:00-11:00")));
        jobRepository.save(buildJob("job201", "mo101", 4, List.of("WED-10:00-12:00")));
        jobRepository.save(buildJob("job301", "mo201", 4, List.of("THU-10:00-12:00")));
        jobRepository.save(buildJob("job401", "mo101", 1, List.of("FRI-10:00-11:00")));
    }

    private static void seedApplications(ApplicationRepository applicationRepository) {
        applicationRepository.save(buildApplication("application101", "job101", "ta101", ApplicationStatus.SUBMITTED));
        applicationRepository.save(buildApplication("application102", "job102", "ta101", ApplicationStatus.SUBMITTED));
        applicationRepository.save(buildApplication("application103", "job103", "ta101", ApplicationStatus.SUBMITTED));
        applicationRepository.save(buildApplication("application104", "job104", "ta101", ApplicationStatus.SUBMITTED));
        applicationRepository.save(buildApplication("application105", "job105", "ta101", ApplicationStatus.SUBMITTED));
        applicationRepository.save(buildApplication("application106", "job106", "ta101", ApplicationStatus.SUBMITTED));
        applicationRepository.save(buildApplication("application201", "job201", "ta102", ApplicationStatus.SUBMITTED));
        applicationRepository.save(buildApplication("application301", "job301", "ta101", ApplicationStatus.SUBMITTED));
        applicationRepository.save(buildApplication("application401", "job401", "ta102", ApplicationStatus.WITHDRAWN));
    }

    private static JobPosting buildJob(String jobId, String organiserId, int weeklyHours, List<String> scheduleSlots) {
        return new JobPosting(
            jobId,
            organiserId,
            "TA role " + jobId,
            "Module " + jobId,
            "Support teaching activities for " + jobId,
            List.of("Communication"),
            weeklyHours,
            scheduleSlots,
            JobStatus.OPEN
        );
    }

    private static JobApplication buildApplication(
        String applicationId,
        String jobId,
        String applicantUserId,
        ApplicationStatus status
    ) {
        return new JobApplication(
            applicationId,
            jobId,
            applicantUserId,
            "cv-" + applicationId,
            status,
            LocalDateTime.parse("2026-04-02T10:00:00"),
            ""
        );
    }

    private static void assertConflictExists(
        List<WorkloadConflict> conflicts,
        String jobIdA,
        String jobIdB,
        String overlapSlot
    ) {
        boolean exists = conflicts.stream().anyMatch(conflict ->
            conflict.jobIdA().equals(jobIdA)
                && conflict.jobIdB().equals(jobIdB)
                && conflict.overlapSlot().equals(overlapSlot)
        );
        assertTrue(exists, "Expected workload conflict was not found: " + jobIdA + " / " + jobIdB + " / " + overlapSlot);
    }

    private static void expectDecisionFailure(ThrowingRunnable runnable, String expectedMessagePart) {
        try {
            runnable.run();
            throw new IllegalStateException("Expected decision failure but operation succeeded.");
        } catch (IllegalArgumentException exception) {
            if (!exception.getMessage().contains(expectedMessagePart)) {
                throw new IllegalStateException("Unexpected decision failure message: " + exception.getMessage(), exception);
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

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}
