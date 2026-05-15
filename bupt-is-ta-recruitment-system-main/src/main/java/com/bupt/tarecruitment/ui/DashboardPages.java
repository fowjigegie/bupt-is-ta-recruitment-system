package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.application.ApplicationStatus;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * 申请人首页，负责统计概览和页面装配。
 */
public class DashboardPages extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.APPLICANT_DASHBOARD, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        long openJobs = context.services().jobRepository().findAll().stream()
            .filter(job -> job.status() == com.bupt.tarecruitment.job.JobStatus.OPEN)
            .count();

        long myApplications = context.services().applicationRepository()
            .findByApplicantUserId(context.session().userId())
            .stream()
            .filter(application -> application.status() != ApplicationStatus.WITHDRAWN)
            .count();

        long unreadMessages = context.services().messageService().countUnreadMessagesForUser(context.session().userId());

        VBox center = new VBox(26);
        center.setPadding(new Insets(30, 40, 30, 40));
        center.setFillWidth(true);
        center.setMaxWidth(Double.MAX_VALUE);

        HBox welcomeRow = ApplicantDashboardSections.createWelcomeRow(
            context.displayNameForUser(context.session().userId())
        );
        HBox featureButtons = ApplicantDashboardSections.createFeatureButtons(nav, context, unreadMessages);
        HBox statRow = ApplicantDashboardSections.createStatRow(openJobs, unreadMessages, myApplications);
        VBox jobsArea = ApplicantDashboardSections.createRecommendedJobsArea(nav, context);

        center.getChildren().addAll(welcomeRow, featureButtons, statRow, jobsArea);

        BorderPane root = UiTheme.createPage(
            "Applicant Dashboard",
            UiTheme.createApplicantSidebar(nav, PageId.APPLICANT_DASHBOARD),
            center,
            nav,
            context
        );
        return UiTheme.createScene(root);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
