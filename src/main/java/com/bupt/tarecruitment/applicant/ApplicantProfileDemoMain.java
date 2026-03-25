package com.bupt.tarecruitment.applicant;

import com.bupt.tarecruitment.bootstrap.ProjectBootstrap;
import com.bupt.tarecruitment.bootstrap.StartupReport;

public final class ApplicantProfileDemoMain {
    private ApplicantProfileDemoMain() {
    }

    public static void main(String[] args) {
        StartupReport report = new ProjectBootstrap().initialize();
        ApplicantProfileRepository repository = new TextFileApplicantProfileRepository(report.dataDirectory());
        ApplicantProfileService service = new ApplicantProfileService(repository, new ApplicantProfileValidator());
        ApplicantProfileSwingDemo demo = new ApplicantProfileSwingDemo(
            service,
            new ApplicantProfileIdGenerator(repository)
        );
        demo.show();
    }
}
