package com.bupt.tarecruitment;

import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.applicant.ApplicantCvService;
import com.bupt.tarecruitment.applicant.ApplicantCvReview;
import com.bupt.tarecruitment.applicant.ApplicantCvReviewService;
import com.bupt.tarecruitment.applicant.ApplicantProfileConsoleWorkflow;
import com.bupt.tarecruitment.applicant.ApplicantProfileIdGenerator;
import com.bupt.tarecruitment.applicant.ApplicantProfileService;
import com.bupt.tarecruitment.applicant.ApplicantProfileValidator;
import com.bupt.tarecruitment.applicant.TextFileApplicantProfileRepository;
import com.bupt.tarecruitment.applicant.TextFileCvStorage;
import com.bupt.tarecruitment.bootstrap.ProjectBootstrap;
import com.bupt.tarecruitment.bootstrap.StartupReport;
import com.bupt.tarecruitment.common.storage.DataFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

public final class AppSmokeTest {
    private AppSmokeTest() {
    }

    public static void main(String[] args) {
        StartupReport report = new ProjectBootstrap().initialize();

        for (DataFile dataFile : DataFile.values()) {
            if (Files.notExists(report.dataDirectory().resolve(dataFile.fileName()))) {
                throw new IllegalStateException("Missing required data file: " + dataFile.fileName());
            }
        }

        if (Files.notExists(report.dataDirectory().resolve("cvs").resolve("ta001").resolve("current.txt"))) {
            throw new IllegalStateException("Missing sample CV text file.");
        }

        try {
            Path tempDataDirectory = Files.createTempDirectory("applicant-module-test");
            Files.write(
                tempDataDirectory.resolve(DataFile.PROFILES.fileName()),
                DataFile.PROFILES.initialLines(),
                StandardCharsets.UTF_8
            );

            ApplicantProfileService profileService = new ApplicantProfileService(
                new TextFileApplicantProfileRepository(tempDataDirectory),
                new ApplicantProfileValidator()
            );
            TextFileApplicantProfileRepository repository = new TextFileApplicantProfileRepository(tempDataDirectory);
            ApplicantProfileIdGenerator idGenerator = new ApplicantProfileIdGenerator(repository);

            ApplicantProfile profile = new ApplicantProfile(
                "profile999",
                "ta999",
                "231225999",
                "Temp Applicant",
                "Software Engineering",
                3,
                "Not Graduated",
                List.of("Java"),
                List.of("MON-09:00-11:00"),
                List.of("Teaching Assistant"),
                ""
            );
            profileService.createProfile(profile);

            profileService.createProfile(
                new ApplicantProfile(
                    "profile995",
                    "ta995",
                    "231225995",
                    "Blank Cv Applicant",
                    "Software Engineering",
                    2,
                    "Not Graduated",
                    List.of("Java"),
                    List.of("TUE-10:00-12:00"),
                    List.of("Teaching Assistant"),
                    ""
                )
            );

            if (profileService.getProfileByUserId("ta999").isEmpty()) {
                throw new IllegalStateException("Applicant profile service failed to save or read profile.");
            }

            ApplicantCvService cvService = new ApplicantCvService(
                repository,
                profileService,
                new TextFileCvStorage(tempDataDirectory)
            );

            expectCreateFailure(
                profileService,
                new ApplicantProfile(
                    "profile998",
                    "ta998",
                    "231225999",
                    "Another Applicant",
                    "Software Engineering",
                    2,
                    "Not Graduated",
                    List.of("Java"),
                    List.of("TUE-10:00-12:00"),
                    List.of("Teaching Assistant"),
                    ""
                ),
                "studentId is already used"
            );

            expectCreateFailure(
                profileService,
                new ApplicantProfile(
                    "profile997",
                    "ta997",
                    "231225997",
                    "1",
                    "Software Engineering",
                    2,
                    "Not Graduated",
                    List.of("Java"),
                    List.of("TUE-10:00-12:00"),
                    List.of("Teaching Assistant"),
                    ""
                ),
                "fullName must contain letters and spaces only."
            );

            expectCreateFailure(
                profileService,
                new ApplicantProfile(
                    "profile996",
                    "ta996",
                    "231225996",
                    "Availability Applicant",
                    "Software Engineering",
                    2,
                    "Not Graduated",
                    List.of("Java"),
                    List.of("Tuesday"),
                    List.of("Teaching Assistant"),
                    ""
                ),
                "availabilitySlots must use the format"
            );

            ApplicantProfile updatedProfile = cvService.submitCv("ta999", "Temp CV content");
            String cvPath = updatedProfile.cvFileName();
            if (!"cvs/ta999/current.txt".equals(cvPath)) {
                throw new IllegalStateException("Unexpected CV relative path: " + cvPath);
            }
            if (Files.notExists(tempDataDirectory.resolve("cvs").resolve("ta999").resolve("current.txt"))) {
                throw new IllegalStateException("CV text file was not created.");
            }
            if (!"Temp CV content".equals(cvService.loadCvContentByUserId("ta999"))) {
                throw new IllegalStateException("CV content was not read back correctly.");
            }
            if (repository.findByUserId("ta999").orElseThrow().cvFileName().isBlank()) {
                throw new IllegalStateException("CV reference was not written back to the applicant profile.");
            }

            ApplicantCvReviewService reviewService = new ApplicantCvReviewService(
                repository,
                new TextFileCvStorage(tempDataDirectory)
            );
            ApplicantCvReview review = reviewService.loadReviewByUserId("ta999");
            if (!"ta999".equals(review.profile().userId())) {
                throw new IllegalStateException("Review service returned the wrong applicant profile.");
            }
            if (!"Temp CV content".equals(review.cvContent())) {
                throw new IllegalStateException("Review service returned the wrong CV content.");
            }

            expectCvFailure(cvService, "ta999", "   ", "cvContent must not be blank.");
            expectLoadCvFailure(cvService, "ta995", "No CV has been submitted");
            expectLoadCvFailure(cvService, "ta404", "No applicant profile exists");
            expectReviewFailure(reviewService, "ta995", "No CV has been submitted");
            expectReviewFailure(reviewService, "ta404", "No applicant profile exists");

            String consoleInput = String.join(
                System.lineSeparator(),
                "1",
                "ta998",
                "231225998",
                "Workflow Applicant",
                "Software Engineering",
                "2",
                "Not Graduated",
                "Java,Communication",
                "TUE-10:00-12:00",
                "Teaching Assistant",
                "",
                "2",
                "ta998",
                "3"
            ) + System.lineSeparator();
            ByteArrayOutputStream workflowOutput = new ByteArrayOutputStream();
            ApplicantProfileConsoleWorkflow workflow = new ApplicantProfileConsoleWorkflow(
                new ApplicantProfileService(repository, new ApplicantProfileValidator()),
                idGenerator,
                new ByteArrayInputStream(consoleInput.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(workflowOutput, true, StandardCharsets.UTF_8)
            );
            workflow.run();

            if (repository.findByUserId("ta998").isEmpty()) {
                throw new IllegalStateException("Applicant profile workflow failed to save a profile.");
            }
            String outputText = workflowOutput.toString(StandardCharsets.UTF_8);
            if (!outputText.contains("Profile created successfully.")) {
                throw new IllegalStateException("Applicant profile workflow did not report successful creation.");
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Applicant module smoke checks failed.", exception);
        }

        System.out.println("Smoke test passed.");
    }

    private static void expectCreateFailure(
        ApplicantProfileService profileService,
        ApplicantProfile profile,
        String expectedMessagePart
    ) {
        try {
            profileService.createProfile(profile);
            throw new IllegalStateException("Expected profile creation to fail: " + profile.userId());
        } catch (IllegalArgumentException exception) {
            if (!exception.getMessage().contains(expectedMessagePart)) {
                throw new IllegalStateException("Unexpected validation message: " + exception.getMessage(), exception);
            }
        }
    }

    private static void expectCvFailure(
        ApplicantCvService cvService,
        String userId,
        String cvContent,
        String expectedMessagePart
    ) {
        try {
            cvService.submitCv(userId, cvContent);
            throw new IllegalStateException("Expected CV submission to fail for userId: " + userId);
        } catch (IllegalArgumentException exception) {
            if (!exception.getMessage().contains(expectedMessagePart)) {
                throw new IllegalStateException("Unexpected CV validation message: " + exception.getMessage(), exception);
            }
        }
    }

    private static void expectLoadCvFailure(
        ApplicantCvService cvService,
        String userId,
        String expectedMessagePart
    ) {
        try {
            cvService.loadCvContentByUserId(userId);
            throw new IllegalStateException("Expected CV loading to fail for userId: " + userId);
        } catch (IllegalArgumentException exception) {
            if (!exception.getMessage().contains(expectedMessagePart)) {
                throw new IllegalStateException("Unexpected CV load message: " + exception.getMessage(), exception);
            }
        }
    }

    private static void expectReviewFailure(
        ApplicantCvReviewService reviewService,
        String userId,
        String expectedMessagePart
    ) {
        try {
            reviewService.loadReviewByUserId(userId);
            throw new IllegalStateException("Expected CV review loading to fail for userId: " + userId);
        } catch (IllegalArgumentException exception) {
            if (!exception.getMessage().contains(expectedMessagePart)) {
                throw new IllegalStateException("Unexpected review load message: " + exception.getMessage(), exception);
            }
        }
    }
}
