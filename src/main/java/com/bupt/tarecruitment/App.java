package com.bupt.tarecruitment;

import com.bupt.tarecruitment.application.ApplicationRepository;
import com.bupt.tarecruitment.application.ApplicationIdGenerator;
import com.bupt.tarecruitment.application.JobApplicationService;
import com.bupt.tarecruitment.application.TextFileApplicationRepository;
import com.bupt.tarecruitment.job.JobDetailApplyStubSwingDemo;
import com.bupt.tarecruitment.job.JobIdGenerator;
import com.bupt.tarecruitment.job.JobRepository;
import com.bupt.tarecruitment.job.JobPostingSwingDemo;
import com.bupt.tarecruitment.job.TextFileJobRepository;
import com.bupt.tarecruitment.applicant.ApplicantProfileConsoleWorkflow;
import com.bupt.tarecruitment.applicant.ApplicantCvIdGenerator;
import com.bupt.tarecruitment.applicant.ApplicantCvLibraryService;
import com.bupt.tarecruitment.applicant.ApplicantCvRepository;
import com.bupt.tarecruitment.applicant.ApplicantCvService;
import com.bupt.tarecruitment.applicant.ApplicantCvSwingDemo;
import com.bupt.tarecruitment.applicant.ApplicantCvReviewService;
import com.bupt.tarecruitment.applicant.ApplicantCvReviewSwingDemo;
import com.bupt.tarecruitment.applicant.ApplicantProfileIdGenerator;
import com.bupt.tarecruitment.applicant.ApplicantProfileRepository;
import com.bupt.tarecruitment.applicant.ApplicantProfileService;
import com.bupt.tarecruitment.applicant.ApplicantProfileSwingDemo;
import com.bupt.tarecruitment.applicant.ApplicantProfileValidator;
import com.bupt.tarecruitment.applicant.TextFileApplicantCvRepository;
import com.bupt.tarecruitment.applicant.TextFileCvStorage;
import com.bupt.tarecruitment.applicant.TextFileApplicantProfileRepository;
import com.bupt.tarecruitment.bootstrap.ProjectBootstrap;
import com.bupt.tarecruitment.bootstrap.StartupReport;

import java.util.Locale;

public final class App {
    private App() {
    }

    public static void main(String[] args) {
        StartupReport report = new ProjectBootstrap().initialize();

        if (args.length > 0) {
            if (isJobPostingUiCommand(args[0])) {
                runJobPostingUi(report);
                return;
            }

            if (isUs04UiCommand(args[0])) {
                if (args.length < 3) {
                    System.out.println("Usage: us04-ui <jobId> <applicantUserId>");
                    System.out.println("Example:");
                    System.out.println(" - powershell -ExecutionPolicy Bypass -File scripts\\run.ps1 us04-ui job001 ta001");
                    return;
                }
                runUs04Ui(report, args[1], args[2]);
                return;
            }

            if (isCvReviewUiCommand(args[0])) {
                runCvReviewUi(report);
                return;
            }

            if (isUs02UiCommand(args[0])) {
                runUs02Ui(report);
                return;
            }

            if (isUs01UiCommand(args[0])) {
                runUs01Ui(report);
                return;
            }

            if (isUs01Command(args[0])) {
                runUs01Workflow(report);
                return;
            }
        }

        System.out.println("BUPT International School TA Recruitment System");
        System.out.println("Starter scaffold is ready.");
        System.out.println("Data directory: " + report.dataDirectory().toAbsolutePath());
        System.out.println("Core modules: auth, applicant, job, application, admin, communication, recommendation");

        if (report.createdFiles().isEmpty()) {
            System.out.println("Data files already existed. No new files were created.");
        } else {
            System.out.println("Created starter data files:");
            for (var file : report.createdFiles()) {
                System.out.println(" - " + file.getFileName());
            }
        }

        System.out.println("Available demo commands:");
        System.out.println(" - us01 : run the applicant profile creation demo");
        System.out.println(" - us01-ui : open a lightweight Swing test UI for the applicant profile module");
        System.out.println(" - us02-ui : open a lightweight Swing test UI for the CV submission module");
        System.out.println(" - job-post-ui : open a Swing demo UI for creating/publishing JobPostings");
        System.out.println(" - us04-ui <jobId> <applicantUserId> : open a stub job detail page with an APPLY button");
        System.out.println(" - cv-review-ui : open a lightweight read-only CV review demo");
        System.out.println("Example:");
        System.out.println(" - powershell -ExecutionPolicy Bypass -File scripts\\run.ps1 us01");
        System.out.println(" - powershell -ExecutionPolicy Bypass -File scripts\\run.ps1 us01-ui");
        System.out.println(" - powershell -ExecutionPolicy Bypass -File scripts\\run.ps1 us02-ui");
        System.out.println(" - powershell -ExecutionPolicy Bypass -File scripts\\run.ps1 job-post-ui");
        System.out.println(" - powershell -ExecutionPolicy Bypass -File scripts\\run.ps1 us04-ui job001 ta001");
        System.out.println(" - powershell -ExecutionPolicy Bypass -File scripts\\run.ps1 cv-review-ui");
    }

    private static boolean isUs01Command(String command) {
        String normalized = command.toLowerCase(Locale.ROOT);
        return normalized.equals("us01")
            || normalized.equals("us01-demo")
            || normalized.equals("applicant-profile");
    }

    private static boolean isUs01UiCommand(String command) {
        String normalized = command.toLowerCase(Locale.ROOT);
        return normalized.equals("us01-ui")
            || normalized.equals("applicant-profile-ui")
            || normalized.equals("us01-gui");
    }

    private static boolean isUs02UiCommand(String command) {
        String normalized = command.toLowerCase(Locale.ROOT);
        return normalized.equals("us02-ui")
            || normalized.equals("submit-cv-ui")
            || normalized.equals("us02-gui")
            || normalized.equals("us02");
    }

    private static boolean isCvReviewUiCommand(String command) {
        String normalized = command.toLowerCase(Locale.ROOT);
        return normalized.equals("cv-review-ui")
            || normalized.equals("us02-review-ui")
            || normalized.equals("mo-cv-review");
    }

    private static boolean isJobPostingUiCommand(String command) {
        String normalized = command.toLowerCase(Locale.ROOT);
        return normalized.equals("job-post-ui")
            || normalized.equals("post-vacancies-ui")
            || normalized.equals("job-ui");
    }

    private static boolean isUs04UiCommand(String command) {
        String normalized = command.toLowerCase(Locale.ROOT);
        return normalized.equals("us04-ui")
            || normalized.equals("apply-job-ui")
            || normalized.equals("us04-gui");
    }

    private static void runUs01Workflow(StartupReport report) {
        ApplicantProfileRepository repository = new TextFileApplicantProfileRepository(report.dataDirectory());
        ApplicantProfileService service = new ApplicantProfileService(repository, new ApplicantProfileValidator());
        ApplicantProfileConsoleWorkflow workflow = new ApplicantProfileConsoleWorkflow(
            service,
            new ApplicantProfileIdGenerator(repository),
            System.in,
            System.out
        );
        workflow.run();
    }

    private static void runUs01Ui(StartupReport report) {
        ApplicantProfileRepository repository = new TextFileApplicantProfileRepository(report.dataDirectory());
        ApplicantProfileService service = new ApplicantProfileService(repository, new ApplicantProfileValidator());
        ApplicantProfileSwingDemo demo = new ApplicantProfileSwingDemo(
            service,
            new ApplicantProfileIdGenerator(repository)
        );
        demo.show();
    }

    private static void runUs02Ui(StartupReport report) {
        ApplicationRepository applicationRepository = new TextFileApplicationRepository(report.dataDirectory());
        ApplicantProfileRepository profileRepository = new TextFileApplicantProfileRepository(report.dataDirectory());
        ApplicantCvRepository cvRepository = new TextFileApplicantCvRepository(report.dataDirectory());
        ApplicantCvLibraryService cvLibraryService = new ApplicantCvLibraryService(
            profileRepository,
            cvRepository,
            new ApplicantCvIdGenerator(cvRepository),
            new TextFileCvStorage(report.dataDirectory())
        );
        ApplicantCvService cvService = new ApplicantCvService(
            applicationRepository,
            cvRepository,
            new TextFileCvStorage(report.dataDirectory())
        );
        ApplicantCvSwingDemo demo = new ApplicantCvSwingDemo(cvLibraryService, cvService);
        demo.show();
    }

    private static void runCvReviewUi(StartupReport report) {
        ApplicationRepository applicationRepository = new TextFileApplicationRepository(report.dataDirectory());
        ApplicantProfileRepository profileRepository = new TextFileApplicantProfileRepository(report.dataDirectory());
        ApplicantCvRepository cvRepository = new TextFileApplicantCvRepository(report.dataDirectory());
        ApplicantCvReviewService reviewService = new ApplicantCvReviewService(
            applicationRepository,
            cvRepository,
            profileRepository,
            new TextFileCvStorage(report.dataDirectory())
        );
        ApplicantCvReviewSwingDemo demo = new ApplicantCvReviewSwingDemo(reviewService);
        demo.show();
    }

    private static void runJobPostingUi(StartupReport report) {
        JobRepository jobRepository = new TextFileJobRepository(report.dataDirectory());
        JobPostingSwingDemo demo = new JobPostingSwingDemo(
            jobRepository,
            new JobIdGenerator(jobRepository)
        );
        demo.show();
    }

    private static void runUs04Ui(StartupReport report, String jobId, String applicantUserId) {
        JobRepository jobRepository = new TextFileJobRepository(report.dataDirectory());
        ApplicationRepository applicationRepository = new TextFileApplicationRepository(report.dataDirectory());
        ApplicantProfileRepository profileRepository = new TextFileApplicantProfileRepository(report.dataDirectory());
        ApplicantCvRepository cvRepository = new TextFileApplicantCvRepository(report.dataDirectory());
        ApplicantCvLibraryService cvLibraryService = new ApplicantCvLibraryService(
            profileRepository,
            cvRepository,
            new ApplicantCvIdGenerator(cvRepository),
            new TextFileCvStorage(report.dataDirectory())
        );
        JobApplicationService applicationService = new JobApplicationService(
            jobRepository,
            applicationRepository,
            new ApplicationIdGenerator(applicationRepository),
            profileRepository,
            cvRepository
        );

        JobDetailApplyStubSwingDemo demo = new JobDetailApplyStubSwingDemo(
            jobId,
            applicantUserId,
            cvLibraryService,
            applicationService
        );
        demo.show();
    }
}
