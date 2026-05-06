package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobStatus;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 岗位管理页中的岗位列表区，负责行渲染和状态操作。
 */
final class JobManagementSection {
    private JobManagementSection() {
    }

    static VBox create(NavigationManager nav, UiAppContext context, List<JobPosting> jobs) {
        VBox section = new VBox(16);
        section.setFillWidth(true);
        section.setMaxWidth(Double.MAX_VALUE);
        if (jobs.isEmpty()) {
            section.getChildren().add(UiTheme.createWhiteCard("No jobs yet", "Publish a vacancy first and it will appear here."));
            return section;
        }

        for (JobPosting job : jobs) {
            section.getChildren().add(createListingRow(nav, context, job));
        }
        return section;
    }

    private static VBox createListingRow(NavigationManager nav, UiAppContext context, JobPosting job) {
        long applicants = context.services().applicationRepository().findAll().stream()
            .filter(application -> application.jobId().equals(job.jobId()))
            .count();
        boolean isClosed = job.status() == JobStatus.CLOSED;

        HBox row = new HBox(18);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setPadding(new Insets(20, 24, 20, 24));
        row.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(24), Insets.EMPTY)));
        row.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(24),
            new BorderWidths(1.5)
        )));

        VBox textBox = new VBox(6);
        textBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        textBox.setMinWidth(420);
        textBox.setPrefWidth(560);
        textBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label titleLabel = new Label(job.title());
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.web("#4664a8"));
        titleLabel.setStyle("-fx-text-fill: #4664a8;");
        titleLabel.setWrapText(true);

        Label applicantLabel = UiTheme.createMutedText(
            job.jobId() + " | " + job.moduleOrActivity() + " | " + job.activityType() + " | applicants: " + applicants
        );
        applicantLabel.setTextFill(Color.web("#4d588f"));
        applicantLabel.setStyle("-fx-text-fill: #4d588f;");
        textBox.getChildren().addAll(titleLabel, applicantLabel);

        Label statusLabel = new Label(job.status().name());
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        statusLabel.setTextFill(Color.WHITE);
        statusLabel.setMinWidth(88);
        statusLabel.setPrefWidth(88);
        statusLabel.setAlignment(javafx.geometry.Pos.CENTER);
        statusLabel.setStyle(
            "-fx-background-color: " + (isClosed ? "#e53935" : "#7bb661") + ";" +
                "-fx-background-radius: 18;" +
                "-fx-padding: 6 14 6 14;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        var editButton = UiTheme.createSoftButton("Edit Details", 150, 42);
        if (isClosed) {
            editButton.setDisable(true);
            editButton.setOpacity(0.55);
        }
        editButton.setOnAction(event -> {
            context.beginEditJob(job.jobId());
            nav.goTo(PageId.POST_VACANCIES);
        });

        var changeStatusButton = UiTheme.createSoftButton("Change Status", 170, 42);
        Polygon triangle = new Polygon(
            0.0, 0.0,
            10.0, 0.0,
            5.0, 7.0
        );
        triangle.setFill(Color.web("#333333"));
        changeStatusButton.setGraphic(triangle);
        changeStatusButton.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        changeStatusButton.setGraphicTextGap(10);

        ContextMenu statusMenu = createStatusMenu(nav, context, job, isClosed);
        changeStatusButton.setOnAction(event -> toggleStatusMenu(changeStatusButton, statusMenu));

        var reviewButton = UiTheme.createSoftButton("View applicants", 150, 42);
        reviewButton.setOnAction(event -> {
            context.selectJob(job.jobId());
            JobApplication firstApplication = context.services().applicationRepository().findAll().stream()
                .filter(application -> application.jobId().equals(job.jobId()))
                .sorted(Comparator.comparing(JobApplication::applicationId))
                .findFirst()
                .orElse(null);
            context.selectApplication(firstApplication == null ? null : firstApplication.applicationId());
            nav.goTo(PageId.APPLICATION_REVIEW);
        });

        HBox actionBox = new HBox(12, editButton, changeStatusButton, reviewButton);
        actionBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        row.getChildren().addAll(textBox, statusLabel, spacer, actionBox);
        return new VBox(row);
    }

    private static ContextMenu createStatusMenu(
        NavigationManager nav,
        UiAppContext context,
        JobPosting job,
        boolean isClosed
    ) {
        ContextMenu statusMenu = new ContextMenu();
        MenuItem openItem = new MenuItem("Open");
        MenuItem closeItem = new MenuItem("Close");

        openItem.setOnAction(event -> {
            if (!isClosed) {
                return;
            }
            updateJobStatus(context, job, JobStatus.OPEN);
            nav.replace(PageId.JOB_MANAGEMENT);
        });
        closeItem.setOnAction(event -> {
            if (isClosed) {
                return;
            }
            if (!confirmClose(job)) {
                return;
            }
            updateJobStatus(context, job, JobStatus.CLOSED);
            nav.replace(PageId.JOB_MANAGEMENT);
        });
        statusMenu.getItems().addAll(openItem, closeItem);
        return statusMenu;
    }

    private static void toggleStatusMenu(javafx.scene.control.Button changeStatusButton, ContextMenu statusMenu) {
        if (statusMenu.isShowing()) {
            statusMenu.hide();
            return;
        }

        double x = changeStatusButton.getWidth() - 18;
        double y = changeStatusButton.getHeight();
        var point = changeStatusButton.localToScreen(x, y);
        if (point != null) {
            statusMenu.show(changeStatusButton, point.getX(), point.getY());
        } else {
            statusMenu.show(changeStatusButton, Side.BOTTOM, 0, 0);
        }
    }

    private static boolean confirmClose(JobPosting job) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm status change");
        alert.setHeaderText("Close this posting...");
        alert.setContentText(
            "Are you sure you want to set this job to CLOSED...\n\n" +
                "Job: " + job.jobId() + " | " + job.title() + "\n\n" +
                "Applicants will no longer be able to apply."
        );
        Optional<ButtonType> choice = alert.showAndWait();
        return choice.isPresent() && choice.get() == ButtonType.OK;
    }

    private static void updateJobStatus(UiAppContext context, JobPosting job, JobStatus nextStatus) {
        JobPosting updated = new JobPosting(
            job.jobId(),
            job.organiserId(),
            job.title(),
            job.moduleOrActivity(),
            job.activityType(),
            job.description(),
            job.requiredSkills(),
            job.weeklyHours(),
            job.scheduleSlots(),
            nextStatus
        );
        context.services().jobPostingService().publish(updated);
        context.selectJob(job.jobId());
    }
}
