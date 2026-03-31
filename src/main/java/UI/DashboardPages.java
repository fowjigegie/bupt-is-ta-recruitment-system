package UI;

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
            .size();
        long unreadMessages = 3;

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

        HBox featureButtons = new HBox(30, invitationButton, createChatBadge(chatButton));
        featureButtons.setAlignment(Pos.CENTER_LEFT);

        HBox statRow = new HBox(20,
            UiTheme.createStatCard("Open jobs", Long.toString(openJobs), "Browse jobs or jump directly into the detailed view."),
            UiTheme.createStatCard("Unread messages", Long.toString(unreadMessages), "Messages now route to a dedicated page instead of printing logs."),
            UiTheme.createStatCard("My applications", Long.toString(myApplications), "Application status lives in its own routed page.")
        );

        VBox jobsArea = new VBox(12);
        jobsArea.getChildren().add(UiTheme.createSectionTitle("Recommended jobs"));

        List<com.bupt.tarecruitment.job.JobPosting> recommendedJobs = context.services().jobRepository().findAll().stream()
            .filter(job -> job.status() == com.bupt.tarecruitment.job.JobStatus.OPEN)
            .sorted(Comparator.comparing(com.bupt.tarecruitment.job.JobPosting::jobId))
            .limit(3)
            .toList();

        if (recommendedJobs.isEmpty()) {
            jobsArea.getChildren().add(UiTheme.createWhiteCard("No jobs", "There are no OPEN jobs to recommend right now."));
        } else {
            for (com.bupt.tarecruitment.job.JobPosting job : recommendedJobs) {
                jobsArea.getChildren().add(createJobRow(nav, context, job));
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

    private static StackPane createChatBadge(Button baseButton) {
        Circle badgeCircle = new Circle(18);
        badgeCircle.setFill(Color.web("#ff3333"));

        Label badgeLabel = new Label("3");
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
        com.bupt.tarecruitment.job.JobPosting job
    ) {
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

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button chatButton = UiTheme.createSoftButton("Chat with Teacher", 190, 46);
        chatButton.setOnAction(event -> nav.goTo(PageId.MESSAGES));

        Button browseButton = UiTheme.createOutlineButton("More jobs", 140, 46);
        browseButton.setOnAction(event -> {
            context.selectJob(job.jobId());
            nav.goTo(PageId.MORE_JOBS);
        });

        row.getChildren().addAll(courseLabel, teacherLabel, spacer, browseButton, chatButton);
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
}
