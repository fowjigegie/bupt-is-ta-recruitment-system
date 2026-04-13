package com.bupt.tarecruitment;

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
import com.bupt.tarecruitment.communication.ConversationReference;
import com.bupt.tarecruitment.communication.InquiryMessage;
import com.bupt.tarecruitment.communication.MessageService;
import com.bupt.tarecruitment.communication.TextFileMessageRepository;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;
import com.bupt.tarecruitment.job.JobStatus;
import com.bupt.tarecruitment.job.TextFileJobRepository;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 覆盖 US02、US06、US08 场景的集成流程测试。
 */
public final class US020608DetailedIntegrationTest {
    private US020608DetailedIntegrationTest() {
    }

    public static void main(String[] args) {
        try {
            Path tempDataDirectory = Files.createTempDirectory("us020608-detailed");
            bootstrapDataFiles(tempDataDirectory);

            UserRepository userRepository = new TextFileUserRepository(tempDataDirectory);
            ApplicantProfileRepository profileRepository = new TextFileApplicantProfileRepository(tempDataDirectory);
            ApplicantCvRepository cvRepository = new TextFileApplicantCvRepository(tempDataDirectory);
            ApplicationRepository applicationRepository = new TextFileApplicationRepository(tempDataDirectory);
            JobRepository jobRepository = new TextFileJobRepository(tempDataDirectory);
            MessageService messageService = new MessageService(new TextFileMessageRepository(tempDataDirectory));

            AuthService authService = new AuthService(userRepository, new AuthValidator());
            authService.register("ta101", "pass-ta101", UserRole.APPLICANT);
            authService.register("ta202", "pass-ta202", UserRole.APPLICANT);
            authService.register("mo101", "pass-mo101", UserRole.MO);
            authService.register("mo202", "pass-mo202", UserRole.MO);

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
            ApplicationDecisionService decisionService = new ApplicationDecisionService(
                applicationRepository,
                jobRepository,
                userRepository
            );

            profileService.createProfile(buildProfile("profile101", "ta101", "231225101", "Applicant One"));
            profileService.createProfile(buildProfile("profile202", "ta202", "231225202", "Applicant Two"));

            jobRepository.save(buildJob("job100", "mo101", JobStatus.OPEN));
            jobRepository.save(buildJob("job200", "mo202", JobStatus.OPEN));

            ApplicantCv ta101CvV1 = cvLibraryService.createCv(
                "ta101",
                "TA101 CV V1",
                "TA101 CV content version one"
            );
            ApplicantCv ta101CvV2 = cvLibraryService.createCv(
                "ta101",
                "TA101 CV V2",
                "TA101 CV content version two"
            );
            ApplicantCv ta202Cv = cvLibraryService.createCv(
                "ta202",
                "TA202 CV",
                "TA202 CV content"
            );

            JobApplication createdApplication = jobApplicationService.applyToJobWithCv("ta101", "job100", ta101CvV1.cvId());
            assertEquals("job100", createdApplication.jobId(), "Application should be linked to selected job.");
            assertEquals("ta101", createdApplication.applicantUserId(), "Application should be linked to logged-in user.");
            assertEquals(ta101CvV1.cvId(), createdApplication.cvId(), "Application should be linked to selected CV.");
            assertEquals(ApplicationStatus.SUBMITTED, createdApplication.status(), "New application should start as SUBMITTED.");

            expectFailure(
                () -> jobApplicationService.applyToJobWithCv("ta101", "job100", ta101CvV1.cvId()),
                "Duplicate application"
            );

            JobApplication reattached = cvService.attachCvToApplication(createdApplication.applicationId(), ta101CvV2.cvId());
            assertEquals(ta101CvV2.cvId(), reattached.cvId(), "Attach CV should overwrite cvId on this application.");
            assertEquals(
                "TA101 CV content version two",
                cvService.loadCvContentByApplicationId(createdApplication.applicationId()),
                "Loading CV by application should reflect the latest attached CV."
            );

            expectFailure(
                () -> cvService.attachCvToApplication(createdApplication.applicationId(), ta202Cv.cvId()),
                "does not belong to applicantUserId"
            );

            JobApplication shortlisted = decisionService.updateStatus(
                "mo101",
                createdApplication.applicationId(),
                ApplicationStatus.SHORTLISTED,
                "Please prepare for interview."
            );
            assertEquals(ApplicationStatus.SHORTLISTED, shortlisted.status(), "MO should be able to shortlist.");

            JobApplication accepted = decisionService.updateStatus(
                "mo101",
                createdApplication.applicationId(),
                ApplicationStatus.ACCEPTED,
                "Offer approved."
            );
            assertEquals(ApplicationStatus.ACCEPTED, accepted.status(), "MO should be able to accept.");
            assertEquals("Offer approved.", accepted.reviewerNote(), "Reviewer note should persist.");
            assertEquals("Accepted", ApplicationStatusPresenter.toDisplayText(accepted.status()), "TA display status should update.");

            JobApplication taView = applicationRepository.findByApplicantUserId("ta101").stream()
                .filter(application -> application.applicationId().equals(createdApplication.applicationId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("TA cannot find updated application by applicantUserId."));
            assertEquals(ApplicationStatus.ACCEPTED, taView.status(), "TA view should observe MO-updated status.");

            expectFailure(
                () -> decisionService.updateStatus(
                    "mo202",
                    createdApplication.applicationId(),
                    ApplicationStatus.REJECTED,
                    "Not your job."
                ),
                "own jobs"
            );

            InquiryMessage m1 = messageService.sendMessage(
                "job100",
                "ta101",
                "mo101",
                "Hi MO, can you clarify expected weekly workload..."
            );
            InquiryMessage m2 = messageService.sendMessage(
                "job100",
                "ta101",
                "mo101",
                "Also, does this include grading duties..."
            );
            InquiryMessage m3 = messageService.sendMessage(
                "job100",
                "mo101",
                "ta101",
                "Yes, around 4 hours weekly including grading."
            );

            List<InquiryMessage> conversation = messageService.listConversation("job100", "ta101", "mo101");
            assertEquals(3, conversation.size(), "Conversation should include both directions under the same job.");
            assertEquals(m1.messageId(), conversation.get(0).messageId(), "Conversation order should keep earliest message first.");
            assertEquals(m2.messageId(), conversation.get(1).messageId(), "Conversation order should keep chronological order.");
            assertEquals(m3.messageId(), conversation.get(2).messageId(), "Conversation order should keep latest message last.");

            assertEquals(2L, messageService.countUnreadMessagesForUser("mo101"), "MO should have two unread messages.");
            assertEquals(1L, messageService.countUnreadMessagesForUser("ta101"), "TA should have one unread message.");

            assertEquals(2, messageService.markConversationAsRead("job100", "mo101", "ta101"), "MO should mark two as read.");
            assertEquals(0L, messageService.countUnreadMessagesForUser("mo101"), "MO unread count should become zero.");
            assertEquals(1, messageService.markConversationAsRead("job100", "ta101", "mo101"), "TA should mark one as read.");
            assertEquals(0L, messageService.countUnreadMessagesForUser("ta101"), "TA unread count should become zero.");

            ConversationReference latestConversation = messageService.findMostRecentConversationForUser("ta101")
                .orElseThrow(() -> new IllegalStateException("No latest conversation found for ta101."));
            assertEquals("job100", latestConversation.jobId(), "Latest conversation should point to the current job.");
            assertEquals("mo101", latestConversation.peerUserId(), "Latest conversation peer should be MO.");

            expectFailure(
                () -> messageService.sendMessage("job100", "ta101", "ta101", "self message"),
                "must be different"
            );

            System.out.println("US02/US06/US08 detailed integration test passed.");
            System.out.println("Temp data directory: " + tempDataDirectory);
        } catch (Exception exception) {
            throw new IllegalStateException("US02/US06/US08 detailed integration test failed.", exception);
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
        Files.write(
            tempDataDirectory.resolve(DataFile.MESSAGES.fileName()),
            List.of(DataFile.MESSAGES.initialLines().getFirst()),
            StandardCharsets.UTF_8
        );
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

    private static JobPosting buildJob(String jobId, String organiserId, JobStatus status) {
        return new JobPosting(
            jobId,
            organiserId,
            "TA role " + jobId,
            "Module " + jobId,
            "Support teaching activities for " + jobId,
            List.of("Java"),
            4,
            List.of("WED-14:00-16:00"),
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
