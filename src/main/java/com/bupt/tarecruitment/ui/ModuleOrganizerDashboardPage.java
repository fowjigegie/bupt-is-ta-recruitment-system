package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.application.ApplicationStatus;
import com.bupt.tarecruitment.job.JobPosting;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;

/**
 * MO 首页，负责数据汇总和页面骨架。
 */
public class ModuleOrganizerDashboardPage extends Application {
    /** Same blue as selected item in {@link UiTheme#createSidebarButton(String, boolean)} */
    private static final Color MO_SIDEBAR_BLUE = Color.web("#4565a8");
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.MO_DASHBOARD, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        String userId = context.session().userId();
        List<JobPosting> ownedJobs = context.services().jobRepository().findAll().stream()
            .filter(job -> job.organiserId().equals(userId))
            .sorted(Comparator.comparing(JobPosting::jobId))
            .toList();

        long ownedJobsCount = ownedJobs.size();
        long ownedApplications = context.services().applicationRepository().findAll().stream()
            .filter(application -> context.services().jobRepository().findByJobId(application.jobId())
                .map(job -> job.organiserId().equals(userId))
                .orElse(false))
            .count();
        long pendingReviews = context.services().applicationRepository().findAll().stream()
            .filter(application -> context.services().jobRepository().findByJobId(application.jobId())
                .map(job -> job.organiserId().equals(userId))
                .orElse(false))
            .filter(application -> application.status() == ApplicationStatus.SUBMITTED)
            .count();
        long unreadMessages = context.services().messageService().countUnreadMessagesForUser(userId);

        VBox center = new VBox(18);
        center.setPadding(new Insets(14, 40, 24, 40));
        center.setFillWidth(true);
        center.setMaxWidth(Double.MAX_VALUE);

        Label welcomeTitle = UiTheme.createPageHeading("Welcome Back");
        welcomeTitle.setTextFill(MO_SIDEBAR_BLUE);

        center.getChildren().add(welcomeTitle);

        HBox stats = ModuleOrganizerDashboardSections.createStats(
            ownedJobsCount,
            ownedApplications,
            pendingReviews
        );
        HBox quickActions = ModuleOrganizerDashboardSections.createQuickActions(
            nav,
            context,
            userId,
            pendingReviews,
            unreadMessages
        );
        VBox jobManagementSection = ModuleOrganizerDashboardSections.createJobManagementSection(nav, context, ownedJobs);
        VBox.setVgrow(jobManagementSection, Priority.ALWAYS);

        Label footerHint = new Label("Use Job Management to edit postings, Application Review to process applicants, and Chat to reply to enquiries.");
        footerHint.setFont(Font.font("Arial", 14));
        footerHint.setTextFill(MO_SIDEBAR_BLUE);
        footerHint.setMaxWidth(Double.MAX_VALUE);
        footerHint.setAlignment(Pos.CENTER);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox footer = new HBox(UiTheme.createBackButton(nav));
        footer.setAlignment(Pos.CENTER_LEFT);

        center.getChildren().addAll(stats, quickActions, jobManagementSection, footerHint, spacer, footer);

        BorderPane root = UiTheme.createPage(
            "MO Dashboard",
            UiTheme.createMoSidebar(nav, PageId.MO_DASHBOARD),
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
