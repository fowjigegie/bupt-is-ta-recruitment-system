package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.application.AvailabilityCheckResult;
import com.bupt.tarecruitment.common.text.DisplayFormats;
import com.bupt.tarecruitment.job.JobBrowseFilter;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobStatus;
import com.bupt.tarecruitment.recommendation.MissingSkillsFeedback;
import javafx.application.Application;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

/**
 * 岗位浏览页，负责展示开放岗位并驱动筛选结果刷新。
 */
public class MoreJobsPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.MORE_JOBS, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        VBox center = new VBox(18);
        center.setPadding(new Insets(28, 40, 28, 40));

        List<JobPosting> jobs = context.services().jobRepository().findAll();
        MoreJobsFilters filters = MoreJobsFilters.create(jobs);

        VBox jobsList = new VBox(16);
        ScrollPane jobsScroll = new ScrollPane(jobsList);
        jobsScroll.setFitToWidth(true);
        jobsScroll.setPrefViewportHeight(520);
        jobsScroll.setStyle(
            "-fx-background:#ffffff;" +
                "-fx-background-color:#ffffff;" +
                "-fx-border-color:transparent;" +
                "-fx-background-radius:18;"
        );
        VBox.setVgrow(jobsScroll, Priority.ALWAYS);

        Runnable refreshJobsView = () -> {
            List<JobPosting> sortedJobs = JobBrowseFilter.filterAndSortOpenJobs(
                jobs,
                filters.keyword(),
                filters.skill(),
                filters.organiser(),
                filters.module(),
                filters.activity(),
                filters.timeSlot(),
                filters.sortNewestFirst(),
                organiserId -> context.services().userRepository().findByUserId(organiserId)
                    .map(account -> account.displayName())
                    .orElse("")
            );
            long openJobs = jobs.stream().filter(job -> job.status() == JobStatus.OPEN).count();
            filters.updateResults(sortedJobs.size(), openJobs);

            jobsList.getChildren().clear();
            if (sortedJobs.isEmpty()) {
                jobsList.getChildren().add(
                    UiTheme.createWhiteCard(
                        "No matching jobs",
                        "Try changing the keyword, module, activity, skill, organiser, or time filters."
                    )
                );
                return;
            }

            for (JobPosting job : sortedJobs) {
                jobsList.getChildren().add(createJobCard(nav, context, job));
            }
        };

        filters.attachRefresh(refreshJobsView);
        refreshJobsView.run();

        HBox footer = new HBox(12, UiTheme.createBackButton(nav));
        footer.setAlignment(Pos.CENTER_LEFT);

        center.getChildren().addAll(
            UiTheme.createPageHeading("More Jobs"),
            filters.searchBar(),
            filters.filterPanel(),
            jobsScroll,
            footer
        );

        BorderPane root = UiTheme.createPage(
            "More Jobs",
            UiTheme.createApplicantSidebar(nav, PageId.MORE_JOBS),
            center,
            nav,
            context
        );
        return UiTheme.createScene(root);
    }

    private static VBox createJobCard(NavigationManager nav, UiAppContext context, JobPosting job) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(20, 24, 20, 24));
        row.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(24), Insets.EMPTY)));
        row.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(24),
            new BorderWidths(1.5)
        )));

        VBox textBox = new VBox(6);
        Label courseLabel = new Label(job.title());
        courseLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        courseLabel.setTextFill(Color.web("#4664a8"));

        Label idLabel = new Label("Job ID : " + job.jobId());
        idLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        idLabel.setTextFill(Color.web("#ff66b3"));

        Label teacherLabel = new Label("Organiser : " + context.formatUserLabel(job.organiserId()));
        teacherLabel.setFont(Font.font("Arial", 16));
        teacherLabel.setTextFill(Color.web("#333333"));

        Label classLabel = new Label("Module / Activity : " + job.moduleOrActivity());
        classLabel.setFont(Font.font("Arial", 16));
        classLabel.setTextFill(Color.web("#333333"));

        Label timeLabel = new Label(
            "Weekly hours : " + DisplayFormats.formatDecimal(job.weeklyHours()) + "    |    Schedule : "
                + FixedScheduleBands.formatScheduleList(job.scheduleSlots())
        );
        timeLabel.setFont(Font.font("Arial", 15));
        timeLabel.setTextFill(Color.web("#666666"));

        Label availabilityLabel = createAvailabilityPreview(context, job);
        Label skillGapLabel = createSkillGapPreview(context, job);

        textBox.getChildren().addAll(courseLabel, idLabel, teacherLabel, classLabel, timeLabel, availabilityLabel, skillGapLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        var detailButton = UiTheme.createPrimaryButton("View Detail", 180, 56);
        detailButton.setOnAction(event -> {
            context.selectJob(job.jobId());
            nav.goTo(PageId.JOB_DETAIL);
        });

        row.getChildren().addAll(textBox, spacer, detailButton);

        VBox card = new VBox(row);
        card.setFillWidth(true);
        return card;
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static Label createAvailabilityPreview(UiAppContext context, JobPosting job) {
        Label label = new Label();
        label.setWrapText(true);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        if (!context.session().isAuthenticated()) {
            label.setText("Log in as an applicant to compare this job with your availability.");
            applyStatusLabelStyle(label, "#8b7fa0");
            return label;
        }

        Optional<AvailabilityCheckResult> availability = context.services().applicantAvailabilityService()
            .availabilityForApplicantAndJob(context.session().userId(), job.jobId());

        if (availability.isEmpty()) {
            label.setText("Create or update your profile availability to check whether this job fits your time.");
            applyStatusLabelStyle(label, "#8b7fa0");
            return label;
        }

        if (job.scheduleSlots().isEmpty()) {
            label.setText("This job does not list schedule slots.");
            applyStatusLabelStyle(label, "#8b7fa0");
            return label;
        }

        AvailabilityCheckResult result = availability.get();
        if (result.fitsAvailability()) {
            label.setText("Availability fit: all listed job slots are covered by your current profile.");
            applyStatusLabelStyle(label, "#2e7d32");
            return label;
        }

        label.setText(
            "Availability conflict: update your profile to cover "
                + result.uncoveredJobSlots().stream()
                    .map(FixedScheduleBands::formatSlotForDisplay)
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("(schedule not listed)")
        );
        applyStatusLabelStyle(label, "#b00020");
        return label;
    }

    private static void applyStatusLabelStyle(Label label, String color) {
        label.setStyle(
            "-fx-font-family: Arial;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + color + ";"
        );
    }

    private static Label createSkillGapPreview(UiAppContext context, JobPosting job) {
        Label label = new Label();
        label.setWrapText(true);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        if (!context.session().isAuthenticated()) {
            label.setText("Log in as an applicant to see matched and missing skills.");
            label.setTextFill(Color.web("#8b7fa0"));
            return label;
        }

        Optional<MissingSkillsFeedback> feedback = context.services().missingSkillsFeedbackService()
            .feedbackForApplicantAndJob(context.session().userId(), job.jobId());

        if (feedback.isEmpty()) {
            label.setText("Create or update your profile in Resume Database to preview your skill gap.");
            label.setTextFill(Color.web("#8b7fa0"));
            return label;
        }

        MissingSkillsFeedback skillFeedback = feedback.get();
        if (skillFeedback.totalRequiredSkillCount() == 0) {
            label.setText("This job does not list required skills.");
            label.setTextFill(Color.web("#8b7fa0"));
            return label;
        }

        label.setText(
            "Skill match: "
                + skillFeedback.coveragePercent()
                + "%  |  Weak: "
                + (skillFeedback.weaklyMatchedSkills().isEmpty()
                    ? "none"
                    : String.join(", ", skillFeedback.weaklyMatchedSkills()))
                + "  |  Missing: "
                + (skillFeedback.missingSkills().isEmpty()
                    ? "none"
                    : String.join(", ", skillFeedback.missingSkills()))
        );
        if (skillFeedback.missingSkills().isEmpty() && skillFeedback.weaklyMatchedSkills().isEmpty()) {
            label.setTextFill(Color.web("#2e7d32"));
        } else if (skillFeedback.missingSkills().isEmpty()) {
            label.setTextFill(Color.web("#c77800"));
        } else {
            label.setTextFill(Color.web("#8a4f7a"));
        }
        return label;
    }
}
