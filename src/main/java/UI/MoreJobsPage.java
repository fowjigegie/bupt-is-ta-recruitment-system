package UI;

import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobStatus;
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
        final int pageSize = 3;
        final int[] currentPage = {0};
        final boolean[] sortNewestFirst = {true};

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
            .toList();

        Label placeholder = new Label("Open jobs: " + jobs.size());
        placeholder.setTextFill(Color.WHITE);
        placeholder.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Region searchSpacer = new Region();
        HBox.setHgrow(searchSpacer, Priority.ALWAYS);

        Button sortButton = UiTheme.createSoftButton("Sort by publish time: newest", 260, 44);
        sortButton.setStyle("-fx-background-color: #f8d7e9; -fx-background-radius: 22; -fx-text-fill: #2f3553; -fx-font-weight: bold; -fx-font-size: 14px;");
        searchBar.getChildren().addAll(searchIcon, divider, placeholder, searchSpacer, sortButton);

        VBox jobsList = new VBox(16);
        HBox pageSelector = new HBox(6);
        pageSelector.setAlignment(Pos.CENTER);
        pageSelector.setPadding(new Insets(4, 6, 4, 6));
        pageSelector.setBackground(new Background(new BackgroundFill(
            Color.web("#e7c4e3"),
            new CornerRadii(24),
            Insets.EMPTY
        )));

        Runnable[] refreshJobsView = new Runnable[1];
        refreshJobsView[0] = () -> {
            List<JobPosting> sortedJobs = jobs.stream()
                .sorted(
                    Comparator.comparingInt(MoreJobsPage::extractJobSequenceNumber)
                        .thenComparing(JobPosting::jobId)
                        .reversed()
                )
                .toList();
            if (!sortNewestFirst[0]) {
                sortedJobs = sortedJobs.stream()
                    .sorted(Comparator.comparingInt(MoreJobsPage::extractJobSequenceNumber).thenComparing(JobPosting::jobId))
                    .toList();
            }

            jobsList.getChildren().clear();
            if (sortedJobs.isEmpty()) {
                jobsList.getChildren().add(UiTheme.createWhiteCard("No jobs", "There are currently no OPEN jobs in the repository."));
                pageSelector.getChildren().clear();
                return;
            }

            int totalPages = (sortedJobs.size() + pageSize - 1) / pageSize;
            currentPage[0] = Math.max(0, Math.min(currentPage[0], totalPages - 1));
            int fromIndex = currentPage[0] * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, sortedJobs.size());

            for (JobPosting job : sortedJobs.subList(fromIndex, toIndex)) {
                jobsList.getChildren().add(createJobCard(nav, context, job));
            }

            pageSelector.getChildren().clear();
            for (int pageNumber = 1; pageNumber <= totalPages; pageNumber++) {
                final int targetPage = pageNumber - 1;
                Button pageButton = new Button(String.valueOf(pageNumber));
                pageButton.setPrefWidth(54);
                pageButton.setPrefHeight(48);
                pageButton.setFont(Font.font("Arial", FontWeight.BOLD, 20));
                pageButton.setStyle(targetPage == currentPage[0]
                    ? "-fx-background-color: #c45786; -fx-background-radius: 22; -fx-text-fill: white;"
                    : "-fx-background-color: transparent; -fx-text-fill: #566589; -fx-background-radius: 22;");
                pageButton.setOnAction(event -> {
                    currentPage[0] = targetPage;
                    refreshJobsView[0].run();
                });
                pageSelector.getChildren().add(pageButton);
            }
        };

        sortButton.setOnAction(event -> {
            sortNewestFirst[0] = !sortNewestFirst[0];
            sortButton.setText(sortNewestFirst[0] ? "Sort by publish time: newest" : "Sort by publish time: oldest");
            sortButton.setStyle(sortNewestFirst[0]
                ? "-fx-background-color: #f8d7e9; -fx-background-radius: 22; -fx-text-fill: #2f3553; -fx-font-weight: bold; -fx-font-size: 14px;"
                : "-fx-background-color: #f3c7dd; -fx-background-radius: 22; -fx-text-fill: #2a2f4e; -fx-font-weight: bold; -fx-font-size: 14px;");
            currentPage[0] = 0;
            refreshJobsView[0].run();
        });

        refreshJobsView[0].run();

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        HBox footer = new HBox(12, UiTheme.createBackButton(nav), footerSpacer, pageSelector);
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
        chatButton.setOnAction(event -> {
            context.openChatContext(job.jobId(), job.organiserId());
            nav.goTo(PageId.MESSAGES);
        });

        row.getChildren().addAll(textBox, spacer, viewDetails, chatButton);
        return new VBox(row);
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static int extractJobSequenceNumber(JobPosting job) {
        String jobId = job.jobId();
        String numeric = jobId.replaceAll("\\D+", "");
        if (numeric.isEmpty()) {
            return Integer.MIN_VALUE;
        }
        try {
            return Integer.parseInt(numeric);
        } catch (NumberFormatException ignored) {
            return Integer.MIN_VALUE;
        }
    }
}
