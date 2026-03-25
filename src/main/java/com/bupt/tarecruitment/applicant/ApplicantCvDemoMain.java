package com.bupt.tarecruitment.applicant;

import com.bupt.tarecruitment.application.ApplicationRepository;
import com.bupt.tarecruitment.application.TextFileApplicationRepository;
import com.bupt.tarecruitment.bootstrap.ProjectBootstrap;
import com.bupt.tarecruitment.bootstrap.StartupReport;

public final class ApplicantCvDemoMain {
    private ApplicantCvDemoMain() {
    }

    public static void main(String[] args) {
        StartupReport report = new ProjectBootstrap().initialize();
        ApplicationRepository applicationRepository = new TextFileApplicationRepository(report.dataDirectory());
        ApplicantProfileRepository repository = new TextFileApplicantProfileRepository(report.dataDirectory());
        ApplicantCvService cvService = new ApplicantCvService(
            applicationRepository,
            repository,
            new TextFileCvStorage(report.dataDirectory())
        );
        ApplicantCvSwingDemo demo = new ApplicantCvSwingDemo(cvService);
        demo.show();
    }
}
