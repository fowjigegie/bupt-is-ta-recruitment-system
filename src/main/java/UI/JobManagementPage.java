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
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;

public class JobManagementPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.JOB_MANAGEMENT, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        List<JobPosting> jobs = context.services().jobRepository().findAll().stream()
            .filter(job -> job.organiserId().equals(context.session().userId()))
            .sorted(Comparator.comparing(JobPosting::jobId))
            .toList();

        VBox center = new VBox(20);
        center.setPadding(new Insets(30, 40, 30, 40));
        center.getChildren().addAll(
            UiTheme.createPageHeading("Job management"),
            UiTheme.createMutedText("This page now reads real jobs and applications from the repository.")
        );

        if (jobs.isEmpty()) {
            center.getChildren().add(UiTheme.createWhiteCard("No jobs yet", "Publish a vacancy first and it will appear here."));
        } else {
            for (JobPosting job : jobs) {
                center.getChildren().add(createListingRow(nav, context, job));
            }
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox footer = new HBox(UiTheme.createBackButton(nav));
        footer.setAlignment(Pos.CENTER_LEFT);
        center.getChildren().addAll(spacer, footer);

        BorderPane root = UiTheme.createPage(
            "Job Management",
            UiTheme.createMoSidebar(nav, PageId.JOB_MANAGEMENT),
            center,
            nav,
            context
        );
        return UiTheme.createScene(root);
    }

    private static VBox createListingRow(NavigationManager nav, UiAppContext context, JobPosting job) {
        long applicants = context.services().applicationRepository().findAll().stream()
            .filter(application -> application.jobId().equals(job.jobId()))
            .count();

        HBox row = new HBox(18);
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
        Label titleLabel = new Label(job.title());
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.web("#4664a8"));

        Label applicantLabel = UiTheme.createMutedText(
            job.jobId() + " | " + job.moduleOrActivity() + " | applicants: " + applicants
        );
        textBox.getChildren().addAll(titleLabel, applicantLabel);

        Label statusLabel = new Label(job.status().name());
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        statusLabel.setTextFill(Color.WHITE);
        statusLabel.setStyle(
            "-fx-background-color: " + ("OPEN".equals(job.status().name()) ? "#7bb661" : "#8b7fa0") + ";" +
                "-fx-background-radius: 18;" +
                "-fx-padding: 6 14 6 14;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        var reviewButton = UiTheme.createSoftButton("View applicants", 150, 42);
        reviewButton.setOnAction(event -> {
            context.selectJob(job.jobId());
            JobApplication firstApplication = context.services().applicationRepository().findAll().stream()
                .filter(application -> application.jobId().equals(job.jobId()))
                .sorted(Comparator.comparing(JobApplication::applicationId))
                .findFirst()
                .orElse(null);
            context.selectApplication(firstApplication == null ? null : firstApplication.applicationId());
            nav.goTo(PageId.APPLICATION_REVIEW);
        });

        row.getChildren().addAll(textBox, statusLabel, spacer, reviewButton);
        return new VBox(row);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
