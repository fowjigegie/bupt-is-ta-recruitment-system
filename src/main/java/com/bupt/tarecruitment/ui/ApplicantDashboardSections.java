package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.recommendation.RecommendationResult;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 封装申请人首页的欢迎区、快捷入口和推荐岗位区。
 */
final class ApplicantDashboardSections {
    private ApplicantDashboardSections() {
    }

    static HBox createWelcomeRow(String displayName) {
        HBox welcomeRow = new HBox(18);
        welcomeRow.setAlignment(Pos.CENTER_LEFT);
        welcomeRow.getChildren().addAll(
            createBlueDiamond(),
            UiTheme.createPageHeading("Welcome back, " + displayName),
            createBlueDiamond()
        );
        return welcomeRow;
    }

    static HBox createFeatureButtons(NavigationManager nav, UiAppContext context, long unreadMessages) {
        Button invitationButton = UiTheme.createPrimaryButton("Application\nStatus", 220, 110);
        invitationButton.setOnAction(event -> nav.goTo(PageId.INTERVIEW_INVITATION));

        Button chatButton = UiTheme.createPrimaryButton("Chat", 220, 110);
        chatButton.setOnAction(event -> {
            context.services().messageService()
                .findMostRecentConversationForUser(context.session().userId())
                .ifPresentOrElse(
                    conversation -> context.openChatContext(conversation.jobId(), conversation.peerUserId()),
                    context::clearSelections
                );
            nav.goTo(PageId.MESSAGES);
        });

        Button aiButton = UiTheme.createPrimaryButton("AI\nAssistant", 220, 110);
        aiButton.setOnAction(event -> FakeAiAssistantDialog.show(aiButton.getScene().getWindow(), context));

        HBox featureButtons = new HBox(30, invitationButton, createChatBadge(chatButton, unreadMessages), aiButton);
        featureButtons.setAlignment(Pos.CENTER_LEFT);
        return featureButtons;
    }

    static HBox createStatRow(long openJobs, long unreadMessages, long myApplications) {
        return new HBox(20,
            UiTheme.createStatCard("Open jobs", Long.toString(openJobs), "Browse jobs or jump directly into the detailed view."),
            UiTheme.createStatCard("Unread messages", Long.toString(unreadMessages), "Unread badges now update from real job-based conversations."),
            UiTheme.createStatCard("My applications", Long.toString(myApplications), "Withdrawn applications are not counted here.")
        );
    }

    static VBox createRecommendedJobsArea(NavigationManager nav, UiAppContext context) {
        VBox jobsArea = new VBox(12);
        jobsArea.getChildren().add(UiTheme.createSectionTitle("Recommended jobs"));

        Map<String, JobPosting> openJobsById = context.services().jobRepository().findAll().stream()
            .filter(job -> job.status() == com.bupt.tarecruitment.job.JobStatus.OPEN)
            .collect(Collectors.toMap(JobPosting::jobId, Function.identity()));
        List<RecommendedJobView> recommendedJobs = context.services().recommendationService()
            .recommendJobsForApplicant(context.session().userId(), 3)
            .stream()
            .map(recommendation -> new RecommendedJobView(openJobsById.get(recommendation.jobId()), recommendation))
            .filter(recommendedJob -> recommendedJob.job() != null)
            .toList();

        if (recommendedJobs.isEmpty()) {
            boolean hasProfile = context.services().profileRepository().findByUserId(context.session().userId()).isPresent();
            jobsArea.getChildren().add(
                UiTheme.createWhiteCard(
                    hasProfile ? "No tailored recommendations yet" : "Profile needed for recommendations",
                    hasProfile
                        ? "We could not find a strong match from your current skills and desired positions. Try browsing all jobs."
                        : "Create or update your profile and skills in Resume Database to unlock personalized recommendations."
                )
            );
            return jobsArea;
        }

        for (RecommendedJobView recommendedJob : recommendedJobs) {
            jobsArea.getChildren().add(createJobRow(nav, context, recommendedJob));
        }
        return jobsArea;
    }

    private static StackPane createChatBadge(Button baseButton, long unreadMessages) {
        if (unreadMessages <= 0) {
            return new StackPane(baseButton);
        }

        Circle badgeCircle = new Circle(18);
        badgeCircle.setFill(Color.web("#ff3333"));

        Label badgeLabel = new Label(Long.toString(Math.min(unreadMessages, 99)));
        badgeLabel.setTextFill(Color.WHITE);
        badgeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        StackPane badge = new StackPane(badgeCircle, badgeLabel);
        badge.setTranslateX(75);
        badge.setTranslateY(-35);
        badge.setMouseTransparent(true);

        StackPane wrapper = new StackPane(baseButton, badge);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setOnMouseClicked(event -> baseButton.fire());
        return wrapper;
    }

    private static VBox createJobRow(
        NavigationManager nav,
        UiAppContext context,
        RecommendedJobView recommendedJob
    ) {
        JobPosting job = recommendedJob.job();
        RecommendationResult recommendation = recommendedJob.recommendation();

        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(18, 22, 18, 22));
        row.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(24), Insets.EMPTY)));
        row.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(24),
            new BorderWidths(1.5)
        )));

        Label courseLabel = new Label(job.title());
        courseLabel.setFont(Font.font("Arial", 22));
        courseLabel.setTextFill(Color.web("#4664a8"));
        courseLabel.setPrefWidth(520);

        Label teacherLabel = new Label("MO: " + context.formatUserLabel(job.organiserId()));
        teacherLabel.setFont(Font.font("Arial", 20));
        teacherLabel.setTextFill(Color.web("#4664a8"));
        teacherLabel.setPrefWidth(320);

        Label reasonLabel = UiTheme.createMutedText(String.join(" | ", recommendation.reasons()));
        reasonLabel.setWrapText(true);
        reasonLabel.setMaxWidth(760);

        Label scoreLabel = new Label("Match score: " + recommendation.matchScore());
        scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        scoreLabel.setTextFill(Color.WHITE);
        scoreLabel.setStyle(
            "-fx-background-color: #7bb661;" +
                "-fx-background-radius: 18;" +
                "-fx-padding: 6 12 6 12;"
        );

        VBox contentBox = new VBox(8, courseLabel, teacherLabel, reasonLabel);
        contentBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button chatButton = UiTheme.createSoftButton("Chat with MO", 190, 46);
        chatButton.setOnAction(event -> {
            context.openChatContext(job.jobId(), job.organiserId());
            nav.goTo(PageId.MESSAGES);
        });

        Button browseButton = UiTheme.createOutlineButton("View details", 140, 46);
        browseButton.setOnAction(event -> {
            context.selectJob(job.jobId());
            nav.goTo(PageId.JOB_DETAIL);
        });

        row.getChildren().addAll(contentBox, scoreLabel, spacer, browseButton, chatButton);
        return new VBox(row);
    }

    private static StackPane createBlueDiamond() {
        javafx.scene.shape.Polygon diamond = new javafx.scene.shape.Polygon(
            0.0, 12.0,
            12.0, 0.0,
            24.0, 12.0,
            12.0, 24.0
        );
        diamond.setFill(Color.web("#4664a8"));
        return new StackPane(diamond);
    }

    private record RecommendedJobView(JobPosting job, RecommendationResult recommendation) {
    }
}
