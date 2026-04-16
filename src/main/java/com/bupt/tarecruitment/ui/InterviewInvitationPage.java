package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.application.ApplicationStatus;
import com.bupt.tarecruitment.job.JobPosting;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;

/**
 * 展示申请状态、面试邀请和录用结果。
 */
public class InterviewInvitationPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.INTERVIEW_INVITATION, stage);
    }

    // US06 主页面：
    // applicant 登录后会在这里看到自己所有申请的状态卡片，按提交时间从新到旧排序。
    static Scene createScene(NavigationManager nav, UiAppContext context) {
        VBox center = new VBox(24);
        center.setPadding(new Insets(35, 40, 28, 40));
        Label title = new Label("My Application Status");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        title.setTextFill(Color.web("#4969ad"));
        center.getChildren().add(title);

        try {
            List<JobApplication> applications = context.services().applicationRepository()
                .findByApplicantUserId(context.session().userId())
                .stream()
                .sorted(Comparator.comparing(JobApplication::submittedAt).reversed())
                .toList();

            if (applications.isEmpty()) {
                center.getChildren().add(UiTheme.createWhiteCard("No applications yet", "Apply for a job from the More Jobs page and the status will appear here."));
            } else {
                for (JobApplication application : applications) {
                    center.getChildren().add(createStatusCard(nav, context, application));
                }
            }
        } catch (RuntimeException exception) {
            center.getChildren().add(
                UiTheme.createWhiteCard(
                    "Application data temporarily unavailable",
                    "We could not load one or more application records just now. Please check the latest saved job/application data and try again.",
                    Color.web("#b00020")
                )
            );
        }

        center.getChildren().add(new HBox(UiTheme.createBackButton(nav)));

        ScrollPane scrollPane = new ScrollPane(center);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(
            "-fx-background:#ffffff;" +
                "-fx-background-color:transparent;" +
                "-fx-border-color:transparent;"
        );

        BorderPane root = UiTheme.createPage(
            "Application Status",
            UiTheme.createApplicantSidebar(nav, PageId.INTERVIEW_INVITATION),
            scrollPane,
            nav,
            context
        );
        return UiTheme.createScene(root);
    }

    // 一张卡片对应一条申请。
    // 左侧显示岗位基本信息和时间，右侧显示状态 chip、applicationId 和查看详情按钮。
    private static VBox createStatusCard(NavigationManager nav, UiAppContext context, JobApplication application) {
        JobPosting job = findJobSafely(context, application.jobId());
        String title = job == null ? application.jobId() : job.title();
        String organiser = formatOrganiserSafely(context, job);
        String schedule = formatScheduleSafely(job);
        Color accentColor = statusColor(application.status());

        VBox wrapper = new VBox();
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(22, 30, 22, 30));
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(24), Insets.EMPTY)));
        card.setBorder(new Border(new BorderStroke(
            accentColor,
            BorderStrokeStyle.SOLID,
            new CornerRadii(24),
            new BorderWidths(3)
        )));

        VBox leftInfo = new VBox(12);
        leftInfo.setAlignment(Pos.CENTER_LEFT);
        leftInfo.setPrefWidth(560);
        leftInfo.getChildren().addAll(
            createHeadline(title),
            createMetaLine("Organiser", organiser),
            createMetaLine("Submitted at", application.submittedAt().toString()),
            createMetaLine("Schedule / room", schedule)
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button detailsButton = new Button("View Details");
        detailsButton.setPrefSize(130, 40);
        detailsButton.setStyle(
            "-fx-background-color: " + toWeb(accentColor) + ";" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 20;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;"
        );
        // 详情按钮不是打开 application 独立页面，而是回到对应 job 的 Job Detail。
        detailsButton.setOnAction(event -> {
            context.selectJob(application.jobId());
            nav.goTo(PageId.JOB_DETAIL);
        });
        detailsButton.setDisable(job == null);

        Label applicationIdLabel = new Label("Application ID: " + application.applicationId());
        applicationIdLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        applicationIdLabel.setTextFill(Color.web("#7b7396"));

        VBox rightInfo = new VBox(14, createStatusChip(statusLabelText(application.status()), accentColor), applicationIdLabel, detailsButton);
        rightInfo.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(leftInfo, spacer, rightInfo);
        wrapper.getChildren().add(card);
        return wrapper;
    }

    private static JobPosting findJobSafely(UiAppContext context, String jobId) {
        try {
            return context.services().jobRepository().findByJobId(jobId).orElse(null);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String formatOrganiserSafely(UiAppContext context, JobPosting job) {
        if (job == null) {
            return "(unknown organiser)";
        }
        try {
            return context.formatUserLabel(job.organiserId());
        } catch (RuntimeException exception) {
            return job.organiserId();
        }
    }

    private static String formatScheduleSafely(JobPosting job) {
        if (job == null || job.scheduleSlots().isEmpty()) {
            return "(schedule not listed)";
        }
        try {
            return FixedScheduleBands.formatScheduleList(job.scheduleSlots());
        } catch (RuntimeException exception) {
            return String.join(", ", job.scheduleSlots());
        }
    }

    private static Label createHeadline(String value) {
        Label label = new Label(value);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 21));
        label.setTextFill(Color.web("#4969ad"));
        return label;
    }

    private static Label createMetaLine(String title, String value) {
        Label label = new Label(title + ": " + value);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        label.setTextFill(Color.web("#5c6481"));
        label.setWrapText(true);
        return label;
    }

    private static StackPane createStatusChip(String status, Color accentColor) {
        Label label = new Label(status);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        label.setTextFill(Color.WHITE);
        StackPane chip = new StackPane(label);
        chip.setPadding(new Insets(8, 18, 8, 18));
        chip.setBackground(new Background(new BackgroundFill(accentColor, new CornerRadii(20), Insets.EMPTY)));
        return chip;
    }

    // 不同状态用不同颜色，方便 applicant 一眼识别当前进度。
    private static Color statusColor(ApplicationStatus status) {
        return switch (status) {
            case SUBMITTED -> Color.web("#ff66b3");
            case ACCEPTED -> Color.web("#2ecc71");
            case REJECTED -> Color.web("#e74c3c");
            case SHORTLISTED -> Color.web("#4969ad");
            default -> Color.web("#8b7fa0");
        };
    }

    // 这里是状态文字的用户友好版本。
    // 例如内部的 SUBMITTED 在页面上显示成 Pending Review。
    private static String statusLabelText(ApplicationStatus status) {
        return switch (status) {
            case SUBMITTED -> "Pending Review";
            case SHORTLISTED -> "Shortlisted";
            case ACCEPTED -> "Accepted";
            case REJECTED -> "Rejected";
            default -> status.name();
        };
    }

    private static String toWeb(Color color) {
        return String.format("#%02x%02x%02x",
            (int) Math.round(color.getRed() * 255),
            (int) Math.round(color.getGreen() * 255),
            (int) Math.round(color.getBlue() * 255));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
