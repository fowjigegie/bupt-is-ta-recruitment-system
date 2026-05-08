package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.application.ApplicationStatusPresenter;
import com.bupt.tarecruitment.common.text.DisplayFormats;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.mo.JobQualityIssue;
import com.bupt.tarecruitment.mo.JobQualityReport;
import com.bupt.tarecruitment.mo.MoJobAnalyticsRow;
import com.bupt.tarecruitment.mo.MoJobAnalyticsSummary;
import com.bupt.tarecruitment.mo.RankedApplicantCandidate;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;

/**
 * MO insights page: applicant ranking, job quality checks, and MO job analytics.
 */
public class MoInsightsPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.MO_INSIGHTS, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        List<JobPosting> ownedJobs = context.services().jobRepository().findAll().stream()
            .filter(job -> job.organiserId().equals(context.session().userId()))
            .sorted(Comparator.comparing(JobPosting::jobId))
            .toList();

        VBox content = new VBox(20);
        content.setPadding(new Insets(24, 40, 30, 40));

        Label heading = UiTheme.createPageHeading("MO insights");
        Label subtitle = UiTheme.createMutedText(
            "Use applicant ranking, job quality checks, and job analytics to improve hiring decisions."
        );

        content.getChildren().addAll(heading, subtitle);

        if (ownedJobs.isEmpty()) {
            content.getChildren().add(UiTheme.createWhiteCard(
                "No jobs yet",
                "Create a vacancy first, then return to MO Insights to rank applicants and audit job quality."
            ));
        } else {
            MoJobAnalyticsSummary summary = context.services().moJobAnalyticsService()
                .summarizeForOrganiser(context.session().userId());

            ComboBox<JobPosting> jobBox = createJobBox(ownedJobs);
            jobBox.setValue(ownedJobs.get(0));

            VBox selectedJobPanel = new VBox(18);
            selectedJobPanel.setMaxWidth(Double.MAX_VALUE);

            Runnable refreshSelectedJob = () -> renderSelectedJob(context, jobBox.getValue(), selectedJobPanel);
            jobBox.valueProperty().addListener((obs, oldJob, newJob) -> refreshSelectedJob.run());

            content.getChildren().addAll(
                createAnalyticsSummaryCards(summary),
                createSectionCard("Choose job", "Select one of your jobs to view ranking and quality details.", jobBox),
                selectedJobPanel,
                createJobPerformanceSection(summary)
            );

            refreshSelectedJob.run();
        }

        ScrollPane pageScroll = new ScrollPane(content);
        pageScroll.setFitToWidth(true);
        pageScroll.setPannable(true);
        pageScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        BorderPane root = UiTheme.createPage(
            "MO Insights",
            UiTheme.createMoSidebar(nav, PageId.MO_INSIGHTS),
            pageScroll,
            nav,
            context
        );
        return UiTheme.createScene(root);
    }

    private static ComboBox<JobPosting> createJobBox(List<JobPosting> ownedJobs) {
        ComboBox<JobPosting> jobBox = new ComboBox<>();
        jobBox.getItems().addAll(ownedJobs);
        jobBox.setMaxWidth(Double.MAX_VALUE);
        jobBox.setPrefHeight(42);
        jobBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(JobPosting item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.jobId() + " | " + item.title());
            }
        });
        jobBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(JobPosting item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.jobId() + " | " + item.title());
            }
        });
        jobBox.setStyle(
            "-fx-background-color: white;" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: #f0a6e9;" +
                "-fx-border-radius: 18;" +
                "-fx-border-width: 2;" +
                "-fx-font-size: 15px;"
        );
        return jobBox;
    }

    private static void renderSelectedJob(UiAppContext context, JobPosting job, VBox selectedJobPanel) {
        selectedJobPanel.getChildren().clear();
        if (job == null) {
            return;
        }

        JobQualityReport qualityReport = context.services().moJobQualityService().analyzeJob(job.jobId());
        List<RankedApplicantCandidate> rankedApplicants = context.services().moApplicantRankingService()
            .rankApplicantsForJob(job.jobId());

        selectedJobPanel.getChildren().addAll(
            createQualitySection(qualityReport),
            createRankingSection(rankedApplicants)
        );
    }

    private static HBox createAnalyticsSummaryCards(MoJobAnalyticsSummary summary) {
        HBox row = new HBox(16,
            createMetricCard("Owned jobs", Long.toString(summary.totalJobs()), "All jobs posted by this MO"),
            createMetricCard("Open jobs", Long.toString(summary.openJobs()), "Currently accepting applications"),
            createMetricCard("Applications", Long.toString(summary.totalApplications()), "Applications across owned jobs"),
            createMetricCard("Accepted", Long.toString(summary.acceptedApplications()), "Accepted application records"),
            createMetricCard("Avg/job", DisplayFormats.formatDecimal(summary.averageApplicationsPerJob()), "Average applications per job")
        );
        row.setMaxWidth(Double.MAX_VALUE);
        row.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));
        return row;
    }

    private static VBox createQualitySection(JobQualityReport report) {
        VBox body = new VBox(12);

        HBox header = new HBox(12,
            createScoreBadge(report.qualityScore()),
            createStatusBadge(report.readyForPublishing() ? "Ready" : "Needs fixes", report.readyForPublishing() ? "#2e7d32" : "#e74c3c")
        );
        header.setAlignment(Pos.CENTER_LEFT);

        body.getChildren().add(header);

        if (report.issues().isEmpty()) {
            body.getChildren().add(createSoftInfoRow("No quality issues detected. This job posting is clear and ready."));
        } else {
            for (JobQualityIssue issue : report.issues()) {
                body.getChildren().add(createIssueRow(issue));
            }
        }

        return createSectionCard(
            "Job quality assistant | " + report.jobId() + " " + report.title(),
            "Checks schedule, weekly hours, required skills, description quality, and current demand.",
            body
        );
    }

    private static VBox createRankingSection(List<RankedApplicantCandidate> rankedApplicants) {
        VBox list = new VBox(10);
        if (rankedApplicants.isEmpty()) {
            list.getChildren().add(createSoftInfoRow("No applications for this job yet."));
        } else {
            int rank = 1;
            for (RankedApplicantCandidate candidate : rankedApplicants) {
                list.getChildren().add(createCandidateRow(rank, candidate));
                rank++;
            }
        }

        return createSectionCard(
            "Applicant ranking",
            "Ranks applicants by skill match, availability, CV presence, profile completeness, and review status.",
            list
        );
    }

    private static VBox createJobPerformanceSection(MoJobAnalyticsSummary summary) {
        VBox list = new VBox(10);
        if (summary.jobRows().isEmpty()) {
            list.getChildren().add(createSoftInfoRow("No job performance data yet."));
        } else {
            for (MoJobAnalyticsRow row : summary.jobRows()) {
                list.getChildren().add(createJobAnalyticsRow(row));
            }
        }

        return createSectionCard(
            "MO job analytics",
            "Compares your jobs by applications, accepted count, acceptance rate, weekly hours, and quality score.",
            list
        );
    }

    private static HBox createCandidateRow(int rank, RankedApplicantCandidate candidate) {
        Label rankLabel = createCircleLabel(Integer.toString(rank), scoreColor(candidate.rankScore()));

        VBox main = new VBox(5);
        Label name = new Label(candidate.applicantName() + " (" + candidate.applicantUserId() + ")");
        name.setFont(Font.font("Arial", FontWeight.BOLD, 17));
        name.setStyle("-fx-text-fill: #4664a8;");

        Label meta = new Label(
            "Score: " + candidate.rankScore()
                + " | Skill match: " + candidate.skillMatchPercent() + "%"
                + " | Availability: " + (candidate.availabilityFit() ? "Fit" : "Risk")
                + " | CV: " + (candidate.hasCv() ? "Yes" : "Missing")
        );
        meta.setStyle("-fx-text-fill: #5c6481; -fx-font-size: 14px;");
        meta.setWrapText(true);

        Label reasons = new Label("Reasons: " + String.join(", ", candidate.reasons()));
        reasons.setStyle("-fx-text-fill: #8b7fa0; -fx-font-size: 13px;");
        reasons.setWrapText(true);

        main.getChildren().addAll(name, meta, reasons);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label status = createStatusBadge(ApplicationStatusPresenter.toDisplayText(candidate.status()), "#4664a8");
        HBox row = new HBox(12, rankLabel, main, spacer, status);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14));
        row.setBackground(new Background(new BackgroundFill(Color.web("#fff8fb"), new CornerRadii(18), Insets.EMPTY)));
        row.setBorder(new Border(new BorderStroke(Color.web("#f4d9e6"), BorderStrokeStyle.SOLID, new CornerRadii(18), new BorderWidths(1.4))));
        return row;
    }

    private static HBox createJobAnalyticsRow(MoJobAnalyticsRow rowData) {
        Label job = new Label(rowData.jobId() + " | " + rowData.title());
        job.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        job.setStyle("-fx-text-fill: #4664a8;");
        job.setWrapText(true);

        Label stats = new Label(
            "Applications: " + rowData.applicationCount()
                + " | Accepted: " + rowData.acceptedCount()
                + " | Acceptance: " + rowData.acceptanceRatePercent() + "%"
                + " | Hours: " + DisplayFormats.formatDecimal(rowData.weeklyHours())
                + " | Quality: " + rowData.qualityScore()
        );
        stats.setStyle("-fx-text-fill: #5c6481;");
        stats.setWrapText(true);

        VBox text = new VBox(4, job, stats);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label status = createStatusBadge(rowData.status().name(), rowData.status().name().equals("OPEN") ? "#2e7d32" : "#8b7fa0");
        HBox row = new HBox(12, text, spacer, status);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12));
        row.setStyle(
            "-fx-background-color: #fff8fb;" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: #f4d9e6;" +
                "-fx-border-radius: 16;"
        );
        return row;
    }

    private static VBox createSectionCard(String title, String subtitle, javafx.scene.Node body) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(22));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(24), Insets.EMPTY)));
        card.setBorder(new Border(new BorderStroke(Color.web("#f4d9e6"), BorderStrokeStyle.SOLID, new CornerRadii(24), new BorderWidths(1.5))));

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        titleLabel.setStyle("-fx-text-fill: #4664a8;");
        titleLabel.setWrapText(true);

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setFont(Font.font("Arial", 14));
        subtitleLabel.setStyle("-fx-text-fill: #8b7fa0;");
        subtitleLabel.setWrapText(true);

        card.getChildren().addAll(titleLabel, subtitleLabel, body);
        return card;
    }

    private static VBox createMetricCard(String title, String value, String subtitle) {
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        titleLabel.setStyle("-fx-text-fill: #5c6481;");

        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 30));
        valueLabel.setStyle("-fx-text-fill: #4664a8;");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setFont(Font.font("Arial", 12));
        subtitleLabel.setStyle("-fx-text-fill: #8b7fa0;");
        subtitleLabel.setWrapText(true);

        VBox card = new VBox(6, titleLabel, valueLabel, subtitleLabel);
        card.setPadding(new Insets(16));
        card.setMinHeight(112);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(22), Insets.EMPTY)));
        card.setBorder(new Border(new BorderStroke(Color.web("#f4d9e6"), BorderStrokeStyle.SOLID, new CornerRadii(22), new BorderWidths(1.4))));
        return card;
    }

    private static Label createScoreBadge(int score) {
        return createStatusBadge("Quality score: " + score, scoreColor(score));
    }

    private static Label createCircleLabel(String text, String color) {
        Label label = new Label(text);
        label.setAlignment(Pos.CENTER);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        label.setStyle(
            "-fx-background-color: " + color + ";" +
                "-fx-background-radius: 22;" +
                "-fx-text-fill: white;"
        );
        label.setPrefSize(44, 44);
        return label;
    }

    private static Label createStatusBadge(String text, String color) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        label.setStyle(
            "-fx-background-color: " + color + ";" +
                "-fx-background-radius: 14;" +
                "-fx-text-fill: white;" +
                "-fx-padding: 6 12 6 12;"
        );
        return label;
    }

    private static HBox createIssueRow(JobQualityIssue issue) {
        String color = switch (issue.severity()) {
            case "CRITICAL" -> "#e74c3c";
            case "WARNING" -> "#f39c12";
            default -> "#4664a8";
        };
        Label severity = createStatusBadge(issue.severity(), color);
        Label message = new Label(issue.code() + " | " + issue.message());
        message.setStyle("-fx-text-fill: #3f4370; -fx-font-size: 14px;");
        message.setWrapText(true);
        HBox row = new HBox(10, severity, message);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-background-color: #fff8fb; -fx-background-radius: 14;");
        return row;
    }

    private static HBox createSoftInfoRow(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setFont(Font.font("Arial", 14));
        label.setStyle("-fx-text-fill: #5c6481;");
        HBox row = new HBox(label);
        row.setPadding(new Insets(12));
        row.setStyle("-fx-background-color: #fff8fb; -fx-background-radius: 16;");
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
}
