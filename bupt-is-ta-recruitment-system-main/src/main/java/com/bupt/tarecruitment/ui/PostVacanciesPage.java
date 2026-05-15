package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobStatus;
import com.bupt.tarecruitment.mo.JobQualityIssue;
import com.bupt.tarecruitment.mo.JobQualityReport;
import com.bupt.tarecruitment.common.skill.SkillCatalog;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 岗位发布页，负责创建和编辑岗位的整体流程。
 */
public class PostVacanciesPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.POST_VACANCIES, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        VBox scrollBody = new VBox(22);
        scrollBody.setPadding(new Insets(20, 40, 28, 40));
        scrollBody.setMaxWidth(Double.MAX_VALUE);

        Optional<JobPosting> editJob = resolveEditJob(context);
        boolean isEditMode = editJob.isPresent();
        PostVacancyForm form = PostVacancyForm.create(
            context.session().userId(),
            resolveSkillSuggestions(context)
        );
        editJob.ifPresent(form::load);

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setTextFill(Color.web("#b00020"));
        statusLabel.managedProperty().bind(Bindings.createBooleanBinding(
            () -> statusLabel.getText() != null && !statusLabel.getText().isBlank(),
            statusLabel.textProperty()
        ));
        statusLabel.visibleProperty().bind(statusLabel.managedProperty());

        VBox qualityAssistantBox = new VBox(10);
        qualityAssistantBox.setMaxWidth(Double.MAX_VALUE);
        Runnable refreshQualityAssistant = () -> renderQualityAssistant(context, form, isEditMode ? editJob.get().jobId() : "draft-job", qualityAssistantBox);
        form.onChange(refreshQualityAssistant);

        Label titleLabel = new Label(isEditMode ? "Edit Posting" : "New Posting");
        titleLabel.setStyle(
            "-fx-font-size: 32px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #ff66b3;"
        );

        VBox formBox = new VBox(14);
        formBox.setPadding(new Insets(28));
        formBox.setPrefWidth(930);
        formBox.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(26), Insets.EMPTY)));
        formBox.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(26),
            new BorderWidths(2)
        )));

        var publishButton = UiTheme.createPrimaryButton(isEditMode ? "Update" : "Publish", 180, 52);
        publishButton.setOnAction(event -> {
            if (isEditMode) {
                update(context, editJob.get(), form, statusLabel);
            } else {
                publish(context, form, statusLabel);
            }
        });

        var clearButton = UiTheme.createSoftButton("Clear", 110, 46);
        clearButton.setOnAction(event -> {
            if (isEditMode) {
                form.load(editJob.get());
            } else {
                form.clearForCreate();
            }
            statusLabel.setText("");
        });

        HBox actionRow = new HBox(14, clearButton, publishButton);
        actionRow.setAlignment(Pos.CENTER);
        actionRow.setPadding(new Insets(2, 0, 0, 0));

        formBox.getChildren().addAll(
            form.createFirstRow(),
            form.createMetaRow(),
            form.createScheduleSelectorBox(),
            form.createDetailRow(),
            qualityAssistantBox,
            statusLabel,
            actionRow
        );

        refreshQualityAssistant.run();

        scrollBody.getChildren().addAll(titleLabel, formBox);

        ScrollPane scrollPane = new ScrollPane(scrollBody);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle(
            "-fx-background: transparent;" +
                "-fx-background-color: transparent;" +
                "-fx-border-color: transparent;"
        );

        BorderPane root = UiTheme.createPage(
            "Post Vacancies",
            UiTheme.createMoSidebar(nav, PageId.POST_VACANCIES),
            scrollPane,
            nav,
            context
        );
        return UiTheme.createScene(root);
    }

    private static Optional<JobPosting> resolveEditJob(UiAppContext context) {
        return Optional.ofNullable(context.editingJobId())
            .flatMap(jobId -> context.services().jobRepository().findByJobId(jobId))
            .filter(job -> job.organiserId().equals(context.session().userId()))
            .filter(job -> {
                if (job.status() == JobStatus.CLOSED) {
                    context.clearJobEdit();
                    return false;
                }
                return true;
            });
    }

    private static Map<String, List<String>> resolveSkillSuggestions(UiAppContext context) {
        List<String> jobSkills = context.services().jobRepository().findAll().stream()
            .flatMap(job -> job.requiredSkills().stream())
            .toList();
        return SkillCatalog.mergeSuggestedSkillCategories(jobSkills);
    }

    private static void renderQualityAssistant(
        UiAppContext context,
        PostVacancyForm form,
        String draftJobId,
        VBox qualityAssistantBox
    ) {
        qualityAssistantBox.getChildren().clear();

        JobPosting draft = form.toDraftJobPosting(draftJobId, JobStatus.OPEN, 0);
        JobQualityReport report = context.services().moJobQualityService().analyzeDraft(draft);

        Label title = new Label("Live job quality assistant");
        title.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 18));
        title.setStyle("-fx-text-fill: #111111 !important;");

        Label score = new Label("Quality score: " + report.qualityScore() + " / 100");
        score.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 15));
        score.setPadding(new Insets(6, 12, 6, 12));
        score.setStyle(
            "-fx-text-fill: #111111 !important;"
                + "-fx-background-color: " + scoreColor(report.qualityScore()) + ";"
                + "-fx-background-radius: 16;"
        );

        Label status = new Label(report.readyForPublishing() ? "Ready to publish" : "Needs attention before publishing");
        status.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 15));
        status.setStyle("-fx-text-fill: #111111 !important;");

        HBox header = new HBox(12, title, score, status);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox issueList = new VBox(8);
        if (report.issues().isEmpty()) {
            Label empty = new Label("No quality issues detected. This posting is clear and ready.");
            empty.setWrapText(true);
            empty.setFont(javafx.scene.text.Font.font("Arial", 15));
            empty.setStyle("-fx-text-fill: #111111 !important;");
            issueList.getChildren().add(empty);
        } else {
            for (JobQualityIssue issue : report.issues()) {
                issueList.getChildren().add(createQualityIssueRow(issue));
            }
        }

        qualityAssistantBox.getChildren().addAll(header, issueList);
        qualityAssistantBox.setPadding(new Insets(16));
        qualityAssistantBox.setStyle(
            "-fx-background-color: #fff8fb;"
                + "-fx-background-radius: 20;"
                + "-fx-border-color: #f4d9e6;"
                + "-fx-border-width: 1.5;"
                + "-fx-border-radius: 20;"
                + "-fx-text-fill: #111111;"
        );
    }

    private static HBox createQualityIssueRow(JobQualityIssue issue) {
        String color = switch (issue.severity()) {
            case "CRITICAL" -> "#e74c3c";
            case "WARNING" -> "#f39c12";
            default -> "#4664a8";
        };

        Label severity = new Label(issue.severity());
        severity.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 12));
        severity.setPadding(new Insets(4, 9, 4, 9));
        severity.setStyle(
            "-fx-background-color: " + color + ";"
                + "-fx-background-radius: 12;"
                + "-fx-text-fill: #ffffff !important;"
        );

        Label message = new Label(issue.code() + " | " + issue.message());
        message.setWrapText(true);
        message.setStyle("-fx-text-fill: #111111 !important;");

        HBox row = new HBox(10, severity, message);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static String scoreColor(int score) {
        if (score >= 80) {
            return "#2e7d32";
        }
        if (score >= 55) {
            return "#f39c12";
        }
        return "#e74c3c";
    }

    private static void publish(
        UiAppContext context,
        PostVacancyForm form,
        Label statusLabel
    ) {
        statusLabel.setTextFill(Color.web("#b00020"));
        statusLabel.setText("");

        final double weeklyHours;
        try {
            weeklyHours = form.parseWeeklyHours();
        } catch (NumberFormatException exception) {
            statusLabel.setText("Weekly hours must be a positive number.");
            return;
        }

        try {
            JobPosting posting = context.services().jobPostingService().publish(
                form.organiserId(),
                form.title(),
                form.moduleOrActivity(),
                form.activityType(),
                form.description(),
                form.requiredSkills(),
                weeklyHours,
                form.scheduleSlots()
            );
            context.selectJob(posting.jobId());
            statusLabel.setTextFill(Color.web("#2e7d32"));
            statusLabel.setText("Published successfully: " + posting.jobId());
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    private static void update(
        UiAppContext context,
        JobPosting existingJob,
        PostVacancyForm form,
        Label statusLabel
    ) {
        statusLabel.setTextFill(Color.web("#b00020"));
        statusLabel.setText("");

        final double weeklyHours;
        try {
            weeklyHours = form.parseWeeklyHours();
        } catch (NumberFormatException exception) {
            statusLabel.setText("Weekly hours must be a positive number.");
            return;
        }

        try {
            JobPosting updated = new JobPosting(
                existingJob.jobId(),
                form.organiserId(),
                form.title(),
                form.moduleOrActivity(),
                form.activityType(),
                form.description(),
                form.requiredSkills(),
                weeklyHours,
                form.scheduleSlots(),
                existingJob.status() == null ? JobStatus.OPEN : existingJob.status()
            );

            context.services().jobPostingService().publish(updated);
            context.selectJob(updated.jobId());
            context.clearJobEdit();
            statusLabel.setTextFill(Color.web("#2e7d32"));
            statusLabel.setText("Updated successfully: " + updated.jobId());
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
