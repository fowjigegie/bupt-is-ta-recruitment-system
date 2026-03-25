package com.bupt.tarecruitment.applicant;

import com.bupt.tarecruitment.application.ApplicationRepository;
import com.bupt.tarecruitment.application.TextFileApplicationRepository;
import com.bupt.tarecruitment.bootstrap.ProjectBootstrap;
import com.bupt.tarecruitment.bootstrap.StartupReport;

public final class ApplicantCvReviewDemoMain {
    private ApplicantCvReviewDemoMain() {
    }

    public static void main(String[] args) {
        StartupReport report = new ProjectBootstrap().initialize();
        ApplicationRepository applicationRepository = new TextFileApplicationRepository(report.dataDirectory());
        ApplicantProfileRepository repository = new TextFileApplicantProfileRepository(report.dataDirectory());
        ApplicantCvReviewService reviewService = new ApplicantCvReviewService(
            applicationRepository,
            repository,
            new TextFileCvStorage(report.dataDirectory())
        );
        ApplicantCvReviewSwingDemo demo = new ApplicantCvReviewSwingDemo(reviewService);
        demo.show();
    }
}
