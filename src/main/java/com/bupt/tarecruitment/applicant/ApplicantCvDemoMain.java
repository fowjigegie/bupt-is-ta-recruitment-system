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
}
