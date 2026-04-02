package UI;

import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobStatus;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
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
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
        boolean isClosed = job.status() == JobStatus.CLOSED;

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
            "-fx-background-color: " + (isClosed ? "#e53935" : "#7bb661") + ";" +
                "-fx-background-radius: 18;" +
                "-fx-padding: 6 14 6 14;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        var editButton = UiTheme.createSoftButton("Edit Details", 150, 42);
        if (isClosed) {
            editButton.setDisable(true);
            editButton.setOpacity(0.55);
        }
        editButton.setOnAction(event -> {
            context.beginEditJob(job.jobId());
            nav.goTo(PageId.POST_VACANCIES);
        });

        var changeStatusButton = UiTheme.createSoftButton("Change Status", 170, 42);
        Polygon triangle = new Polygon(
            0.0, 0.0,
            10.0, 0.0,
            5.0, 7.0
        );
        triangle.setFill(Color.web("#333333"));
        changeStatusButton.setGraphic(triangle);
        changeStatusButton.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        changeStatusButton.setGraphicTextGap(10);

        ContextMenu statusMenu = new ContextMenu();
        MenuItem openItem = new MenuItem("Open");
        MenuItem closeItem = new MenuItem("Close");
        openItem.setOnAction(event -> {
            if (!isClosed) {
                return;
            }
            updateJobStatus(context, job, JobStatus.OPEN);
            nav.replace(PageId.JOB_MANAGEMENT);
        });
        closeItem.setOnAction(event -> {
            if (isClosed) {
                return;
            }
            if (!confirmClose(job)) {
                return;
            }
            updateJobStatus(context, job, JobStatus.CLOSED);
            nav.replace(PageId.JOB_MANAGEMENT);
        });
        statusMenu.getItems().addAll(openItem, closeItem);

        // Show the menu right under the small triangle.
        changeStatusButton.setOnAction(event -> {
            if (statusMenu.isShowing()) {
                statusMenu.hide();
                return;
            }
            double x = changeStatusButton.getWidth() - 18; // near triangle
            double y = changeStatusButton.getHeight();
            var point = changeStatusButton.localToScreen(x, y);
            if (point != null) {
                statusMenu.show(changeStatusButton, point.getX(), point.getY());
            } else {
                statusMenu.show(changeStatusButton, Side.BOTTOM, 0, 0);
            }
        });

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

        HBox actionBox = new HBox(12, editButton, changeStatusButton, reviewButton);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        row.getChildren().addAll(textBox, statusLabel, spacer, actionBox);
        return new VBox(row);
    }

    private static boolean confirmClose(JobPosting job) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm status change");
        alert.setHeaderText("Close this posting?");
        alert.setContentText(
            "Are you sure you want to set this job to CLOSED?\n\n" +
                "Job: " + job.jobId() + " | " + job.title() + "\n\n" +
                "Applicants will no longer be able to apply."
        );
        Optional<ButtonType> choice = alert.showAndWait();
        return choice.isPresent() && choice.get() == ButtonType.OK;
    }

    private static void updateJobStatus(UiAppContext context, JobPosting job, JobStatus nextStatus) {
        JobPosting updated = new JobPosting(
            job.jobId(),
            job.organiserId(),
            job.title(),
            job.moduleOrActivity(),
            job.description(),
            job.requiredSkills(),
            job.weeklyHours(),
            job.scheduleSlots(),
            nextStatus
        );
        context.services().jobPostingService().publish(updated);
        context.selectJob(job.jobId());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
