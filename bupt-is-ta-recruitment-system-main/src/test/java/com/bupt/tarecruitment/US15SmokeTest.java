package com.bupt.tarecruitment;

import com.bupt.tarecruitment.applicant.ApplicantCv;
import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.applicant.ApplicantProfileValidator;
import com.bupt.tarecruitment.applicant.TextFileApplicantCvRepository;
import com.bupt.tarecruitment.applicant.TextFileApplicantProfileRepository;
import com.bupt.tarecruitment.application.ApplicationDecisionService;
import com.bupt.tarecruitment.application.ApplicationIdGenerator;
import com.bupt.tarecruitment.application.ApplicationRepository;
import com.bupt.tarecruitment.application.ApplicationStatus;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.application.JobApplicationService;
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

/**
 * 覆盖 US15 场景的冒烟测试。
 */
public final class US15SmokeTest {
    private US15SmokeTest() {
    }

    public static void main(String[] args) {
        try {
            Path tempDataDirectory = Files.createTempDirectory("us15-smoke");
            bootstrapDataFiles(tempDataDirectory);

            UserRepository userRepository = new TextFileUserRepository(tempDataDirectory);
            AuthService authService = new AuthService(userRepository, new AuthValidator());
            authService.register("mo501", "pass-mo501", UserRole.MO);
            authService.register("ta501", "pass-ta501", UserRole.APPLICANT);

            TextFileApplicantProfileRepository profileRepository = new TextFileApplicantProfileRepository(tempDataDirectory);
            profileRepository.save(new ApplicantProfile(
                "profile501",
                "ta501",
                "231225501",
                "Conflict Demo Applicant",
                "Software Engineering",
                2,
                "Not Graduated",
                List.of("Java", "Communication"),
                List.of("MON-09:00-12:00", "TUE-14:00-16:00"),
                List.of("Teaching Assistant")
            ));

            TextFileApplicantCvRepository cvRepository = new TextFileApplicantCvRepository(tempDataDirectory);
            cvRepository.save(new ApplicantCv(
                "cv501",
                "ta501",
                "Conflict Demo CV",
                "cv501.txt",
                LocalDateTime.parse("2026-04-04T09:00:00"),
                LocalDateTime.parse("2026-04-04T09:00:00")
            ));

            JobRepository jobRepository = new TextFileJobRepository(tempDataDirectory);
            jobRepository.save(buildJob("job501", List.of("MON-09:00-11:00")));
            jobRepository.save(buildJob("job502", List.of("MON-10:00-12:00")));
            jobRepository.save(buildJob("job503", List.of("TUE-14:00-16:00")));
            jobRepository.save(buildJob("job504", List.of("MON-10:30-11:30")));

            ApplicationRepository applicationRepository = new TextFileApplicationRepository(tempDataDirectory);
            applicationRepository.save(new JobApplication(
                "application501",
                "job501",
                "ta501",
                "cv501",
                ApplicationStatus.ACCEPTED,
                LocalDateTime.parse("2026-04-04T09:10:00"),
                ""
            ));
            applicationRepository.save(new JobApplication(
                "application502",
                "job502",
                "ta501",
                "cv501",
                ApplicationStatus.SUBMITTED,
                LocalDateTime.parse("2026-04-04T09:20:00"),
                ""
            ));
            applicationRepository.save(new JobApplication(
                "application503",
                "job503",
                "ta501",
                "cv501",
                ApplicationStatus.WITHDRAWN,
                LocalDateTime.parse("2026-04-04T09:30:00"),
                ""
            ));

            JobApplicationService jobApplicationService = new JobApplicationService(
                jobRepository,
                applicationRepository,
                new ApplicationIdGenerator(applicationRepository),
                profileRepository,
                cvRepository,
                userRepository
            );
            ApplicationDecisionService decisionService = new ApplicationDecisionService(
                applicationRepository,
                jobRepository,
                userRepository
            );

            expectFailure(
                () -> jobApplicationService.applyToJobWithCv("ta501", "job504", "cv501"),
                "Schedule conflict with accepted TA assignment"
            );

            JobApplication safeApplication = jobApplicationService.applyToJobWithCv("ta501", "job503", "cv501");
            assertEquals("job503", safeApplication.jobId(), "Non-conflicting application should still be created.");

            expectFailure(
                () -> decisionService.updateStatus("mo501", "application502", ApplicationStatus.ACCEPTED, "Would overlap"),
                "Schedule conflict with accepted TA assignment"
            );

            JobApplication acceptedSafeApplication = decisionService.updateStatus(
                "mo501",
                safeApplication.applicationId(),
                ApplicationStatus.ACCEPTED,
                "No overlap"
            );
            assertEquals(ApplicationStatus.ACCEPTED, acceptedSafeApplication.status(), "Safe application should be accepted.");

            System.out.println("US15 smoke test passed.");
            System.out.println("Temp data directory: " + tempDataDirectory);
        } catch (Exception exception) {
            throw new IllegalStateException("US15 smoke test failed.", exception);
        }
    }

    private static void bootstrapDataFiles(Path tempDataDirectory) throws Exception {
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
            tempDataDirectory.resolve(DataFile.CVS.fileName()),
            List.of(DataFile.CVS.initialLines().getFirst()),
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

    private static JobPosting buildJob(String jobId, List<String> scheduleSlots) {
        return new JobPosting(
            jobId,
            "mo501",
            "TA role " + jobId,
            "Module " + jobId,
            "Support teaching activities for " + jobId,
            List.of("Communication"),
            3,
            scheduleSlots,
            JobStatus.OPEN
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
