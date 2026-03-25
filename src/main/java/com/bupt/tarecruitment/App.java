package com.bupt.tarecruitment;

import com.bupt.tarecruitment.bootstrap.ProjectBootstrap;
import com.bupt.tarecruitment.bootstrap.StartupReport;

public final class App {
    private App() {
    }

    public static void main(String[] args) {
        StartupReport report = new ProjectBootstrap().initialize();

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

        System.out.println("Next step: each teammate can now implement one module on a feature branch.");
    }
}
