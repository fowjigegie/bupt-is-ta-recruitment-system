package com.bupt.tarecruitment.applicant;

import com.bupt.tarecruitment.bootstrap.ProjectBootstrap;
import com.bupt.tarecruitment.bootstrap.StartupReport;

public final class ApplicantProfileDemoMain {
    private ApplicantProfileDemoMain() {
    }

    // 这是 US01/US05 的独立演示入口：
    // 先完成项目初始化，再组装 profile service 和 id generator，
    // 最后打开 Swing 版演示页面，方便单独演示“创建/编辑 Applicant Profile”。
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
