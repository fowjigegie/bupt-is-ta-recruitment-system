package com.bupt.tarecruitment;

import com.bupt.tarecruitment.application.ApplicationRepository;
import com.bupt.tarecruitment.application.ApplicationStatus;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.application.TextFileApplicationRepository;
import com.bupt.tarecruitment.applicant.ApplicantCv;
import com.bupt.tarecruitment.applicant.ApplicantCvIdGenerator;
import com.bupt.tarecruitment.applicant.ApplicantCvLibraryService;
import com.bupt.tarecruitment.applicant.ApplicantCvRepository;
import com.bupt.tarecruitment.applicant.ApplicantCvReview;
import com.bupt.tarecruitment.applicant.ApplicantCvReviewService;
import com.bupt.tarecruitment.applicant.ApplicantCvService;
import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.applicant.ApplicantProfileConsoleWorkflow;
import com.bupt.tarecruitment.applicant.ApplicantProfileIdGenerator;
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
import com.bupt.tarecruitment.bootstrap.ProjectBootstrap;
import com.bupt.tarecruitment.bootstrap.StartupReport;
import com.bupt.tarecruitment.common.storage.DataFile;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobPostingService;
import com.bupt.tarecruitment.job.TextFileJobRepository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 覆盖应用主入口的基础冒烟测试。
 */
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

        if (Files.notExists(report.dataDirectory().resolve("cvs").resolve("ta001").resolve("cv001.txt"))) {
            throw new IllegalStateException("Missing sample CV text file.");
        }

        try {
            Path tempDataDirectory = Files.createTempDirectory("applicant-module-test");
            Files.write(
                tempDataDirectory.resolve(DataFile.PROFILES.fileName()),
                DataFile.PROFILES.initialLines(),
                StandardCharsets.UTF_8
            );
            Files.write(
                tempDataDirectory.resolve(DataFile.USERS.fileName()),
                List.of(DataFile.USERS.initialLines().getFirst()),
                StandardCharsets.UTF_8
            );
            Files.write(
                tempDataDirectory.resolve(DataFile.CVS.fileName()),
                DataFile.CVS.initialLines(),
                StandardCharsets.UTF_8
            );
            Files.write(
                tempDataDirectory.resolve(DataFile.APPLICATIONS.fileName()),
                DataFile.APPLICATIONS.initialLines(),
                StandardCharsets.UTF_8
            );
            Files.write(
                tempDataDirectory.resolve(DataFile.JOBS.fileName()),
                DataFile.JOBS.initialLines(),
                StandardCharsets.UTF_8
            );

            UserRepository userRepository = new TextFileUserRepository(tempDataDirectory);
            AuthService authService = new AuthService(userRepository, new AuthValidator());
            registerApplicant(authService, "ta999");
            registerApplicant(authService, "ta998");
            registerApplicant(authService, "ta997");
            registerApplicant(authService, "ta996");
            registerApplicant(authService, "ta995");
            registerApplicant(authService, "ta994");
            authService.register("mo999", "pass-mo999", UserRole.MO);
            authService.register("mo998", "pass-mo998", UserRole.MO);

            TextFileApplicantProfileRepository profileRepository = new TextFileApplicantProfileRepository(tempDataDirectory);
            ApplicantProfileService profileService = new ApplicantProfileService(
                profileRepository,
                new ApplicantProfileValidator(),
                userRepository
            );
            ApplicantCvRepository cvRepository = new TextFileApplicantCvRepository(tempDataDirectory);
            ApplicationRepository applicationRepository = new TextFileApplicationRepository(tempDataDirectory);
            ApplicantProfileIdGenerator profileIdGenerator = new ApplicantProfileIdGenerator(profileRepository);
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
            JobPostingService jobPostingService = new JobPostingService(
                new TextFileJobRepository(tempDataDirectory),
                new com.bupt.tarecruitment.job.JobIdGenerator(new TextFileJobRepository(tempDataDirectory)),
                userRepository
            );

            profileService.createProfile(
                new ApplicantProfile(
                    "profile999",
                    "ta999",
                    "231225999",
                    "Temp Applicant",
                    "Software Engineering",
                    3,
                    "Not Graduated",
                    List.of("Java"),
                    List.of("MON-09:00-11:00"),
                    List.of("Teaching Assistant")
                )
            );

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
                    List.of("Teaching Assistant")
                )
            );

            profileService.createProfile(
                new ApplicantProfile(
                    "profile994",
                    "ta994",
                    "231225994",
                    "Cv Limit Applicant",
                    "Software Engineering",
                    2,
                    "Not Graduated",
                    List.of("Java"),
                    List.of("WED-10:00-12:00"),
                    List.of("Teaching Assistant")
                )
            );

            applicationRepository.save(
                new JobApplication(
                    "application999",
                    "job999",
                    "ta999",
                    "",
                    ApplicationStatus.SUBMITTED,
                    LocalDateTime.parse("2026-03-26T10:00:00"),
                    ""
                )
            );

            applicationRepository.save(
                new JobApplication(
                    "application995",
                    "job995",
                    "ta995",
                    "",
                    ApplicationStatus.SUBMITTED,
                    LocalDateTime.parse("2026-03-26T11:00:00"),
                    ""
                )
            );

            if (profileService.getProfileByUserId("ta999").isEmpty()) {
                throw new IllegalStateException("Applicant profile service failed to save or read profile.");
            }

            expectCreateFailure(
                profileService,
                new ApplicantProfile(
                    "profile404",
                    "ta404",
                    "231225404",
                    "Missing Applicant",
                    "Software Engineering",
                    2,
                    "Not Graduated",
                    List.of("Java"),
                    List.of("TUE-10:00-12:00"),
                    List.of("Teaching Assistant")
                ),
                "No registered user exists"
            );

            expectCreateFailure(
                profileService,
                new ApplicantProfile(
                    "profilemo1",
                    "mo998",
                    "231225498",
                    "Wrong Role",
                    "Software Engineering",
                    2,
                    "Not Graduated",
                    List.of("Java"),
                    List.of("TUE-10:00-12:00"),
                    List.of("Teaching Assistant")
                ),
                "ACTIVE APPLICANT account"
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
                    List.of("Teaching Assistant")
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
                    List.of("Teaching Assistant")
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
                    List.of("Teaching Assistant")
                ),
                "availabilitySlots must use the format"
            );

            ApplicantCv createdCv = cvLibraryService.createCv("ta999", "Primary TA CV", "Temp CV content");
            if (!createdCv.cvId().startsWith("cv")) {
                throw new IllegalStateException("Unexpected generated CV ID: " + createdCv.cvId());
            }
            if (!"Temp CV content".equals(cvLibraryService.loadCvContentByCvId(createdCv.cvId()))) {
                throw new IllegalStateException("CV library did not read back the new CV content.");
            }

            JobApplication updatedApplication = cvService.attachCvToApplication("application999", createdCv.cvId());
            if (!createdCv.cvId().equals(updatedApplication.cvId())) {
                throw new IllegalStateException("Unexpected assigned cvId: " + updatedApplication.cvId());
            }
            if (!"cvs/ta999/%s.txt".formatted(createdCv.cvId()).equals(createdCv.fileName())) {
                throw new IllegalStateException("Unexpected CV relative path: " + createdCv.fileName());
            }
            if (Files.notExists(tempDataDirectory.resolve("cvs").resolve("ta999").resolve(createdCv.cvId() + ".txt"))) {
                throw new IllegalStateException("CV text file was not created.");
            }
            if (!"Temp CV content".equals(cvService.loadCvContentByApplicationId("application999"))) {
                throw new IllegalStateException("CV content was not read back correctly.");
            }
            if (applicationRepository.findByApplicationId("application999").orElseThrow().cvId().isBlank()) {
                throw new IllegalStateException("CV id was not written back to the application record.");
            }

            ApplicantCvReviewService reviewService = new ApplicantCvReviewService(
                applicationRepository,
                cvRepository,
                profileRepository,
                new TextFileCvStorage(tempDataDirectory)
            );
            ApplicantCvReview review = reviewService.loadReviewByApplicationId("application999");
            if (!"application999".equals(review.application().applicationId())) {
                throw new IllegalStateException("Review service returned the wrong application.");
            }
            if (!createdCv.cvId().equals(review.cv().cvId())) {
                throw new IllegalStateException("Review service returned the wrong CV metadata.");
            }
            if (!"ta999".equals(review.profile().userId())) {
                throw new IllegalStateException("Review service returned the wrong applicant profile.");
            }
            if (!"Temp CV content".equals(review.cvContent())) {
                throw new IllegalStateException("Review service returned the wrong CV content.");
            }

            JobPosting publishedJob = jobPostingService.publish(
                "mo999",
                "TA for Testing",
                "Testing Module",
                "Support smoke testing activities",
                List.of("Testing", "Communication"),
                3,
                List.of("MON-10:00-12:00")
            );
            if (!"mo999".equals(publishedJob.organiserId())) {
                throw new IllegalStateException("Job posting service did not preserve organiserId.");
            }

            expectCvCreateFailure(cvLibraryService, "ta999", "Primary TA CV", "   ", "cvContent must not be blank.");
            expectCvCreateFailure(cvLibraryService, "ta404", "Missing Account CV", "CV", "No registered user exists");
            expectCvCreateFailure(cvLibraryService, "mo999", "Wrong Role CV", "CV", "ACTIVE APPLICANT account");
            expectAttachCvFailure(cvService, "application999", "cv404", "No CV exists");
            expectAttachCvFailure(cvService, "application995", createdCv.cvId(), "does not belong to applicantUserId");
            expectLoadCvFailure(cvService, "application995", "No CV has been submitted");
            expectLoadCvFailure(cvService, "application404", "No application exists");
            expectReviewFailure(reviewService, "application995", "No CV has been submitted");
            expectReviewFailure(reviewService, "application404", "No application exists");
            expectJobPublishFailure(
                jobPostingService,
                "ta999",
                "Wrong Role Posting",
                "Testing Module",
                "Support smoke testing activities",
                List.of("Testing"),
                3,
                List.of("MON-10:00-12:00"),
                "ACTIVE MO account"
            );
            expectJobPublishFailure(
                jobPostingService,
                "mo404",
                "Missing User Posting",
                "Testing Module",
                "Support smoke testing activities",
                List.of("Testing"),
                3,
                List.of("MON-10:00-12:00"),
                "No registered user exists"
            );

            for (int index = 1; index <= ApplicantCvLibraryService.MAX_CVS_PER_APPLICANT; index++) {
                cvLibraryService.createCv("ta994", "Variant CV " + index, "Variant CV content " + index);
            }
            expectCvCreateFailure(
                cvLibraryService,
                "ta994",
                "Variant CV 11",
                "Variant CV content 11",
                "at most %d CVs".formatted(ApplicantCvLibraryService.MAX_CVS_PER_APPLICANT)
            );

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
                "2",
                "ta998",
                "3"
            ) + System.lineSeparator();
            ByteArrayOutputStream workflowOutput = new ByteArrayOutputStream();
            ApplicantProfileConsoleWorkflow workflow = new ApplicantProfileConsoleWorkflow(
                new ApplicantProfileService(profileRepository, new ApplicantProfileValidator(), userRepository),
                profileIdGenerator,
                new ByteArrayInputStream(consoleInput.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(workflowOutput, true, StandardCharsets.UTF_8)
            );
            workflow.run();

            if (profileRepository.findByUserId("ta998").isEmpty()) {
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

    private static void registerApplicant(AuthService authService, String userId) {
        authService.register(userId, "pass-" + userId, UserRole.APPLICANT);
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

    private static void expectCvCreateFailure(
        ApplicantCvLibraryService cvLibraryService,
        String ownerUserId,
        String title,
        String cvContent,
        String expectedMessagePart
    ) {
        try {
            cvLibraryService.createCv(ownerUserId, title, cvContent);
            throw new IllegalStateException("Expected CV creation to fail for ownerUserId: " + ownerUserId);
        } catch (IllegalArgumentException exception) {
            if (!exception.getMessage().contains(expectedMessagePart)) {
                throw new IllegalStateException("Unexpected CV validation message: " + exception.getMessage(), exception);
            }
        }
    }

    private static void expectAttachCvFailure(
        ApplicantCvService cvService,
        String applicationId,
        String cvId,
        String expectedMessagePart
    ) {
        try {
            cvService.attachCvToApplication(applicationId, cvId);
            throw new IllegalStateException("Expected CV attachment to fail for applicationId: " + applicationId);
        } catch (IllegalArgumentException exception) {
            if (!exception.getMessage().contains(expectedMessagePart)) {
                throw new IllegalStateException("Unexpected CV attachment message: " + exception.getMessage(), exception);
            }
        }
    }

    private static void expectLoadCvFailure(
        ApplicantCvService cvService,
        String applicationId,
        String expectedMessagePart
    ) {
        try {
            cvService.loadCvContentByApplicationId(applicationId);
            throw new IllegalStateException("Expected CV loading to fail for applicationId: " + applicationId);
        } catch (IllegalArgumentException exception) {
            if (!exception.getMessage().contains(expectedMessagePart)) {
                throw new IllegalStateException("Unexpected CV load message: " + exception.getMessage(), exception);
            }
        }
    }

    private static void expectReviewFailure(
        ApplicantCvReviewService reviewService,
        String applicationId,
        String expectedMessagePart
    ) {
        try {
            reviewService.loadReviewByApplicationId(applicationId);
            throw new IllegalStateException("Expected CV review loading to fail for applicationId: " + applicationId);
        } catch (IllegalArgumentException exception) {
            if (!exception.getMessage().contains(expectedMessagePart)) {
                throw new IllegalStateException("Unexpected review load message: " + exception.getMessage(), exception);
            }
        }
    }

    private static void expectJobPublishFailure(
        JobPostingService jobPostingService,
        String organiserId,
        String title,
        String moduleOrActivity,
        String description,
        List<String> requiredSkills,
        int weeklyHours,
        List<String> scheduleSlots,
        String expectedMessagePart
    ) {
        try {
            jobPostingService.publish(
                organiserId,
                title,
                moduleOrActivity,
                description,
                requiredSkills,
                weeklyHours,
                scheduleSlots
            );
            throw new IllegalStateException("Expected job publish to fail for organiserId: " + organiserId);
        } catch (IllegalArgumentException exception) {
            if (!exception.getMessage().contains(expectedMessagePart)) {
                throw new IllegalStateException("Unexpected job publish message: " + exception.getMessage(), exception);
            }
        }
    }
}
