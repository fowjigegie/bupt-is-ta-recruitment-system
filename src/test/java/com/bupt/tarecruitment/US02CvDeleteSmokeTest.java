package com.bupt.tarecruitment;

import com.bupt.tarecruitment.applicant.ApplicantCv;
import com.bupt.tarecruitment.applicant.ApplicantCvIdGenerator;
import com.bupt.tarecruitment.applicant.ApplicantCvLibraryService;
import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.applicant.TextFileApplicantCvRepository;
import com.bupt.tarecruitment.applicant.TextFileApplicantProfileRepository;
import com.bupt.tarecruitment.applicant.TextFileCvStorage;
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Covers applicant CV deletion from the CV library.
 */
public final class US02CvDeleteSmokeTest {
    private US02CvDeleteSmokeTest() {
    }

    public static void main(String[] args) {
        try {
            Path tempDataDirectory = Files.createTempDirectory("us02-cv-delete-smoke");
            bootstrapDataFiles(tempDataDirectory);

            UserRepository userRepository = new TextFileUserRepository(tempDataDirectory);
            AuthService authService = new AuthService(userRepository, new AuthValidator());
            authService.register("ta701", "pass-ta701", UserRole.APPLICANT);

            TextFileApplicantProfileRepository profileRepository = new TextFileApplicantProfileRepository(tempDataDirectory);
            profileRepository.save(new ApplicantProfile(
                "profile701",
                "ta701",
                "231225701",
                "CV Delete Applicant",
                "Software Engineering",
                2,
                "Not Graduated",
                List.of("Java"),
                List.of("MON-09:00-11:00"),
                List.of("Teaching Assistant")
            ));

            TextFileApplicantCvRepository cvRepository = new TextFileApplicantCvRepository(tempDataDirectory);
            TextFileCvStorage cvStorage = new TextFileCvStorage(tempDataDirectory);
            ApplicationRepository applicationRepository = new TextFileApplicationRepository(tempDataDirectory);
            ApplicantCvLibraryService cvLibraryService = new ApplicantCvLibraryService(
                profileRepository,
                cvRepository,
                new ApplicantCvIdGenerator(cvRepository),
                cvStorage,
                userRepository,
                applicationRepository
            );

            ApplicantCv removableCv = cvLibraryService.createCv("ta701", "Removable CV", "CV body to delete");
            Path removableCvPath = tempDataDirectory.resolve(removableCv.fileName());
            assertTrue(Files.exists(removableCvPath), "Created CV text file should exist before delete.");

            cvLibraryService.deleteCv("ta701", removableCv.cvId());
            assertTrue(cvRepository.findByCvId(removableCv.cvId()).isEmpty(), "Deleted CV metadata should be removed.");
            assertTrue(Files.notExists(removableCvPath), "Deleted CV text file should be removed.");
            expectFailure(
                () -> cvLibraryService.loadCvContentByCvId(removableCv.cvId()),
                "No CV exists"
            );

            ApplicantCv linkedCv = cvLibraryService.createCv("ta701", "Linked CV", "CV body linked to an application");
            applicationRepository.save(new JobApplication(
                "application701",
                "job701",
                "ta701",
                linkedCv.cvId(),
                ApplicationStatus.SUBMITTED,
                LocalDateTime.parse("2026-04-24T10:00:00"),
                ""
            ));

            expectFailure(
                () -> cvLibraryService.deleteCv("ta701", linkedCv.cvId()),
                "linked to an existing application"
            );
            assertTrue(cvRepository.findByCvId(linkedCv.cvId()).isPresent(), "Linked CV metadata should be kept.");

            System.out.println("US02 CV delete smoke test passed.");
            System.out.println("Temp data directory: " + tempDataDirectory);
        } catch (Exception exception) {
            throw new IllegalStateException("US02 CV delete smoke test failed.", exception);
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
