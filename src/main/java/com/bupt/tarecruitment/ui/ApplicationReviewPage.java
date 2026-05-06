package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.application.ApplicationStatus;
import com.bupt.tarecruitment.application.ApplicationStatusPresenter;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.job.JobPosting;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
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
 * 申请审核页，负责岗位切换、申请列表刷新和页面编排。
 */
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

            Runnable[] refreshListRef = new Runnable[1];
            refreshListRef[0] = () -> {
                workspace.applicantListBox().getChildren().clear();
                JobPosting selectedJob = workspace.jobBox().getValue();
                if (selectedJob == null) {
                    return;
                }

                List<JobApplication> applications = loadApplicationsForJob(context, selectedJob.jobId());
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
                    workspace.setSelectedApplication(applications.getFirst());
                }

                for (JobApplication application : applications) {
                        workspace.applicantListBox().getChildren().add(createApplicantRow(
                            context,
                            application,
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
        // Keep the same page background as other MO pages (UiTheme.pageBackground()).
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
        // Data source is repository snapshot at render time (no client cache).
        return context.services().jobRepository().findAll().stream()
            .filter(job -> job.organiserId().equals(context.session().userId()))
            .sorted(Comparator.comparing(JobPosting::jobId))
            .toList();
    }

    private static List<JobApplication> loadApplicationsForJob(UiAppContext context, String jobId) {
        // Keep deterministic order for stable UI rendering and predictable tests.
        return context.services().applicationRepository().findAll().stream()
            .filter(application -> application.jobId().equals(jobId))
            .sorted(Comparator.comparing(JobApplication::applicationId))
            .toList();
    }

    private static HBox createApplicantRow(
        UiAppContext context,
        JobApplication application,
        boolean selected,
        java.util.function.Consumer<JobApplication> onSelect
    ) {
        var profileOpt = context.services().profileRepository().findByUserId(application.applicantUserId());

        String applicantName = profileOpt.map(p -> p.fullName()).orElse(application.applicantUserId());
        String major = profileOpt.map(p -> p.programme()).orElse("-");
        String appliedDate = application.submittedAt().toLocalDate().toString();

        Label nameLabel = new Label(applicantName);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        nameLabel.setTextFill(Color.web("#4664a8"));

        Label majorLabel = new Label("Major: " + major);
        majorLabel.setFont(Font.font("Arial", 16));
        majorLabel.setTextFill(Color.web("#2f2f2f"));

        Label dateLabel = new Label("Applied: " + appliedDate);
        dateLabel.setFont(Font.font("Arial", 16));
        dateLabel.setTextFill(Color.web("#2f2f2f"));

        Label statusLabel = new Label(ApplicationStatusPresenter.toDisplayText(application.status()));
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        statusLabel.setTextFill(Color.web("#4664a8"));
        statusLabel.setPadding(new Insets(4, 10, 4, 10));
        statusLabel.setStyle(
            "-fx-background-color: " + ApplicationReviewDetailsPanel.statusColor(application.status()) + ";"
                + " -fx-background-radius: 10;"
        );

        Button detailsButton = UiTheme.createSoftButton("View", 80, 42);
        // Row action only updates local page state; no navigation side effects.
        detailsButton.setOnAction(event -> onSelect.accept(application));

        Region spacer1 = new Region();
        Region spacer2 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        HBox row = new HBox(16, nameLabel, spacer1, statusLabel, detailsButton);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 16, 14, 16));
        row.setMaxWidth(Double.MAX_VALUE);
        row.setStyle(
            (selected ? "-fx-background-color: #fdf4fb;" : "-fx-background-color: transparent;")
                + "-fx-border-color: #f4d9e6; -fx-border-width: 0 0 2 0;"
        );
        return row;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
