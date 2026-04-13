package com.bupt.tarecruitment;

import com.bupt.tarecruitment.applicant.ApplicantCv;
import com.bupt.tarecruitment.applicant.ApplicantCvIdGenerator;
import com.bupt.tarecruitment.applicant.ApplicantCvLibraryService;
import com.bupt.tarecruitment.applicant.ApplicantCvRepository;
import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.applicant.ApplicantProfileRepository;
import com.bupt.tarecruitment.applicant.ApplicantProfileService;
import com.bupt.tarecruitment.applicant.ApplicantProfileValidator;
import com.bupt.tarecruitment.applicant.TextFileApplicantCvRepository;
import com.bupt.tarecruitment.applicant.TextFileApplicantProfileRepository;
import com.bupt.tarecruitment.applicant.TextFileCvStorage;
import com.bupt.tarecruitment.application.ApplicationDecisionService;
import com.bupt.tarecruitment.application.ApplicationIdGenerator;
import com.bupt.tarecruitment.application.ApplicationRepository;
import com.bupt.tarecruitment.application.ApplicationStatus;
import com.bupt.tarecruitment.application.ApplicationStatusPresenter;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.application.JobApplicationService;
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
import java.util.List;

/**
 * 覆盖 US11、US12、US13 场景的集成流程测试。
 */
public final class US111213DetailedIntegrationTest {
    private US111213DetailedIntegrationTest() {
    }

    public static void main(String[] args) {
        try {
            Path tempDataDirectory = Files.createTempDirectory("us111213-detailed");
            bootstrapDataFiles(tempDataDirectory);

            UserRepository userRepository = new TextFileUserRepository(tempDataDirectory);
            ApplicantProfileRepository profileRepository = new TextFileApplicantProfileRepository(tempDataDirectory);
            ApplicantCvRepository cvRepository = new TextFileApplicantCvRepository(tempDataDirectory);
            ApplicationRepository applicationRepository = new TextFileApplicationRepository(tempDataDirectory);
            JobRepository jobRepository = new TextFileJobRepository(tempDataDirectory);

            AuthService authService = new AuthService(userRepository, new AuthValidator());
            authService.register("mo101", "pass-mo101", UserRole.MO);
            authService.register("ta101", "pass-ta101", UserRole.APPLICANT);

            ApplicantProfileService profileService = new ApplicantProfileService(
                profileRepository,
                new ApplicantProfileValidator(),
                userRepository
            );
            ApplicantCvLibraryService cvLibraryService = new ApplicantCvLibraryService(
                profileRepository,
                cvRepository,
                new ApplicantCvIdGenerator(cvRepository),
                new TextFileCvStorage(tempDataDirectory),
                userRepository
            );
            JobPostingService jobPostingService = new JobPostingService(
                jobRepository,
                new JobIdGenerator(jobRepository),
                userRepository
            );
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

            // US11: MO posts a new OPEN job.
            JobPosting createdJob = jobPostingService.publish(
                "mo101",
                "TA for Software Engineering",
                "EBU6304",
                "Support tutorials and grading.",
                List.of("Java", "Communication"),
                3,
                List.of("MON-09:00-11:00")
            );
            assertEquals(JobStatus.OPEN, createdJob.status(), "US11: Newly posted job should be OPEN.");

            profileService.createProfile(buildProfile("profile101", "ta101"));
            ApplicantCv cv = cvLibraryService.createCv(
                "ta101",
                "TA101 CV",
                "Name: Demo Applicant\nSkills: Java, Communication\nDesired Positions: Teaching Assistant"
            );

            JobApplication application = jobApplicationService.applyToJobWithCv("ta101", createdJob.jobId(), cv.cvId());
            assertEquals(ApplicationStatus.SUBMITTED, application.status(), "Application should start as SUBMITTED.");

            // US12: MO edits the job.
            JobPosting editedJob = new JobPosting(
                createdJob.jobId(),
                createdJob.organiserId(),
                "TA for Software Engineering (Updated)",
                createdJob.moduleOrActivity(),
                "Support tutorials, labs and grading.",
                List.of("Java", "Communication", "Teamwork"),
                4,
                List.of("MON-09:00-11:00", "WED-14:00-16:00"),
                JobStatus.OPEN
            );
            jobPostingService.publish(editedJob);
            JobPosting storedEdited = jobRepository.findByJobId(createdJob.jobId())
                .orElseThrow(() -> new IllegalStateException("Edited job was not found."));
            assertEquals("TA for Software Engineering (Updated)", storedEdited.title(), "US12: Edited title should persist.");
            assertEquals(4, storedEdited.weeklyHours(), "US12: Edited workload should persist.");
            assertEquals(2, storedEdited.scheduleSlots().size(), "US12: Edited schedule should persist.");

            // US13: MO reviews this application and updates status.
            JobApplication shortlisted = decisionService.updateStatus(
                "mo101",
                application.applicationId(),
                ApplicationStatus.SHORTLISTED,
                "Strong baseline."
            );
            assertEquals(ApplicationStatus.SHORTLISTED, shortlisted.status(), "US13: Should allow shortlist.");

            JobApplication accepted = decisionService.updateStatus(
                "mo101",
                application.applicationId(),
                ApplicationStatus.ACCEPTED,
                "Final offer approved."
            );
            assertEquals(ApplicationStatus.ACCEPTED, accepted.status(), "US13: Should allow accept.");
            assertEquals("Accepted", ApplicationStatusPresenter.toDisplayText(accepted.status()), "Display text should match.");

            JobApplication taVisible = applicationRepository.findByApplicationId(application.applicationId())
                .orElseThrow(() -> new IllegalStateException("TA-visible application not found."));
            assertEquals(ApplicationStatus.ACCEPTED, taVisible.status(), "US13: Applicant should observe latest status.");

            // US12: Close job and ensure no new applications can be created.
            JobPosting closedJob = new JobPosting(
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
            jobPostingService.publish(closedJob);
            JobPosting storedClosed = jobRepository.findByJobId(createdJob.jobId())
                .orElseThrow(() -> new IllegalStateException("Closed job was not found."));
            assertEquals(JobStatus.CLOSED, storedClosed.status(), "US12: Job should be CLOSED after close action.");

            expectFailure(
                () -> jobApplicationService.applyToJobWithCv("ta101", storedClosed.jobId(), cv.cvId()),
                "Only OPEN jobs can be applied to."
            );

            // Existing applications must remain visible after close.
            long existingCount = applicationRepository.findAll().stream()
                .filter(item -> item.jobId().equals(storedClosed.jobId()))
                .count();
            assertEquals(1L, existingCount, "US12: Existing applications should remain visible after closing.");

            System.out.println("US11/US12/US13 detailed integration test passed.");
            System.out.println("Temp data directory: " + tempDataDirectory);
        } catch (Exception exception) {
            throw new IllegalStateException("US11/US12/US13 detailed integration test failed.", exception);
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
            tempDataDirectory.resolve(DataFile.APPLICATIONS.fileName()),
            List.of(DataFile.APPLICATIONS.initialLines().getFirst()),
            StandardCharsets.UTF_8
        );
        Files.write(
            tempDataDirectory.resolve(DataFile.JOBS.fileName()),
            List.of(DataFile.JOBS.initialLines().getFirst()),
            StandardCharsets.UTF_8
        );
    }

    private static ApplicantProfile buildProfile(String profileId, String userId) {
        return new ApplicantProfile(
            profileId,
            userId,
            "231225101",
            "Demo Applicant",
            "Software Engineering",
            3,
            "Not Graduated",
            List.of("Java", "Communication"),
            List.of("MON-09:00-11:00"),
            List.of("Teaching Assistant")
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
