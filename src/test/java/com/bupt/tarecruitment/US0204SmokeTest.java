package com.bupt.tarecruitment;

import com.bupt.tarecruitment.application.ApplicationIdGenerator;
import com.bupt.tarecruitment.application.ApplicationRepository;
import com.bupt.tarecruitment.application.ApplicationStatus;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.application.JobApplicationService;
import com.bupt.tarecruitment.application.TextFileApplicationRepository;
import com.bupt.tarecruitment.applicant.ApplicantCv;
import com.bupt.tarecruitment.applicant.ApplicantCvIdGenerator;
import com.bupt.tarecruitment.applicant.ApplicantCvLibraryService;
import com.bupt.tarecruitment.applicant.ApplicantCvRepository;
import com.bupt.tarecruitment.applicant.ApplicantCvService;
import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.applicant.ApplicantProfileRepository;
import com.bupt.tarecruitment.applicant.ApplicantProfileService;
import com.bupt.tarecruitment.applicant.ApplicantProfileValidator;
import com.bupt.tarecruitment.applicant.TextFileApplicantCvRepository;
import com.bupt.tarecruitment.applicant.TextFileApplicantProfileRepository;
import com.bupt.tarecruitment.applicant.TextFileCvStorage;
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
 * 覆盖 US02、US04 场景的冒烟测试。
 */
public final class US0204SmokeTest {
    private US0204SmokeTest() {
    }

    public static void main(String[] args) {
        try {
            Path tempDataDirectory = Files.createTempDirectory("us02-us04-smoke");
            bootstrapDataFiles(tempDataDirectory);

            ApplicantProfileRepository profileRepository = new TextFileApplicantProfileRepository(tempDataDirectory);
            ApplicantCvRepository cvRepository = new TextFileApplicantCvRepository(tempDataDirectory);
            ApplicationRepository applicationRepository = new TextFileApplicationRepository(tempDataDirectory);
            JobRepository jobRepository = new TextFileJobRepository(tempDataDirectory);
            UserRepository userRepository = new TextFileUserRepository(tempDataDirectory);
            AuthService authService = new AuthService(userRepository, new AuthValidator());
            authService.register("ta101", "pass-ta101", UserRole.APPLICANT);
            authService.register("ta102", "pass-ta102", UserRole.APPLICANT);
            authService.register("mo101", "pass-mo101", UserRole.MO);

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
            ApplicantCvService cvService = new ApplicantCvService(
                applicationRepository,
                cvRepository,
                new TextFileCvStorage(tempDataDirectory)
            );
            JobApplicationService jobApplicationService = new JobApplicationService(
                jobRepository,
                applicationRepository,
                new ApplicationIdGenerator(applicationRepository),
                profileRepository,
                cvRepository,
                userRepository
            );

            profileService.createProfile(buildProfile("profile101", "ta101", "231225101", "Applicant One"));
            profileService.createProfile(buildProfile("profile102", "ta102", "231225102", "Applicant Two"));

            jobRepository.save(buildJob("job-open", JobStatus.OPEN));
            jobRepository.save(buildJob("job-closed", JobStatus.CLOSED));
            jobRepository.save(buildJob("job-open-2", JobStatus.OPEN));

            ApplicantCv cv101 = cvLibraryService.createCv("ta101", "General CV", "CV content for ta101");
            ApplicantCv cv102 = cvLibraryService.createCv("ta102", "Other CV", "CV content for ta102");

            JobApplication us04Application = jobApplicationService.applyToJobWithCv("ta101", "job-open", cv101.cvId());
            if (!cv101.cvId().equals(us04Application.cvId())) {
                throw new IllegalStateException("US04 success case failed: unexpected cvId.");
            }

            expectFailure(
                () -> jobApplicationService.applyToJobWithCv("ta101", "job-open", cv101.cvId()),
                "Duplicate application"
            );
            expectFailure(
                () -> jobApplicationService.applyToJobWithCv("ta101", "job-closed", cv101.cvId()),
                "Only OPEN jobs can be applied"
            );
            expectFailure(
                () -> jobApplicationService.applyToJobWithCv("ta101", "job-open-2", cv102.cvId()),
                "does not belong to applicantUserId"
            );
            expectFailure(
                () -> jobApplicationService.applyToJobWithCv("ta404", "job-open-2", cv101.cvId()),
                "No registered user exists"
            );
            expectFailure(
                () -> jobApplicationService.applyToJobWithCv("mo101", "job-open-2", cv101.cvId()),
                "ACTIVE APPLICANT account"
            );
            expectFailure(
                () -> jobApplicationService.applyToJobWithCv("ta101", "job404", cv101.cvId()),
                "No job exists"
            );

            applicationRepository.save(
                new JobApplication(
                    "application900",
                    "job-open-2",
                    "ta101",
                    "",
                    ApplicationStatus.SUBMITTED,
                    LocalDateTime.parse("2026-03-26T10:00:00"),
                    ""
                )
            );

            JobApplication us02Updated = cvService.attachCvToApplication("application900", cv101.cvId());
            if (!cv101.cvId().equals(us02Updated.cvId())) {
                throw new IllegalStateException("US02 attach case failed: cvId was not written back.");
            }

            String loadedContent = cvService.loadCvContentByApplicationId("application900");
            if (!"CV content for ta101".equals(loadedContent)) {
                throw new IllegalStateException("US02 load case failed: CV text mismatch.");
            }

            expectFailure(
                () -> cvService.attachCvToApplication("application900", cv102.cvId()),
                "does not belong to applicantUserId"
            );
            expectFailure(
                () -> cvService.attachCvToApplication("application404", cv101.cvId()),
                "No application exists"
            );
            expectFailure(
                () -> cvService.attachCvToApplication("application900", "cv404"),
                "No CV exists"
            );

            System.out.println("US02/US04 smoke test passed.");
            System.out.println("Temp data directory: " + tempDataDirectory);
        } catch (Exception exception) {
            throw new IllegalStateException("US02/US04 smoke test failed.", exception);
        }
    }

    private static void bootstrapDataFiles(Path tempDataDirectory) throws Exception {
        Files.write(tempDataDirectory.resolve(DataFile.USERS.fileName()), List.of(DataFile.USERS.initialLines().getFirst()), StandardCharsets.UTF_8);
        Files.write(tempDataDirectory.resolve(DataFile.PROFILES.fileName()), DataFile.PROFILES.initialLines(), StandardCharsets.UTF_8);
        Files.write(tempDataDirectory.resolve(DataFile.CVS.fileName()), DataFile.CVS.initialLines(), StandardCharsets.UTF_8);
        Files.write(tempDataDirectory.resolve(DataFile.APPLICATIONS.fileName()), DataFile.APPLICATIONS.initialLines(), StandardCharsets.UTF_8);
        Files.write(tempDataDirectory.resolve(DataFile.JOBS.fileName()), DataFile.JOBS.initialLines(), StandardCharsets.UTF_8);
    }

    private static ApplicantProfile buildProfile(String profileId, String userId, String studentId, String fullName) {
        return new ApplicantProfile(
            profileId,
            userId,
            studentId,
            fullName,
            "Software Engineering",
            3,
            "Not Graduated",
            List.of("Java", "Communication"),
            List.of("MON-10:00-12:00"),
            List.of("Teaching Assistant")
        );
    }

    private static JobPosting buildJob(String jobId, JobStatus status) {
        return new JobPosting(
            jobId,
            "mo001",
            "TA position " + jobId,
            "SE Module",
            "Support teaching tasks",
            List.of("Java"),
            4,
            List.of("MON-10:00-12:00"),
            status
        );
    }

    private static void expectFailure(ThrowingRunnable runnable, String expectedMessagePart) {
        try {
            runnable.run();
            throw new IllegalStateException("Expected failure but operation succeeded.");
        } catch (IllegalArgumentException exception) {
            if (!exception.getMessage().contains(expectedMessagePart)) {
                throw new IllegalStateException("Unexpected message: " + exception.getMessage(), exception);
            }
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}
