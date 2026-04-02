package UI;

import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.application.ApplicationStatus;
import com.bupt.tarecruitment.job.JobPosting;
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
        Label title = new Label("My Application Status");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        title.setTextFill(Color.web("#4969ad"));
        center.getChildren().add(title);

        if (applications.isEmpty()) {
            center.getChildren().add(UiTheme.createWhiteCard("No applications yet", "Apply for a job from the More Jobs page and the status will appear here."));
        } else {
            for (JobApplication application : applications) {
                center.getChildren().add(createStatusCard(nav, context, application));
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

    private static VBox createStatusCard(NavigationManager nav, UiAppContext context, JobApplication application) {
        JobPosting job = context.services().jobRepository().findByJobId(application.jobId()).orElse(null);
        String title = job == null ? application.jobId() : job.title();
        String organiser = job == null ? "(unknown organiser)" : job.organiserId();
        String schedule = job == null || job.scheduleSlots().isEmpty()
            ? "(schedule not listed)"
            : String.join(", ", job.scheduleSlots());
        String status = application.status().name();
        Color accentColor = statusColor(application.status());

        VBox wrapper = new VBox();
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(22, 30, 22, 30));
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(24), Insets.EMPTY)));
        card.setBorder(new Border(new BorderStroke(
            accentColor,
            BorderStrokeStyle.SOLID,
            new CornerRadii(24),
            new BorderWidths(3)
        )));

        VBox leftInfo = new VBox(12);
        leftInfo.setAlignment(Pos.CENTER_LEFT);
        leftInfo.setPrefWidth(560);
        leftInfo.getChildren().addAll(
            createHeadline(title),
            createMetaLine("Organiser", organiser),
            createMetaLine("Submitted at", application.submittedAt().toString()),
            createMetaLine("Schedule / room", schedule)
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusLabel = new Label(statusLabelText(application.status()));
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        statusLabel.setTextFill(accentColor);

        Button detailsButton = new Button("View Details");
        detailsButton.setPrefSize(130, 40);
        detailsButton.setStyle(
            "-fx-background-color: " + toWeb(accentColor) + ";" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 20;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;"
        );
        detailsButton.setOnAction(event -> {
            context.selectJob(application.jobId());
            nav.goTo(PageId.JOB_DETAIL);
        });

        VBox rightInfo = new VBox(14, createStatusChip(statusLabelText(application.status()), accentColor), statusLabel, detailsButton);
        rightInfo.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(leftInfo, spacer, rightInfo);
        wrapper.getChildren().add(card);
        return wrapper;
    }

    private static Label createHeadline(String value) {
        Label label = new Label(value);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 21));
        label.setTextFill(Color.web("#4969ad"));
        return label;
    }

    private static Label createMetaLine(String title, String value) {
        Label label = new Label(title + ": " + value);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        label.setTextFill(Color.web("#5c6481"));
        label.setWrapText(true);
        return label;
    }

    private static StackPane createStatusChip(String status, Color accentColor) {
        Label label = new Label(status);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        label.setTextFill(Color.WHITE);
        StackPane chip = new StackPane(label);
        chip.setPadding(new Insets(8, 18, 8, 18));
        chip.setBackground(new Background(new BackgroundFill(accentColor, new CornerRadii(20), Insets.EMPTY)));
        return chip;
    }

    private static Color statusColor(ApplicationStatus status) {
        return switch (status) {
            case SUBMITTED -> Color.web("#ff66b3");
            case ACCEPTED -> Color.web("#2ecc71");
            case REJECTED -> Color.web("#e74c3c");
            case SHORTLISTED -> Color.web("#4969ad");
            default -> Color.web("#8b7fa0");
        };
    }

    private static String statusLabelText(ApplicationStatus status) {
        return switch (status) {
            case SUBMITTED -> "Pending Review";
            case SHORTLISTED -> "Shortlisted";
            case ACCEPTED -> "Accepted";
            case REJECTED -> "Rejected";
            default -> status.name();
        };
    }

    private static String toWeb(Color color) {
        return String.format("#%02x%02x%02x",
            (int) Math.round(color.getRed() * 255),
            (int) Math.round(color.getGreen() * 255),
            (int) Math.round(color.getBlue() * 255));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
