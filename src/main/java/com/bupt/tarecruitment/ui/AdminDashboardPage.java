package com.bupt.tarecruitment.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * 管理员首页，负责系统概览和页面装配。
 */
public class AdminDashboardPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.ADMIN_DASHBOARD, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        long userCount = context.services().userRepository().findAll().size();
        long jobCount = context.services().jobRepository().findAll().size();
        long applicationCount = context.services().applicationRepository().findAll().size();

        VBox center = new VBox(24);
        center.setPadding(new Insets(34, 46, 34, 46));

        HBox stats = new HBox(20,
            UiTheme.createStatCard("Users", Long.toString(userCount), "User records come directly from data/users.txt."),
            UiTheme.createStatCard("Jobs", Long.toString(jobCount), "Jobs currently stored across the project."),
            UiTheme.createStatCard("Applications", Long.toString(applicationCount), "Applications currently stored across the project.")
        );

        HBox lowerCards = new HBox(20,
            UiTheme.createPlaceholderCard("Current user", context.session().displayName() + " is signed in as " + context.session().role() + "."),
            UiTheme.createPlaceholderCard("Data directory", context.startupReport().dataDirectory().toString()),
            UiTheme.createPlaceholderCard("Workload rule", "Only ACCEPTED TA assignments are counted. The default overload limit is 10 weekly hours.")
        );

        AdminWorkloadPanel workloadPanel = AdminWorkloadPanel.create(context);

        center.getChildren().addAll(
            UiTheme.createPageHeading("Admin dashboard"),
            UiTheme.createMutedText("Use this page to monitor accepted TA allocations and spot overload, schedule conflict, or schedule-data risks."),
            stats,
            lowerCards,
            workloadPanel.container()
        );

        BorderPane root = UiTheme.createPage("Admin Dashboard", null, center, nav, context);
        return UiTheme.createScene(root);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
