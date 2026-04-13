package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.job.JobPosting;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;

/**
 * 岗位管理页，负责加载 MO 已发布岗位并组织页面布局。
 */
public class JobManagementPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.JOB_MANAGEMENT, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        List<JobPosting> jobs = context.services().jobRepository().findAll().stream()
            .filter(job -> job.organiserId().equals(context.session().userId()))
            .sorted(Comparator.comparing(JobPosting::jobId))
            .toList();

        VBox center = new VBox(20);
        center.setPadding(new Insets(18, 40, 30, 40));
        center.getChildren().add(UiTheme.createPageHeading("Job management"));
        center.getChildren().add(JobManagementSection.create(nav, context, jobs));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox footer = new HBox(UiTheme.createBackButton(nav));
        footer.setAlignment(Pos.CENTER_LEFT);
        center.getChildren().addAll(spacer, footer);

        BorderPane root = UiTheme.createPage(
            "Job Management",
            UiTheme.createMoSidebar(nav, PageId.JOB_MANAGEMENT),
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
