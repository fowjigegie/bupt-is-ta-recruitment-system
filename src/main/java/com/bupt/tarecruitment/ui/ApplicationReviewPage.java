package com.bupt.tarecruitment.ui;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bupt.tarecruitment.application.ApplicationStatusPresenter;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.mo.RankedApplicantCandidate;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class ApplicationReviewPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.APPLICATION_REVIEW, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        List<JobPosting> ownedJobs = loadOwnedJobs(context);

        VBox content = new VBox(18);
        content.setPadding(new Insets(20, 40, 28, 40));

        Label heading = UiTheme.createPageHeading("Application review");
        heading.setStyle("-fx-text-fill: #4664a8;");
        content.getChildren().add(heading);

        if (ownedJobs.isEmpty()) {
            content.getChildren().add(UiTheme.createWhiteCard(
                "No jobs",
                "You don't have any jobs yet. Go to Post Vacancies to create one."
            ));
        } else {
            JobPosting initialJob = ownedJobs.stream()
                .filter(job -> job.jobId().equals(context.selectedJobId()))
                .findFirst()
                .orElse(ownedJobs.getFirst());

            ApplicationReviewWorkspace workspace = ApplicationReviewWorkspace.create(ownedJobs, initialJob);

            workspace.applicantListBox().setPrefWidth(500);
            workspace.applicantListBox().setMinWidth(460);
            workspace.detailContent().setPrefWidth(520);

            Runnable[] refreshListRef = new Runnable[1];
            refreshListRef[0] = () -> {
                workspace.applicantListBox().getChildren().clear();

                JobPosting selectedJob = workspace.jobBox().getValue();
                if (selectedJob == null) {
                    return;
                }

                List<RankedApplicantCandidate> rankedCandidates = context.services().moApplicantRankingService()
                    .rankApplicantsForJob(selectedJob.jobId());

                Map<String, RankedApplicantCandidate> rankingByApplicationId = rankedCandidates.stream()
                    .collect(Collectors.toMap(
                        RankedApplicantCandidate::applicationId,
                        Function.identity(),
                        (left, right) -> left
                    ));

                List<JobApplication> applications = loadApplicationsForJob(context, selectedJob.jobId()).stream()
                    .sorted(Comparator
                        .comparingInt((JobApplication application) -> rankingByApplicationId
                            .getOrDefault(application.applicationId(), fallbackCandidate(application))
                            .rankScore())
                        .reversed()
                        .thenComparing(JobApplication::applicationId))
                    .toList();

                if (applications.isEmpty()) {
                    workspace.applicantListBox().getChildren().add(UiTheme.createWhiteCard(
                        "No applications",
                        "There are no applications for this job yet."
                    ));
                    workspace.detailTitle().setText("Select an applicant");
                    workspace.detailContent().getChildren().setAll(UiTheme.createMutedText("No CV details to display."));
                    workspace.setSelectedApplication(null);
                    return;
                }

                if (workspace.selectedApplication() == null
                    || applications.stream().noneMatch(app -> app.applicationId().equals(workspace.selectedApplication().applicationId()))) {
                    workspace.setSelectedApplication(applications.get(0));
                }

                if (!rankedCandidates.isEmpty()) {
                    workspace.applicantListBox().getChildren().add(createRankingSummaryCard(rankedCandidates));
                }

                for (JobApplication application : applications) {
                    RankedApplicantCandidate candidate = rankingByApplicationId
                        .getOrDefault(application.applicationId(), fallbackCandidate(application));

                    workspace.applicantListBox().getChildren().add(createApplicantRow(
                        context,
                        application,
                        candidate,
                        workspace.selectedApplication() != null
                            && application.applicationId().equals(workspace.selectedApplication().applicationId()),
                        selected -> {
                            workspace.setSelectedApplication(selected);
                            ApplicationReviewDetailsPanel.renderSelectedDetail(
                                context,
                                selected,
                                workspace.detailTitle(),
                                workspace.detailContent(),
                                workspace.actionStatus(),
                                nav,
                                refreshListRef[0]
                            );
                        }
                    ));
                }

                if (workspace.selectedApplication() != null) {
                    ApplicationReviewDetailsPanel.renderSelectedDetail(
                        context,
                        workspace.selectedApplication(),
                        workspace.detailTitle(),
                        workspace.detailContent(),
                        workspace.actionStatus(),
                        nav,
                        refreshListRef[0]
                    );
                }
            };

            workspace.jobBox().valueProperty().addListener((obs, oldValue, newValue) -> refreshListRef[0].run());
            refreshListRef[0].run();

            content.getChildren().addAll(workspace.jobBox(), workspace.contentRow());
        }

        ScrollPane pageScroll = new ScrollPane(content);
        pageScroll.setFitToWidth(true);
        pageScroll.setPannable(true);
        pageScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pageScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        BorderPane root = UiTheme.createPage(
            "Application Review",
            UiTheme.createMoSidebar(nav, PageId.APPLICATION_REVIEW),
            pageScroll,
            nav,
            context
        );

        return UiTheme.createScene(root);
    }

    private static List<JobPosting> loadOwnedJobs(UiAppContext context) {
        return context.services().jobRepository().findAll().stream()
            .filter(job -> job.organiserId().equals(context.session().userId()))
            .sorted(Comparator.comparing(JobPosting::jobId))
            .toList();
    }

    private static List<JobApplication> loadApplicationsForJob(UiAppContext context, String jobId) {
        return context.services().applicationRepository().findAll().stream()
            .filter(application -> application.jobId().equals(jobId))
            .sorted(Comparator.comparing(JobApplication::applicationId))
            .toList();
    }

    private static HBox createApplicantRow(
        UiAppContext context,
        JobApplication application,
        RankedApplicantCandidate candidate,
        boolean selected,
        java.util.function.Consumer<JobApplication> onSelect
    ) {
        var profileOpt = context.services().profileRepository().findByUserId(application.applicantUserId());
        String applicantName = profileOpt.map(p -> p.fullName()).orElse(application.applicantUserId());

        Label nameLabel = new Label(applicantName);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        nameLabel.setTextFill(Color.web("#4664a8"));
        nameLabel.setPrefWidth(170);
        nameLabel.setMinWidth(150);

        Label scoreLabel = new Label("Rank " + candidate.rankScore());
        scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        scoreLabel.setTextFill(Color.WHITE);
        scoreLabel.setPadding(new Insets(4, 10, 4, 10));
        scoreLabel.setPrefWidth(85);
        scoreLabel.setMinWidth(80);
        scoreLabel.setStyle(
            "-fx-background-color: " + rankingScoreColor(candidate.rankScore()) + ";"
                + " -fx-background-radius: 10;"
        );

        Label statusLabel = new Label(ApplicationStatusPresenter.toDisplayText(application.status()));
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        statusLabel.setTextFill(Color.web("#4664a8"));
        statusLabel.setPadding(new Insets(4, 10, 4, 10));
        statusLabel.setPrefWidth(120);
        statusLabel.setMinWidth(115);
        statusLabel.setStyle(
            "-fx-background-color: " + ApplicationReviewDetailsPanel.statusColor(application.status()) + ";"
                + " -fx-background-radius: 10;"
        );

        Button detailsButton = UiTheme.createSoftButton("View", 72, 42);
        detailsButton.setMinWidth(72);
        detailsButton.setOnAction(event -> onSelect.accept(application));

        HBox row = new HBox(10, nameLabel, scoreLabel, statusLabel, detailsButton);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 10, 14, 10));
        row.setMaxWidth(Double.MAX_VALUE);
        row.setStyle(
            (selected ? "-fx-background-color: #fdf4fb;" : "-fx-background-color: transparent;")
                + "-fx-border-color: #f4d9e6; -fx-border-width: 0 0 2 0;"
        );

        return row;
    }

    private static VBox createRankingSummaryCard(List<RankedApplicantCandidate> candidates) {
        RankedApplicantCandidate top = candidates.get(0);

        Label title = new Label("AI-style applicant ranking");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#4664a8"));

        Label body = new Label(
            "Top candidate: " + top.applicantName()
                + " | Score: " + top.rankScore()
                + " | Availability: " + (top.availabilityFit() ? "Fit" : "Risk")
        );
        body.setWrapText(true);
        body.setTextFill(Color.web("#5c6481"));

        VBox card = new VBox(6, title, body);
        card.setPadding(new Insets(12, 14, 12, 14));
        card.setStyle(
            "-fx-background-color: #fff8fb;"
                + "-fx-background-radius: 16;"
                + "-fx-border-color: #f4d9e6;"
                + "-fx-border-radius: 16;"
        );

        return card;
    }

    private static RankedApplicantCandidate fallbackCandidate(JobApplication application) {
        return new RankedApplicantCandidate(
            application.applicationId(),
            application.jobId(),
            application.applicantUserId(),
            application.applicantUserId(),
            application.status(),
            0,
            0,
            false,
            false,
            List.of(),
            List.of(),
            List.of("Ranking data unavailable")
        );
    }

    private static String rankingScoreColor(int score) {
        if (score >= 80) {
            return "#2e7d32";
        }
        if (score >= 55) {
            return "#f39c12";
        }
        return "#e74c3c";
    }

    public static void main(String[] args) {
        launch(args);
    }
}
