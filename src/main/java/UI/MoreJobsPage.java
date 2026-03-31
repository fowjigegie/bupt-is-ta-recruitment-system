package UI;

import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobStatus;
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

public class MoreJobsPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.MORE_JOBS, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        VBox center = new VBox(18);
        center.setPadding(new Insets(28, 40, 28, 40));

        HBox searchBar = new HBox(16);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setPadding(new Insets(0, 22, 0, 22));
        searchBar.setPrefHeight(56);
        searchBar.setBackground(new Background(new BackgroundFill(
            Color.web("#ffcce6"),
            new CornerRadii(28),
            Insets.EMPTY
        )));

        Label searchIcon = new Label("Jobs");
        searchIcon.setTextFill(Color.WHITE);
        searchIcon.setFont(Font.font("Arial", FontWeight.BOLD, 22));

        Region divider = new Region();
        divider.setPrefWidth(3);
        divider.setPrefHeight(34);
        divider.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

        List<JobPosting> jobs = context.services().jobRepository().findAll().stream()
            .filter(job -> job.status() == JobStatus.OPEN)
            .sorted(Comparator.comparing(JobPosting::jobId))
            .toList();

        Label placeholder = new Label("Open jobs: " + jobs.size());
        placeholder.setTextFill(Color.WHITE);
        placeholder.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Region searchSpacer = new Region();
        HBox.setHgrow(searchSpacer, Priority.ALWAYS);

        var timeButton = UiTheme.createSoftButton("Open only", 120, 44);
        searchBar.getChildren().addAll(searchIcon, divider, placeholder, searchSpacer, timeButton);

        VBox jobsList = new VBox(16);
        if (jobs.isEmpty()) {
            jobsList.getChildren().add(UiTheme.createWhiteCard("No jobs", "There are currently no OPEN jobs in the repository."));
        } else {
            for (JobPosting job : jobs) {
                jobsList.getChildren().add(createJobCard(nav, context, job));
            }
        }

        HBox footer = new HBox(UiTheme.createBackButton(nav));
        footer.setAlignment(Pos.CENTER_LEFT);

        center.getChildren().addAll(
            UiTheme.createPageHeading("More jobs"),
            searchBar,
            jobsList,
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

        Label moLabel = new Label("MO : " + job.organiserId() + " | " + job.moduleOrActivity());
        moLabel.setFont(Font.font("Arial", 17));
        moLabel.setTextFill(Color.web("#8b7fa0"));

        Label hoursAndScheduleLabel = UiTheme.createMutedText(
            "Weekly Hours: " + job.weeklyHours() + "h/week | Schedule: " +
                (job.scheduleSlots().isEmpty() ? "(none listed)" : String.join(", ", job.scheduleSlots()))
        );

        Label skillsLabel = UiTheme.createMutedText(
            "Skills: " + (job.requiredSkills().isEmpty() ? "(none listed)" : String.join(", ", job.requiredSkills()))
        );
        textBox.getChildren().addAll(courseLabel, idLabel, moLabel, hoursAndScheduleLabel, skillsLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        var viewDetails = UiTheme.createSoftButton("View Details", 150, 44);
        viewDetails.setOnAction(event -> {
            context.selectJob(job.jobId());
            nav.goTo(PageId.JOB_DETAIL);
        });

        var chatButton = UiTheme.createSoftButton("Chat with MO", 180, 44);
        chatButton.setOnAction(event -> nav.goTo(PageId.MESSAGES));

        row.getChildren().addAll(textBox, spacer, viewDetails, chatButton);
        return new VBox(row);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
