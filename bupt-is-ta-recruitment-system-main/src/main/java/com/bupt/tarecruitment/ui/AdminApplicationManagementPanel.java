package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.application.ApplicationStatus;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.applicant.ApplicantCv;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobStatus;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Admin-facing global application list with status filtering and consistency checks.
 */
final class AdminApplicationManagementPanel {
    private static final String ALL_STATUSES = "All statuses";
    private static final int MAX_VISIBLE_APPLICATION_ROWS = 8;
    private static final double APPLICATION_ROW_HEIGHT = 118;
    private static final double APPLICATION_ROW_GAP = 10;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UiAppContext context;
    private final VBox container;
    private final ComboBox<String> statusFilter;
    private final VBox applicationList;
    private final ScrollPane applicationScroll;
    private final Label summaryLabel;

    private AdminApplicationManagementPanel(
        UiAppContext context,
        VBox container,
        ComboBox<String> statusFilter,
        VBox applicationList,
        ScrollPane applicationScroll,
        Label summaryLabel
    ) {
        this.context = context;
        this.container = container;
        this.statusFilter = statusFilter;
        this.applicationList = applicationList;
        this.applicationScroll = applicationScroll;
        this.summaryLabel = summaryLabel;
    }

    static AdminApplicationManagementPanel create(UiAppContext context) {
        ComboBox<String> statusFilter = createStatusFilter();
        Label summaryLabel = UiTheme.createMutedText("");
        summaryLabel.setStyle("-fx-text-fill: #2f3553;");

        VBox applicationList = new VBox(APPLICATION_ROW_GAP);
        applicationList.setFillWidth(true);
        ScrollPane applicationScroll = createApplicationScrollPane(applicationList);

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
        Label title = UiTheme.createSectionTitle("Global application management");
        title.setStyle("-fx-text-fill: #2f3553;");
        Label subtitle = UiTheme.createMutedText("View all applications, filter by status, validate CV references, and spot inconsistent records.");
        subtitle.setStyle("-fx-text-fill: #2f3553;");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(titleBox, spacer, statusFilter);
        panel.getChildren().addAll(header, summaryLabel, applicationScroll);

        AdminApplicationManagementPanel applicationManagementPanel = new AdminApplicationManagementPanel(
            context,
            panel,
            statusFilter,
            applicationList,
            applicationScroll,
            summaryLabel
        );
        statusFilter.valueProperty().addListener((obs, oldValue, newValue) -> applicationManagementPanel.refresh());
        applicationManagementPanel.refresh();
        return applicationManagementPanel;
    }

    VBox container() {
        return container;
    }

    private void refresh() {
        List<JobApplication> applications = filteredApplications();
        applicationList.getChildren().clear();

        long acceptedCount = applications.stream()
            .filter(application -> application.status() == ApplicationStatus.ACCEPTED)
            .count();
        long issueCount = applications.stream()
            .filter(application -> !integrityIssues(application).isEmpty())
            .count();
        int visibleRows = Math.max(1, Math.min(MAX_VISIBLE_APPLICATION_ROWS, applications.size()));
        applicationScroll.setPrefViewportHeight(
            visibleRows * APPLICATION_ROW_HEIGHT + Math.max(0, visibleRows - 1) * APPLICATION_ROW_GAP
        );
        summaryLabel.setText(
            "Showing "
                + applications.size()
                + " applications. Accepted: "
                + acceptedCount
                + ", With issues: "
                + issueCount
                + "."
        );

        if (applications.isEmpty()) {
            applicationList.getChildren().add(UiTheme.createWhiteCard(
                "No applications",
                "No applications match the selected status filter."
            ));
            return;
        }

        for (JobApplication application : applications) {
            applicationList.getChildren().add(createApplicationRow(application));
        }
    }

    private List<JobApplication> filteredApplications() {
        String selectedStatus = statusFilter.getValue();
        return context.services().applicationRepository().findAll().stream()
            .filter(application -> selectedStatus == null
                || ALL_STATUSES.equals(selectedStatus)
                || application.status().name().equals(selectedStatus))
            .sorted(Comparator.comparing(JobApplication::submittedAt).reversed())
            .toList();
    }

    private VBox createApplicationRow(JobApplication application) {
        Optional<JobPosting> job = findJob(application.jobId());
        Optional<ApplicantCv> cv = context.services().cvRepository().findByCvId(application.cvId());
        boolean cvValid = cv.map(applicantCv -> applicantCv.ownerUserId().equals(application.applicantUserId())).orElse(false);
        long acceptedJobs = acceptedJobsForApplicant(application.applicantUserId());
        List<String> issues = integrityIssues(application);

        VBox row = new VBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMinHeight(APPLICATION_ROW_HEIGHT);
        row.setPadding(new Insets(14, 18, 14, 18));
        row.setMaxWidth(Double.MAX_VALUE);
        row.setBackground(new Background(new BackgroundFill(Color.web("#fffafd"), new CornerRadii(18), Insets.EMPTY)));
        row.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(18),
            new BorderWidths(1.2)
        )));

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label(job.map(JobPosting::title).orElse("Unknown job"));
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-text-fill: #4664a8;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Label jobStatusLabel = createMutedLine(
            job.map(jobPosting -> "Job: " + jobPosting.status().name()).orElse("Job: missing")
        );
        jobStatusLabel.setMinWidth(96);

        topRow.getChildren().addAll(titleLabel, createApplicationStatusChip(application.status()), jobStatusLabel);

        HBox bottomRow = new HBox(10);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        bottomRow.setMaxWidth(Double.MAX_VALUE);

        Label applicantLabel = UiTheme.createMutedText(
            context.formatUserLabel(application.applicantUserId())
                + " | "
                + application.applicationId()
                + " | "
                + DATE_TIME_FORMATTER.format(application.submittedAt())
        );
        applicantLabel.setMinWidth(330);
        applicantLabel.setStyle("-fx-text-fill: #5c6481;");

        bottomRow.getChildren().addAll(
            applicantLabel,
            createChip(
                cvValid ? "CV valid: " + application.cvId() : "CV issue: " + application.cvId(),
                cvValid ? "#edf7ff" : "#fff6e5",
                cvValid ? "#2f5c9f" : "#9a6500",
                cvValid ? "#b8d5ff" : "#ffd58a"
            ),
            createChip("Accepted jobs: " + acceptedJobs, "#f7f3ff", "#5d4c86", "#d7c5ff")
        );
        if (!issues.isEmpty()) {
            bottomRow.getChildren().add(createChip("Issues: " + String.join(", ", issues), "#fff2f2", "#b00020", "#f2a6a6"));
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button detailsButton = UiTheme.createOutlineButton("View details", 130, 38);
        detailsButton.setMinWidth(130);
        detailsButton.setOnAction(event -> showDetails(application));
        bottomRow.getChildren().addAll(spacer, detailsButton);

        row.getChildren().addAll(topRow, bottomRow);
        return row;
    }

    private void showDetails(JobApplication application) {
        Optional<JobPosting> job = findJob(application.jobId());
        Optional<ApplicantCv> cv = context.services().cvRepository().findByCvId(application.cvId());
        boolean cvValid = cv.map(applicantCv -> applicantCv.ownerUserId().equals(application.applicantUserId())).orElse(false);
        long acceptedJobs = acceptedJobsForApplicant(application.applicantUserId());
        List<String> issues = integrityIssues(application);

        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #fffaf3;");

        content.getChildren().add(createDetailsHeader(application, job, issues));
        content.getChildren().add(createMetricRow(
            createMetric("Status", application.status().name()),
            createMetric("CV", cvValid ? "Valid" : "Issue"),
            createMetric("Accepted jobs", Long.toString(acceptedJobs)),
            createMetric("Issues", Integer.toString(issues.size()))
        ));

        content.getChildren().add(createDetailCard(
            "Application",
            createKeyValue("Application ID", application.applicationId()),
            createKeyValue("Submitted at", DATE_TIME_FORMATTER.format(application.submittedAt())),
            createKeyValue("Reviewer note", application.reviewerNote().isBlank() ? "(none)" : application.reviewerNote())
        ));

        content.getChildren().add(createDetailCard(
            "Applicant",
            createKeyValue("Applicant", context.formatUserLabel(application.applicantUserId())),
            createKeyValue("Accepted jobs", Long.toString(acceptedJobs))
        ));

        content.getChildren().add(createDetailCard(
            "Job",
            createKeyValue("Title", job.map(JobPosting::title).orElse("Unknown job")),
            createKeyValue("Job ID", application.jobId()),
            createKeyValue("Status", job.map(jobPosting -> jobPosting.status().name()).orElse("Missing")),
            createKeyValue("Module/activity", job.map(JobPosting::moduleOrActivity).orElse("(missing)")),
            createKeyValue("Required skills", job.map(jobPosting -> joinOrEmpty(jobPosting.requiredSkills())).orElse("(missing)"))
        ));

        content.getChildren().add(createDetailCard(
            "CV reference",
            createKeyValue("CV ID", application.cvId()),
            createKeyValue("Status", cvValid ? "Valid and owned by applicant" : "Missing or owned by another user"),
            createKeyValue("Title", cv.map(ApplicantCv::title).orElse("(missing)")),
            createKeyValue("File", cv.map(ApplicantCv::fileName).orElse("(missing)"))
        ));

        content.getChildren().add(createIntegrityCard(
            "Integrity check",
            issues.isEmpty(),
            createDetailLine(issues.isEmpty() ? "No issues detected." : String.join(", ", issues))
        ));

        Button closeButton = UiTheme.createOutlineButton("Close", 120, 40);
        HBox footer = new HBox(closeButton);
        footer.setAlignment(Pos.CENTER_RIGHT);
        content.getChildren().add(footer);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        Stage dialog = new Stage();
        dialog.setTitle("Application details - " + application.applicationId());
        dialog.initModality(Modality.APPLICATION_MODAL);
        closeButton.setOnAction(event -> dialog.close());
        dialog.setScene(new Scene(scrollPane, 820, 620));
        dialog.showAndWait();
    }

    private List<String> integrityIssues(JobApplication application) {
        Optional<JobPosting> job = findJob(application.jobId());
        Optional<ApplicantCv> cv = context.services().cvRepository().findByCvId(application.cvId());
        boolean cvValid = cv.map(applicantCv -> applicantCv.ownerUserId().equals(application.applicantUserId())).orElse(false);
        return List.of(
                job.isEmpty() ? "missing job" : "",
                cvValid ? "" : "invalid CV",
                job.filter(jobPosting -> jobPosting.status() == JobStatus.CLOSED
                    && application.status() == ApplicationStatus.SUBMITTED).isPresent()
                    ? "submitted on closed job"
                    : ""
            ).stream()
            .filter(issue -> !issue.isBlank())
            .toList();
    }

    private long acceptedJobsForApplicant(String applicantUserId) {
        return context.services().applicationRepository().findAll().stream()
            .filter(application -> application.applicantUserId().equals(applicantUserId))
            .filter(application -> application.status() == ApplicationStatus.ACCEPTED)
            .count();
    }

    private Optional<JobPosting> findJob(String jobId) {
        return context.services().jobRepository().findByJobId(jobId);
    }

    private static ComboBox<String> createStatusFilter() {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(
            ALL_STATUSES,
            ApplicationStatus.SUBMITTED.name(),
            ApplicationStatus.SHORTLISTED.name(),
            ApplicationStatus.ACCEPTED.name(),
            ApplicationStatus.REJECTED.name(),
            ApplicationStatus.WITHDRAWN.name()
        );
        comboBox.getSelectionModel().selectFirst();
        comboBox.setPrefWidth(190);
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

    private static ScrollPane createApplicationScrollPane(VBox applicationList) {
        ScrollPane scrollPane = new ScrollPane(applicationList);
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

    private static Label createApplicationStatusChip(ApplicationStatus status) {
        return switch (status) {
            case ACCEPTED -> createChip("Accepted", "#e8f5e9", "#2e7d32", "#9fd6a6");
            case REJECTED -> createChip("Rejected", "#fff2f2", "#b00020", "#f2a6a6");
            case SHORTLISTED -> createChip("Shortlisted", "#edf7ff", "#2f5c9f", "#b8d5ff");
            case WITHDRAWN -> createChip("Withdrawn", "#f3f3f3", "#5c6481", "#d8d8d8");
            case SUBMITTED -> createChip("Submitted", "#fff6e5", "#9a6500", "#ffd58a");
        };
    }

    private static Label createMutedLine(String text) {
        Label label = UiTheme.createMutedText(text);
        label.setStyle("-fx-text-fill: #5c6481;");
        return label;
    }

    private HBox createDetailsHeader(JobApplication application, Optional<JobPosting> job, List<String> issues) {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20));
        header.setMaxWidth(Double.MAX_VALUE);
        header.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(24), Insets.EMPTY)));
        header.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(24),
            new BorderWidths(1.5)
        )));

        VBox identity = new VBox(6);
        Label title = new Label(job.map(JobPosting::title).orElse("Unknown job"));
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setWrapText(true);
        title.setStyle("-fx-text-fill: #4664a8;");
        Label subtitle = createDetailLine(application.applicationId() + " | " + context.formatUserLabel(application.applicantUserId()));
        identity.getChildren().addAll(title, subtitle);
        HBox.setHgrow(identity, Priority.ALWAYS);

        HBox chips = new HBox(8);
        chips.setAlignment(Pos.CENTER_RIGHT);
        chips.getChildren().add(createApplicationStatusChip(application.status()));
        if (!issues.isEmpty()) {
            chips.getChildren().add(createChip("Needs review", "#fff2f2", "#b00020", "#f2a6a6"));
        }

        header.getChildren().addAll(identity, chips);
        return header;
    }

    private HBox createMetricRow(VBox... metrics) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        for (VBox metric : metrics) {
            HBox.setHgrow(metric, Priority.ALWAYS);
            row.getChildren().add(metric);
        }
        return row;
    }

    private VBox createMetric(String title, String value) {
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        titleLabel.setStyle("-fx-text-fill: #5c6481;");

        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        valueLabel.setStyle("-fx-text-fill: #2f3553;");

        VBox metric = new VBox(4, titleLabel, valueLabel);
        metric.setPadding(new Insets(14, 16, 14, 16));
        metric.setMaxWidth(Double.MAX_VALUE);
        metric.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(18), Insets.EMPTY)));
        metric.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(18),
            new BorderWidths(1.3)
        )));
        return metric;
    }

    private VBox createDetailCard(String title, Node... children) {
        return createTintedDetailCard(title, Color.WHITE, Color.web("#f4d9e6"), children);
    }

    private VBox createIntegrityCard(String title, boolean clear, Node... children) {
        return createTintedDetailCard(
            title,
            clear ? Color.web("#f2fbf3") : Color.web("#fff2f2"),
            clear ? Color.web("#9fd6a6") : Color.web("#f2a6a6"),
            children
        );
    }

    private VBox createTintedDetailCard(String title, Color backgroundColor, Color borderColor, Node... children) {
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        titleLabel.setStyle("-fx-text-fill: #4664a8;");

        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setBackground(new Background(new BackgroundFill(backgroundColor, new CornerRadii(22), Insets.EMPTY)));
        card.setBorder(new Border(new BorderStroke(
            borderColor,
            BorderStrokeStyle.SOLID,
            new CornerRadii(22),
            new BorderWidths(1.5)
        )));
        card.getChildren().add(titleLabel);
        card.getChildren().addAll(children);
        return card;
    }

    private HBox createKeyValue(String key, String value) {
        Label keyLabel = new Label(key);
        keyLabel.setMinWidth(130);
        keyLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        keyLabel.setStyle("-fx-text-fill: #5c6481;");

        Label valueLabel = createDetailLine(value);
        HBox.setHgrow(valueLabel, Priority.ALWAYS);

        HBox row = new HBox(10, keyLabel, valueLabel);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private static Label createDetailLine(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setFont(Font.font("Arial", 15));
        label.setStyle("-fx-text-fill: #2f3553;");
        return label;
    }

    private static String joinOrEmpty(List<String> values) {
        return values.isEmpty() ? "(none)" : String.join(", ", values);
    }

    private static Label createChip(String text, String background, String textColor, String borderColor) {
        Label chip = new Label(text);
        chip.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        chip.setTextFill(Color.web(textColor));
        chip.setMinWidth(Region.USE_PREF_SIZE);
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
