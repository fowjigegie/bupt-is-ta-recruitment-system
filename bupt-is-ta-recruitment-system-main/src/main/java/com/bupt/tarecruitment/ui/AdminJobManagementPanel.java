package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobStatus;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Admin-facing global job list with status filtering, integrity checks, and close control.
 */
final class AdminJobManagementPanel {
    private static final String ALL_STATUSES = "All statuses";
    private static final int MAX_VISIBLE_JOB_ROWS = 8;
    private static final double JOB_ROW_HEIGHT = 92;
    private static final double JOB_ROW_GAP = 10;

    private final UiAppContext context;
    private final VBox container;
    private final ComboBox<String> statusFilter;
    private final VBox jobList;
    private final ScrollPane jobScroll;
    private final Label summaryLabel;

    private AdminJobManagementPanel(
        UiAppContext context,
        VBox container,
        ComboBox<String> statusFilter,
        VBox jobList,
        ScrollPane jobScroll,
        Label summaryLabel
    ) {
        this.context = context;
        this.container = container;
        this.statusFilter = statusFilter;
        this.jobList = jobList;
        this.jobScroll = jobScroll;
        this.summaryLabel = summaryLabel;
    }

    static AdminJobManagementPanel create(UiAppContext context) {
        ComboBox<String> statusFilter = createStatusFilter();
        Label summaryLabel = UiTheme.createMutedText("");
        summaryLabel.setStyle("-fx-text-fill: #2f3553;");

        VBox jobList = new VBox(JOB_ROW_GAP);
        jobList.setFillWidth(true);
        ScrollPane jobScroll = createJobScrollPane(jobList);

        VBox panel = new VBox(14);
        panel.setPadding(new Insets(22));
        panel.setMaxWidth(Double.MAX_VALUE);
        panel.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(24), Insets.EMPTY)));
        panel.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(24),
            new BorderWidths(1.6)
        )));

        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = UiTheme.createSectionTitle("Global job management");
        title.setStyle("-fx-text-fill: #2f3553;");
        Label subtitle = UiTheme.createMutedText("View all postings, filter by status, check missing data, and close invalid jobs.");
        subtitle.setStyle("-fx-text-fill: #2f3553;");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(titleBox, spacer, statusFilter);
        panel.getChildren().addAll(header, summaryLabel, jobScroll);

        AdminJobManagementPanel jobManagementPanel = new AdminJobManagementPanel(
            context,
            panel,
            statusFilter,
            jobList,
            jobScroll,
            summaryLabel
        );
        statusFilter.valueProperty().addListener((obs, oldValue, newValue) -> jobManagementPanel.refresh());
        jobManagementPanel.refresh();
        return jobManagementPanel;
    }

    VBox container() {
        return container;
    }

    private void refresh() {
        List<JobPosting> jobs = filteredJobs();
        jobList.getChildren().clear();

        long openCount = jobs.stream().filter(job -> job.status() == JobStatus.OPEN).count();
        long issueCount = jobs.stream().filter(job -> !integrityIssues(job).isEmpty()).count();
        int visibleRows = Math.max(1, Math.min(MAX_VISIBLE_JOB_ROWS, jobs.size()));
        jobScroll.setPrefViewportHeight(visibleRows * JOB_ROW_HEIGHT + Math.max(0, visibleRows - 1) * JOB_ROW_GAP);
        summaryLabel.setText(
            "Showing "
                + jobs.size()
                + " jobs. Open: "
                + openCount
                + ", Closed: "
                + (jobs.size() - openCount)
                + ", With data issues: "
                + issueCount
                + "."
        );

        if (jobs.isEmpty()) {
            jobList.getChildren().add(UiTheme.createWhiteCard("No jobs", "No jobs match the selected status filter."));
            return;
        }

        for (JobPosting job : jobs) {
            jobList.getChildren().add(createJobRow(job));
        }
    }

    private List<JobPosting> filteredJobs() {
        String selectedStatus = statusFilter.getValue();
        return context.services().jobRepository().findAll().stream()
            .filter(job -> selectedStatus == null
                || ALL_STATUSES.equals(selectedStatus)
                || job.status().name().equals(selectedStatus))
            .sorted(Comparator.comparing(JobPosting::status).thenComparing(JobPosting::jobId))
            .toList();
    }

    private HBox createJobRow(JobPosting job) {
        long applicationCount = context.services().applicationRepository().findAll().stream()
            .filter(application -> application.jobId().equals(job.jobId()))
            .count();
        List<String> issues = integrityIssues(job);
        boolean isOpen = job.status() == JobStatus.OPEN;

        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMinHeight(JOB_ROW_HEIGHT);
        row.setPadding(new Insets(12, 18, 12, 18));
        row.setMaxWidth(Double.MAX_VALUE);
        row.setBackground(new Background(new BackgroundFill(Color.web("#fffafd"), new CornerRadii(18), Insets.EMPTY)));
        row.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(18),
            new BorderWidths(1.2)
        )));

        VBox identity = new VBox(5);
        identity.setMinWidth(300);
        identity.setPrefWidth(390);
        Label titleLabel = new Label(job.title());
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-text-fill: #4664a8;");
        Label metaLabel = UiTheme.createMutedText(job.jobId() + " | " + job.moduleOrActivity() + " | " + job.activityType());
        metaLabel.setStyle("-fx-text-fill: #5c6481;");
        identity.getChildren().addAll(titleLabel, metaLabel);

        HBox chips = new HBox(8);
        chips.setAlignment(Pos.CENTER_LEFT);
        chips.getChildren().addAll(
            createStatusChip(job.status()),
            createChip("Applications: " + applicationCount, "#f7f3ff", "#5d4c86", "#d7c5ff"),
            issues.isEmpty()
                ? createChip("Data complete", "#edf7ff", "#2f5c9f", "#b8d5ff")
                : createChip("Issues: " + String.join(", ", issues), "#fff6e5", "#9a6500", "#ffd58a")
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeButton = UiTheme.createOutlineButton(isOpen ? "Close job" : "Closed", 130, 42);
        if (isOpen) {
            closeButton.setStyle(
                "-fx-background-color: #fff2f2;" +
                    "-fx-text-fill: #b00020;" +
                    "-fx-background-radius: 22;" +
                    "-fx-border-color: #f2a6a6;" +
                    "-fx-border-width: 2;" +
                    "-fx-border-radius: 22;" +
                    "-fx-font-weight: bold;" +
                    "-fx-cursor: hand;"
            );
            closeButton.setOnAction(event -> {
                if (!confirmClose(job)) {
                    return;
                }
                context.services().jobRepository().save(new JobPosting(
                    job.jobId(),
                    job.organiserId(),
                    job.title(),
                    job.moduleOrActivity(),
                    job.activityType(),
                    job.description(),
                    job.requiredSkills(),
                    job.weeklyHours(),
                    job.scheduleSlots(),
                    JobStatus.CLOSED
                ));
                refresh();
            });
        } else {
            closeButton.setDisable(true);
            closeButton.setOpacity(0.72);
        }

        Button aiReviewButton = UiTheme.createOutlineButton("AI review", 120, 42);
        aiReviewButton.setStyle(
            "-fx-background-color: #fff0f8;" +
                "-fx-text-fill: #2f3553;" +
                "-fx-background-radius: 22;" +
                "-fx-border-color: #f0a6e9;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 22;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;"
        );
        aiReviewButton.setOnAction(event -> AdminAiAnalysisDialog.showJobPostingReview(
            aiReviewButton.getScene() == null ? null : aiReviewButton.getScene().getWindow(),
            context,
            job
        ));

        row.getChildren().addAll(identity, chips, spacer, aiReviewButton, closeButton);
        return row;
    }

    private boolean confirmClose(JobPosting job) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm close job");
        alert.setHeaderText("Close this job posting?");
        alert.setContentText(
            "Job: " + job.title() + "\n\n" +
                "Applicants will no longer be able to apply after this job is closed."
        );
        Optional<ButtonType> choice = alert.showAndWait();
        return choice.isPresent() && choice.get() == ButtonType.OK;
    }

    private List<String> integrityIssues(JobPosting job) {
        return List.of(
                job.scheduleSlots().isEmpty() ? "missing time" : "",
                job.requiredSkills().isEmpty() ? "missing skills" : "",
                job.description().isBlank() ? "missing description" : ""
            ).stream()
            .filter(issue -> !issue.isBlank())
            .toList();
    }

    private static ComboBox<String> createStatusFilter() {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(ALL_STATUSES, JobStatus.OPEN.name(), JobStatus.CLOSED.name());
        comboBox.getSelectionModel().selectFirst();
        comboBox.setPrefWidth(180);
        comboBox.setPrefHeight(42);
        comboBox.setStyle(
            "-fx-background-color: white;" +
                "-fx-background-radius: 22;" +
                "-fx-border-color: #f1c7da;" +
                "-fx-border-radius: 22;" +
                "-fx-border-width: 1.5;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 14px;"
        );
        return comboBox;
    }

    private static ScrollPane createJobScrollPane(VBox jobList) {
        ScrollPane scrollPane = new ScrollPane(jobList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle(
            "-fx-background-color: transparent;" +
                "-fx-background: transparent;" +
                "-fx-border-color: transparent;"
        );
        return scrollPane;
    }

    private static Label createStatusChip(JobStatus status) {
        boolean open = status == JobStatus.OPEN;
        return createChip(
            open ? "Open" : "Closed",
            open ? "#e8f5e9" : "#f3f3f3",
            open ? "#2e7d32" : "#5c6481",
            open ? "#9fd6a6" : "#d8d8d8"
        );
    }

    private static Label createChip(String text, String background, String textColor, String borderColor) {
        Label chip = new Label(text);
        chip.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        chip.setTextFill(Color.web(textColor));
        chip.setPadding(new Insets(7, 12, 7, 12));
        chip.setStyle(
            "-fx-background-color: " + background + ";" +
                "-fx-text-fill: " + textColor + ";" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-radius: 16;" +
                "-fx-border-width: 1.2;"
        );
        return chip;
    }
}
