package UI;

import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.job.JobPosting;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;

public class InterviewInvitationPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.INTERVIEW_INVITATION, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        List<JobApplication> applications = context.services().applicationRepository()
            .findByApplicantUserId(context.session().userId())
            .stream()
            .sorted(Comparator.comparing(JobApplication::submittedAt).reversed())
            .toList();

        VBox center = new VBox(24);
        center.setPadding(new Insets(35, 40, 28, 40));
        center.getChildren().addAll(
            UiTheme.createPageHeading("Application status"),
            UiTheme.createMutedText("This page now reflects your real submitted applications and their current statuses.")
        );

        if (applications.isEmpty()) {
            center.getChildren().add(UiTheme.createWhiteCard("No applications yet", "Apply for a job from the More Jobs page and the status will appear here."));
        } else {
            for (JobApplication application : applications) {
                center.getChildren().add(createStatusCard(context, application));
            }
        }

        center.getChildren().add(new HBox(UiTheme.createBackButton(nav)));

        BorderPane root = UiTheme.createPage(
            "Interview Invitation",
            UiTheme.createApplicantSidebar(nav, PageId.INTERVIEW_INVITATION),
            center,
            nav,
            context
        );
        return UiTheme.createScene(root);
    }

    private static VBox createStatusCard(UiAppContext context, JobApplication application) {
        JobPosting job = context.services().jobRepository().findByJobId(application.jobId()).orElse(null);
        String title = job == null ? application.jobId() : job.title();
        String organiser = job == null ? "(unknown organiser)" : job.organiserId();
        String schedule = job == null || job.scheduleSlots().isEmpty()
            ? "(schedule not listed)"
            : String.join(", ", job.scheduleSlots());

        VBox wrapper = new VBox();
        HBox card = new HBox(60);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(28, 32, 28, 32));
        card.setPrefHeight(210);
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(24), Insets.EMPTY)));
        card.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(24),
            new BorderWidths(1.5)
        )));

        VBox leftInfo = new VBox(16);
        leftInfo.setAlignment(Pos.CENTER_LEFT);
        leftInfo.setPrefWidth(470);
        leftInfo.getChildren().addAll(
            createInfoLine("Job: ", title),
            createInfoLine("Organiser: ", organiser),
            createInfoLine("Submitted at: ", application.submittedAt().toString())
        );

        VBox rightInfo = new VBox(16);
        rightInfo.setAlignment(Pos.CENTER_LEFT);
        rightInfo.getChildren().addAll(
            createInfoLine("Schedule / room: ", schedule),
            UiTheme.createSectionTitle("Current Status"),
            createStatusChip(application.status().name())
        );

        card.getChildren().addAll(leftInfo, rightInfo);
        wrapper.getChildren().add(card);
        return wrapper;
    }

    private static Label createInfoLine(String title, String value) {
        Label label = new Label(title + value);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 19));
        label.setTextFill(Color.web("#333333"));
        label.setWrapText(true);
        return label;
    }

    private static Label createStatusChip(String status) {
        Label label = new Label(status);
        label.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, 22));
        label.setTextFill(Color.WHITE);
        label.setStyle(
            "-fx-background-color: linear-gradient(to right, #ffd699, #ffb3d9);" +
                "-fx-background-radius: 22;" +
                "-fx-padding: 6 18 6 18;"
        );
        return label;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
