package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.applicant.ApplicantCv;
import com.bupt.tarecruitment.application.ApplicationStatus;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobStatus;
import com.bupt.tarecruitment.recommendation.MissingSkillsFeedback;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 岗位详情页，展示岗位信息并承接投递流程。
 */
public class JobDetailPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.JOB_DETAIL, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        JobPosting selectedJob = resolveSelectedJob(context);

        VBox main = new VBox(18);
        main.setPadding(new Insets(28, 40, 28, 40));

        if (selectedJob == null) {
            main.getChildren().addAll(
                UiTheme.createPageHeading("Job detail"),
                UiTheme.createWhiteCard("No job selected", "Go back to the More Jobs page and choose an OPEN job first."),
                new HBox(UiTheme.createBackButton(nav))
            );
        } else {
            context.selectJob(selectedJob.jobId());
            main.getChildren().addAll(createJobDetailContent(nav, context, selectedJob));
        }

        ScrollPane scrollPane = new ScrollPane(main);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(
            "-fx-background:#ffffff;" +
                "-fx-background-color:transparent;" +
                "-fx-border-color:transparent;"
        );

        BorderPane root = UiTheme.createPage(
            "Job Detail",
            UiTheme.createApplicantSidebar(nav, PageId.MORE_JOBS),
            scrollPane,
            nav,
            context
        );
        return UiTheme.createScene(root);
    }

    private static List<javafx.scene.Node> createJobDetailContent(NavigationManager nav, UiAppContext context, JobPosting job) {
        var courseTag = UiTheme.createTag("Course Title : " + job.title(), 500);
        var teacherTag = UiTheme.createTag("Organised By : " + context.formatUserLabel(job.organiserId()), 400);
        var classTag = UiTheme.createTag("Module / Activity : " + job.moduleOrActivity(), 420);
        var hoursTag = UiTheme.createTag("Weekly Hours : " + job.weeklyHours(), 260);
        var statusTag = UiTheme.createTag("Status : " + job.status().name(), 220);

        VBox details = new VBox(16,
            new HBox(20, courseTag, teacherTag),
            new HBox(20, classTag, hoursTag, statusTag),
            UiTheme.createWhiteCard(
                "Job Description",
                job.description()
            ),
            UiTheme.createWhiteCard(
                "Requirements",
                "Required skills: " + (job.requiredSkills().isEmpty() ? "(none listed)" : String.join(", ", job.requiredSkills())) +
                    System.lineSeparator() +
                    System.lineSeparator() +
                    "Schedule: " + (job.scheduleSlots().isEmpty() ? "(none listed)" : String.join(", ", job.scheduleSlots()))
            ),
            createSkillGapFeedbackCard(context, job)
        );

        JobApplication currentApplication = resolveActiveApplicationForCurrentJob(context, job);
        JobApplicationPanel applicationPanel = JobApplicationPanel.create(
            nav,
            context,
            job,
            currentApplication,
            (selectedCv, statusLabel) -> applyToJob(nav, context, job, selectedCv, statusLabel),
            statusLabel -> {
                if (currentApplication != null) {
                    withdrawApplication(nav, context, currentApplication, statusLabel);
                }
            },
            () -> {
                context.openChatContext(job.jobId(), job.organiserId());
                nav.goTo(PageId.MESSAGES);
            }
        );

        return List.of(
            UiTheme.createPageHeading("Job detail"),
            details,
            applicationPanel.container()
        );
    }

    private static VBox createSkillGapFeedbackCard(UiAppContext context, JobPosting job) {
        if (!context.session().isAuthenticated()) {
            return UiTheme.createWhiteCard(
                "Missing skills feedback",
                "Log in as an applicant and create your profile to compare your skills with this job."
            );
        }

        Optional<MissingSkillsFeedback> feedback = context.services().missingSkillsFeedbackService()
            .feedbackForApplicantAndJob(context.session().userId(), job.jobId());

        if (feedback.isEmpty()) {
            return UiTheme.createWhiteCard(
                "Missing skills feedback",
                "Create or update your profile in Resume Database to see which required skills you already match and which ones are still missing."
            );
        }

        MissingSkillsFeedback skillFeedback = feedback.get();
        if (skillFeedback.totalRequiredSkillCount() == 0) {
            return UiTheme.createWhiteCard(
                "Missing skills feedback",
                "This job does not list required skills, so there is no skill gap to report."
            );
        }

        StringBuilder body = new StringBuilder();
        body.append("Coverage: ")
            .append(skillFeedback.coveragePercent())
            .append("% of listed required skills matched.")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("Matched skills: ")
            .append(skillFeedback.matchedSkills().isEmpty() ? "(none yet)" : String.join(", ", skillFeedback.matchedSkills()))
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("Missing skills to improve: ")
            .append(skillFeedback.missingSkills().isEmpty() ? "(none)" : String.join(", ", skillFeedback.missingSkills()));

        return UiTheme.createWhiteCard("Missing skills feedback", body.toString());
    }

    private static void applyToJob(
        NavigationManager nav,
        UiAppContext context,
        JobPosting job,
        ApplicantCv selectedCv,
        Label statusLabel
    ) {
        statusLabel.setTextFill(Color.web("#b00020"));
        statusLabel.setText("");

        if (!context.session().isAuthenticated()) {
            statusLabel.setText("Please log in before applying.");
            return;
        }

        if (job.status() != JobStatus.OPEN) {
            statusLabel.setText("This job is no longer open for application.");
            return;
        }

        if (selectedCv == null) {
            statusLabel.setText("Please create or choose a CV before applying.");
            return;
        }

        try {
            JobApplication application = context.services().jobApplicationService().applyToJobWithCv(
                context.session().userId(),
                job.jobId(),
                selectedCv.cvId()
            );

            context.selectApplication(application.applicationId());

            statusLabel.setTextFill(Color.web("#2e7d32"));
            statusLabel.setText("Application submitted successfully: " + application.applicationId());

            nav.goTo(PageId.JOB_DETAIL);
        } catch (IllegalArgumentException exception) {
            String message = exception.getMessage();

            if (message != null && message.toLowerCase().contains("duplicate")) {
                statusLabel.setText("You have already applied for this job.");
            } else if (message != null && message.toLowerCase().contains("closed")) {
                statusLabel.setText("This job is already closed.");
            } else {
                statusLabel.setText(message);
            }

            statusLabel.setTextFill(Color.web("#b00020"));
        }
    }

    private static void withdrawApplication(
        NavigationManager nav,
        UiAppContext context,
        JobApplication application,
        Label statusLabel
    ) {
        statusLabel.setTextFill(Color.web("#b00020"));
        statusLabel.setText("");

        if (!context.session().isAuthenticated()) {
            statusLabel.setText("Please log in before withdrawing an application.");
            return;
        }

        try {
            JobApplication updated = context.services().jobApplicationService().withdrawApplication(
                context.session().userId(),
                application.applicationId()
            );

            context.selectApplication(updated.applicationId());

            statusLabel.setTextFill(Color.web("#2e7d32"));
            statusLabel.setText("Application withdrawn successfully: " + updated.applicationId());

            nav.goTo(PageId.JOB_DETAIL);
        } catch (IllegalArgumentException exception) {
            statusLabel.setTextFill(Color.web("#b00020"));
            statusLabel.setText(exception.getMessage());
        }
    }

    private static JobApplication resolveActiveApplicationForCurrentJob(UiAppContext context, JobPosting job) {
        if (context.session() == null || !context.session().isAuthenticated()) {
            return null;
        }

        return context.services().applicationRepository()
            .findByApplicantUserId(context.session().userId())
            .stream()
            .filter(application -> application.jobId().equals(job.jobId()))
            .filter(application -> application.status() != ApplicationStatus.WITHDRAWN)
            .findFirst()
            .orElse(null);
    }

    private static JobPosting resolveSelectedJob(UiAppContext context) {
        String selectedJobId = context.selectedJobId();
        if (selectedJobId != null) {
            return context.services().jobRepository().findByJobId(selectedJobId).orElse(null);
        }

        return context.services().jobRepository().findAll().stream()
            .filter(job -> job.status() == JobStatus.OPEN)
            .sorted(Comparator.comparing(JobPosting::jobId))
            .findFirst()
            .orElse(null);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
