package UI;

import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobStatus;
import com.bupt.tarecruitment.job.JobBrowseFilter;
import com.bupt.tarecruitment.recommendation.MissingSkillsFeedback;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
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
import java.util.Optional;

public class MoreJobsPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.MORE_JOBS, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
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

        List<JobPosting> jobs = context.services().jobRepository().findAll();

        Label resultLabel = new Label();
        resultLabel.setTextFill(Color.WHITE);
        resultLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Region searchSpacer = new Region();
        HBox.setHgrow(searchSpacer, Priority.ALWAYS);

        Button sortButton = UiTheme.createSoftButton("Sort by job ID: newest", 230, 44);
        sortButton.setStyle("-fx-background-color: #f8d7e9; -fx-background-radius: 22; -fx-text-fill: #2f3553; -fx-font-weight: bold; -fx-font-size: 14px;");
        searchBar.getChildren().addAll(searchIcon, divider, resultLabel, searchSpacer, sortButton);

        TextField keywordField = createFilterField("Search title, module, skill, organiser, MO name, or job ID");
        HBox.setHgrow(keywordField, Priority.ALWAYS);

        ComboBox<String> moduleFilter = createFilterComboBox(190, "All modules");
        moduleFilter.getItems().addAll(JobBrowseFilter.collectUniqueModules(jobs));
        moduleFilter.getSelectionModel().selectFirst();

        ComboBox<String> activityFilter = createFilterComboBox(190, "All activity types");
        activityFilter.getItems().addAll(
            "Lab session",
            "Tutorial",
            "Assignment / marking",
            "Project / development"
        );
        activityFilter.getSelectionModel().selectFirst();

        ComboBox<String> skillFilter = createFilterComboBox(210, "All skills");
        skillFilter.getItems().addAll(JobBrowseFilter.collectUniqueSkills(jobs));
        skillFilter.getSelectionModel().selectFirst();

        ComboBox<String> organiserFilter = createFilterComboBox(200, "All organisers");
        organiserFilter.getItems().addAll(JobBrowseFilter.collectUniqueOrganisers(jobs));
        organiserFilter.getSelectionModel().selectFirst();

        ComboBox<String> timeSlotFilter = createFilterComboBox(170, "Any time");
        timeSlotFilter.getItems().addAll(
            "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN",
            "Morning", "Afternoon", "Evening"
        );
        timeSlotFilter.getSelectionModel().selectFirst();

        Button resetButton = UiTheme.createOutlineButton("Reset filters", 150, 44);

        HBox searchRow = new HBox(12, keywordField, resetButton);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        HBox filterRow = new HBox(12, moduleFilter, activityFilter, skillFilter, organiserFilter, timeSlotFilter);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        VBox filterPanel = new VBox(12, searchRow, filterRow);
        filterPanel.setPadding(new Insets(14, 16, 14, 16));
        filterPanel.setBackground(new Background(new BackgroundFill(
            Color.web("#fffafd"),
            new CornerRadii(18),
            Insets.EMPTY
        )));
        filterPanel.setBorder(new Border(new BorderStroke(
            Color.web("#f0d9e9"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(18),
            new BorderWidths(1.2)
        )));

        VBox jobsList = new VBox(16);
        ScrollPane jobsScroll = new ScrollPane(jobsList);
        jobsScroll.setFitToWidth(true);
        jobsScroll.setPrefViewportHeight(520);
        jobsScroll.setStyle(
            "-fx-background:#ffffff;" +
                "-fx-background-color:#ffffff;" +
                "-fx-border-color:transparent;" +
                "-fx-background-radius:18;"
        );
        VBox.setVgrow(jobsScroll, Priority.ALWAYS);

        Runnable[] refreshJobsView = new Runnable[1];
        refreshJobsView[0] = () -> {
            List<JobPosting> sortedJobs = JobBrowseFilter.filterAndSortOpenJobs(
                jobs,
                keywordField.getText(),
                skillFilter.getValue(),
                organiserFilter.getValue(),
                moduleFilter.getValue(),
                activityFilter.getValue(),
                timeSlotFilter.getValue(),
                sortNewestFirst[0],
                organiserId -> context.services().userRepository().findByUserId(organiserId)
                    .map(account -> account.displayName())
                    .orElse("")
            );
            long openJobs = jobs.stream().filter(job -> job.status() == JobStatus.OPEN).count();
            resultLabel.setText("Showing " + sortedJobs.size() + " of " + openJobs + " OPEN jobs");

            jobsList.getChildren().clear();
            if (sortedJobs.isEmpty()) {
                jobsList.getChildren().add(
                    UiTheme.createWhiteCard(
                        "No matching jobs",
                        "Try changing the keyword, module, activity, skill, organiser, or time filters."
                    )
                );
                return;
            }

            for (JobPosting job : sortedJobs) {
                jobsList.getChildren().add(createJobCard(nav, context, job));
            }
        };

        resetButton.setOnAction(event -> {
            keywordField.clear();
            moduleFilter.getSelectionModel().selectFirst();
            activityFilter.getSelectionModel().selectFirst();
            skillFilter.getSelectionModel().selectFirst();
            organiserFilter.getSelectionModel().selectFirst();
            timeSlotFilter.getSelectionModel().selectFirst();
            refreshJobsView[0].run();
        });

        sortButton.setOnAction(event -> {
            sortNewestFirst[0] = !sortNewestFirst[0];
            sortButton.setText(sortNewestFirst[0] ? "Sort by job ID: newest" : "Sort by job ID: oldest");
            sortButton.setStyle(sortNewestFirst[0]
                ? "-fx-background-color: #f8d7e9; -fx-background-radius: 22; -fx-text-fill: #2f3553; -fx-font-weight: bold; -fx-font-size: 14px;"
                : "-fx-background-color: #f3c7dd; -fx-background-radius: 22; -fx-text-fill: #2a2f4e; -fx-font-weight: bold; -fx-font-size: 14px;");
            refreshJobsView[0].run();
        });

        keywordField.textProperty().addListener((obs, oldValue, newValue) -> {
            refreshJobsView[0].run();
        });
        moduleFilter.valueProperty().addListener((obs, oldValue, newValue) -> {
            refreshJobsView[0].run();
        });
        activityFilter.valueProperty().addListener((obs, oldValue, newValue) -> {
            refreshJobsView[0].run();
        });
        skillFilter.valueProperty().addListener((obs, oldValue, newValue) -> {
            refreshJobsView[0].run();
        });
        organiserFilter.valueProperty().addListener((obs, oldValue, newValue) -> {
            refreshJobsView[0].run();
        });
        timeSlotFilter.valueProperty().addListener((obs, oldValue, newValue) -> {
            refreshJobsView[0].run();
        });

        refreshJobsView[0].run();

        HBox footer = new HBox(12, UiTheme.createBackButton(nav));
        footer.setAlignment(Pos.CENTER_LEFT);

        center.getChildren().addAll(
            UiTheme.createPageHeading("More Jobs"),
            searchBar,
            filterPanel,
            jobsScroll,
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

        Label moLabel = new Label("MO : " + context.formatUserLabel(job.organiserId()) + " | " + job.moduleOrActivity());
        moLabel.setFont(Font.font("Arial", 17));
        moLabel.setTextFill(Color.web("#8b7fa0"));
        moLabel.setWrapText(true);

        Label hoursAndScheduleLabel = UiTheme.createMutedText(
            "Weekly Hours: " + job.weeklyHours() + "h/week | Schedule: " +
                (job.scheduleSlots().isEmpty() ? "(none listed)" : String.join(", ", job.scheduleSlots()))
        );

        Label skillsLabel = UiTheme.createMutedText(
            "Skills: " + (job.requiredSkills().isEmpty() ? "(none listed)" : String.join(", ", job.requiredSkills()))
        );

        textBox.getChildren().addAll(courseLabel, idLabel, moLabel, hoursAndScheduleLabel, skillsLabel);

        Label gapLabel = createSkillGapPreview(context, job);
        if (gapLabel != null) {
            textBox.getChildren().add(gapLabel);
        }

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

    private static Label createSkillGapPreview(UiAppContext context, JobPosting job) {
        if (context.session() == null || !context.session().isAuthenticated()) {
            return null;
        }

        Optional<MissingSkillsFeedback> feedback = context.services().missingSkillsFeedbackService()
            .feedbackForApplicantAndJob(context.session().userId(), job.jobId());
        if (feedback.isEmpty()) {
            return UiTheme.createMutedText("Skill feedback: create your profile to compare skills.");
        }

        MissingSkillsFeedback skillFeedback = feedback.get();
        if (skillFeedback.totalRequiredSkillCount() == 0) {
            return UiTheme.createMutedText("Skill feedback: this job has no listed skill gap.");
        }

        if (skillFeedback.fullyMatched()) {
            return UiTheme.createMutedText("Skill feedback: all listed required skills are already covered.");
        }

        return UiTheme.createMutedText("Missing skills: " + String.join(", ", skillFeedback.missingSkills()));
    }

    private static TextField createFilterField(String promptText) {
        TextField field = new TextField();
        field.setPromptText(promptText);
        field.setPrefHeight(44);
        field.setStyle(
            "-fx-background-color: white;" +
                "-fx-background-radius: 20;" +
                "-fx-border-color: #f4d9e6;" +
                "-fx-border-radius: 20;" +
                "-fx-border-width: 1.6;" +
                "-fx-padding: 0 14 0 14;" +
                "-fx-font-size: 14px;"
        );
        return field;
    }

    private static ComboBox<String> createFilterComboBox(double width, String firstOption) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setPrefWidth(width);
        comboBox.setPrefHeight(44);
        comboBox.getItems().add(firstOption);
        comboBox.setStyle(
            "-fx-background-color: white;" +
                "-fx-background-radius: 20;" +
                "-fx-border-color: #f4d9e6;" +
                "-fx-border-radius: 20;" +
                "-fx-border-width: 1.6;" +
                "-fx-font-size: 14px;"
        );
        return comboBox;
    }
}
