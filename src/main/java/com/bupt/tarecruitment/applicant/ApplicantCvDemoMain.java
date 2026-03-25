package com.bupt.tarecruitment.applicant;

import com.bupt.tarecruitment.bootstrap.ProjectBootstrap;
import com.bupt.tarecruitment.bootstrap.StartupReport;

public final class ApplicantCvDemoMain {
    private ApplicantCvDemoMain() {
    }

    public static void main(String[] args) {
        StartupReport report = new ProjectBootstrap().initialize();
        ApplicantProfileRepository repository = new TextFileApplicantProfileRepository(report.dataDirectory());
        ApplicantProfileService profileService = new ApplicantProfileService(repository, new ApplicantProfileValidator());
        ApplicantCvService cvService = new ApplicantCvService(
            repository,
            profileService,
            new TextFileCvStorage(report.dataDirectory())
        );
        ApplicantCvSwingDemo demo = new ApplicantCvSwingDemo(cvService);
        demo.show();
    }
}
