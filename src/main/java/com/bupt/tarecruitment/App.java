package com.bupt.tarecruitment;

import com.bupt.tarecruitment.bootstrap.ProjectBootstrap;
import com.bupt.tarecruitment.bootstrap.StartupReport;

import java.util.Locale;

public final class App {
    private App() {
    }

    public static void main(String[] args) {
        StartupReport report = new ProjectBootstrap().initialize();
        ProjectModuleFactory moduleFactory = new ProjectModuleFactory(report.dataDirectory());

        if (args.length > 0) {
            if (isHelpCommand(args[0])) {
                printHelp(report);
                return;
            }

            if (isMainUiCommand(args[0])) {
                runMainUi(moduleFactory);
                return;
            }

            if (isUs00Command(args[0])) {
                runUs00Workflow(moduleFactory);
                return;
            }

            if (isJobPostingUiCommand(args[0])) {
                runJobPostingUi(moduleFactory);
                return;
            }

            if (isUs04UiCommand(args[0])) {
                if (args.length < 3) {
                    System.out.println("Usage: us04-ui <jobId> <applicantUserId>");
                    System.out.println("Example:");
                    System.out.println(" - powershell -ExecutionPolicy Bypass -File scripts\\run.ps1 us04-ui job001 ta001");
                    return;
                }
                runUs04Ui(moduleFactory, args[1], args[2]);
                return;
            }

            if (isCvReviewUiCommand(args[0])) {
                runCvReviewUi(moduleFactory);
                return;
            }

            if (isUs02UiCommand(args[0])) {
                runUs02Ui(moduleFactory);
                return;
            }

            if (isUs01UiCommand(args[0])) {
                runUs01Ui(moduleFactory);
                return;
            }

            if (isUs01Command(args[0])) {
                runUs01Workflow(moduleFactory);
                return;
            }

            System.out.println("Unknown command: " + args[0]);
            printHelp(report);
            return;
        }

        runMainUi(moduleFactory);
    }

    private static void printHelp(StartupReport report) {
        System.out.println("BUPT International School TA Recruitment System");
        System.out.println("Starter scaffold is ready. Default startup now opens the main Swing UI.");
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
        System.out.println(" - main-ui : open the unified Swing launcher");
        System.out.println(" - us00 : run the registration and login demo");
        System.out.println(" - us01 : run the applicant profile creation demo");
        System.out.println(" - us01-ui : open a lightweight Swing test UI for the applicant profile module");
        System.out.println(" - us02-ui : open a lightweight Swing test UI for the CV submission module");
        System.out.println(" - job-post-ui : open a Swing demo UI for creating/publishing JobPostings");
        System.out.println(" - us04-ui <jobId> <applicantUserId> : open a stub job detail page with an APPLY button");
        System.out.println(" - cv-review-ui : open a lightweight read-only CV review demo");
        System.out.println("Example:");
        System.out.println(" - powershell -ExecutionPolicy Bypass -File scripts\\run.ps1");
        System.out.println(" - powershell -ExecutionPolicy Bypass -File scripts\\run.ps1 help");
        System.out.println(" - powershell -ExecutionPolicy Bypass -File scripts\\run.ps1 us00");
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

    private static boolean isUs00Command(String command) {
        String normalized = command.toLowerCase(Locale.ROOT);
        return normalized.equals("us00")
            || normalized.equals("us00-demo")
            || normalized.equals("auth");
    }

    private static boolean isHelpCommand(String command) {
        String normalized = command.toLowerCase(Locale.ROOT);
        return normalized.equals("help")
            || normalized.equals("--help")
            || normalized.equals("-h");
    }

    private static boolean isMainUiCommand(String command) {
        String normalized = command.toLowerCase(Locale.ROOT);
        return normalized.equals("main-ui")
            || normalized.equals("ui")
            || normalized.equals("launcher");
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

    private static void runMainUi(ProjectModuleFactory moduleFactory) {
        new MainLauncherSwing(moduleFactory).show();
    }

    private static void runUs01Workflow(ProjectModuleFactory moduleFactory) {
        moduleFactory.createUs01Workflow(System.in, System.out).run();
    }

    private static void runUs00Workflow(ProjectModuleFactory moduleFactory) {
        moduleFactory.createUs00Workflow(System.in, System.out).run();
    }

    private static void runUs01Ui(ProjectModuleFactory moduleFactory) {
        moduleFactory.createUs01Ui().show();
    }

    private static void runUs02Ui(ProjectModuleFactory moduleFactory) {
        moduleFactory.createUs02Ui().show();
    }

    private static void runCvReviewUi(ProjectModuleFactory moduleFactory) {
        moduleFactory.createCvReviewUi().show();
    }

    private static void runJobPostingUi(ProjectModuleFactory moduleFactory) {
        moduleFactory.createJobPostingUi().show();
    }

    private static void runUs04Ui(ProjectModuleFactory moduleFactory, String jobId, String applicantUserId) {
        moduleFactory.createUs04Ui(jobId, applicantUserId).show();
    }
}
