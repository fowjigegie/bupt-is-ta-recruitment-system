package UI;

import com.bupt.tarecruitment.application.ApplicationStatus;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.recommendation.RecommendationResult;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DashboardPages extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.APPLICANT_DASHBOARD, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        long openJobs = context.services().jobRepository().findAll().stream()
            .filter(job -> job.status() == com.bupt.tarecruitment.job.JobStatus.OPEN)
            .count();

        long myApplications = context.services().applicationRepository()
            .findByApplicantUserId(context.session().userId())
            .stream()
            .filter(application -> application.status() != ApplicationStatus.WITHDRAWN)
            .count();

        long unreadMessages = context.services().messageService().countUnreadMessagesForUser(context.session().userId());

        VBox center = new VBox(26);
        center.setPadding(new Insets(30, 40, 30, 40));

        HBox welcomeRow = new HBox(18);
        welcomeRow.setAlignment(Pos.CENTER_LEFT);
        welcomeRow.getChildren().addAll(
            createBlueDiamond(),
            UiTheme.createPageHeading("Welcome back, " + context.session().displayName()),
            createBlueDiamond()
        );

        Button invitationButton = UiTheme.createPrimaryButton("Interview\nInvitation", 220, 110);
        invitationButton.setOnAction(event -> nav.goTo(PageId.INTERVIEW_INVITATION));

        Button chatButton = UiTheme.createPrimaryButton("Chat", 220, 110);
        chatButton.setOnAction(event -> nav.goTo(PageId.MESSAGES));

        HBox featureButtons = new HBox(30, invitationButton, createChatBadge(chatButton, unreadMessages));
        featureButtons.setAlignment(Pos.CENTER_LEFT);

        HBox statRow = new HBox(20,
            UiTheme.createStatCard("Open jobs", Long.toString(openJobs), "Browse jobs or jump directly into the detailed view."),
            UiTheme.createStatCard("Unread messages", Long.toString(unreadMessages), "Messages now route to a dedicated page instead of printing logs."),
            UiTheme.createStatCard("My applications", Long.toString(myApplications), "Withdrawn applications are not counted here.")
        );

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
        } else {
            for (RecommendedJobView recommendedJob : recommendedJobs) {
                jobsArea.getChildren().add(createJobRow(nav, context, recommendedJob));
            }
        }

        center.getChildren().addAll(welcomeRow, featureButtons, statRow, jobsArea);

        BorderPane root = UiTheme.createPage(
            "Applicant Dashboard",
            UiTheme.createApplicantSidebar(nav, PageId.APPLICANT_DASHBOARD),
            center,
            nav,
            context
        );
        return UiTheme.createScene(root);
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

        Label teacherLabel = new Label("MO: " + job.organiserId());
        teacherLabel.setFont(Font.font("Arial", 20));
        teacherLabel.setTextFill(Color.web("#4664a8"));
        teacherLabel.setPrefWidth(220);

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

        Button chatButton = UiTheme.createSoftButton("Chat with Teacher", 190, 46);
        chatButton.setOnAction(event -> {
            context.openChatContext(job.jobId(), job.organiserId());
            nav.goTo(PageId.MESSAGES);
        });

        Button browseButton = UiTheme.createOutlineButton("More jobs", 140, 46);
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

    public static void main(String[] args) {
        launch(args);
    }

    private record RecommendedJobView(JobPosting job, RecommendationResult recommendation) {
    }
}
