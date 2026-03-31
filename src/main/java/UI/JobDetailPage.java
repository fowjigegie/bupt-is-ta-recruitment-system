package UI;

import com.bupt.tarecruitment.applicant.ApplicantCv;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobStatus;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.Comparator;
import java.util.List;

public class JobDetailPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.JOB_DETAIL, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        JobPosting selectedJob = resolveSelectedJob(context);

        VBox main = new VBox(18);
        main.setPadding(new Insets(28, 40, 28, 40));

        if (selectedJob == null) {
            main.getChildren().addAll(
                UiTheme.createPageHeading("Job detail"),
                UiTheme.createWhiteCard("No job selected", "Go back to the More Jobs page and choose an OPEN job first."),
                new HBox(UiTheme.createBackButton(nav))
            );
        } else {
            context.selectJob(selectedJob.jobId());
            main.getChildren().addAll(createJobDetailContent(nav, context, selectedJob));
        }

        BorderPane root = UiTheme.createPage(
            "Job Detail",
            UiTheme.createApplicantSidebar(nav, PageId.MORE_JOBS),
            main,
            nav,
            context
        );
        return UiTheme.createScene(root);
    }

    private static List<javafx.scene.Node> createJobDetailContent(NavigationManager nav, UiAppContext context, JobPosting job) {
        var courseTag = UiTheme.createTag("Course Title : " + job.title(), 500);
        var teacherTag = UiTheme.createTag("Taught By : " + job.organiserId(), 280);
        var classTag = UiTheme.createTag("Module / Activity : " + job.moduleOrActivity(), 420);
        var hoursTag = UiTheme.createTag("Weekly Hours : " + job.weeklyHours(), 260);
        var statusTag = UiTheme.createTag("Status : " + job.status().name(), 220);

        VBox details = new VBox(16,
            new HBox(20, courseTag, teacherTag),
            new HBox(20, classTag, hoursTag, statusTag),
            UiTheme.createWhiteCard(
                "Job Description",
                job.description()
            ),
            UiTheme.createWhiteCard(
                "Requirements",
                "Required skills: " + (job.requiredSkills().isEmpty() ? "(none listed)" : String.join(", ", job.requiredSkills())) +
                    System.lineSeparator() +
                    System.lineSeparator() +
                    "Schedule: " + (job.scheduleSlots().isEmpty() ? "(none listed)" : String.join(", ", job.scheduleSlots()))
            )
        );

        Label applyTitle = new Label("Apply with one of your CVs");
        applyTitle.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        applyTitle.setTextFill(Color.web("#4664a8"));

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setTextFill(Color.web("#b00020"));

        ComboBox<ApplicantCv> cvBox = new ComboBox<>();
        cvBox.setPrefWidth(380);
        cvBox.setPrefHeight(42);
        cvBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ApplicantCv applicantCv) {
                if (applicantCv == null) {
                    return "";
                }
                return applicantCv.cvId() + " - " + applicantCv.title();
            }

            @Override
            public ApplicantCv fromString(String string) {
                return null;
            }
        });

        if (context.session().isAuthenticated()) {
            try {
                List<ApplicantCv> cvs = context.services().cvLibraryService()
                    .listCvsByUserId(context.session().userId())
                    .stream()
                    .sorted(Comparator.comparing(ApplicantCv::cvId))
                    .toList();
                cvBox.getItems().addAll(cvs);
                if (!cvs.isEmpty()) {
                    cvBox.getSelectionModel().selectFirst();
                }
            } catch (IllegalArgumentException exception) {
                statusLabel.setText(exception.getMessage());
            }
        }

        var applyButton = UiTheme.createPrimaryButton("Apply now", 190, 56);
        applyButton.setDisable(job.status() != JobStatus.OPEN);
        applyButton.setOnAction(event -> applyToJob(context, job, cvBox, statusLabel));
        var chatButton = UiTheme.createSoftButton("Chat with MO", 170, 56);
        chatButton.setOnAction(event -> {
            context.openChatContext(job.jobId(), job.organiserId());
            nav.goTo(PageId.MESSAGES);
        });

        var backButton = UiTheme.createBackButton(nav);

        HBox actions = new HBox(16, cvBox, applyButton, chatButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        HBox footer = new HBox(16, backButton);
        footer.setAlignment(Pos.CENTER_LEFT);

        return List.of(
            UiTheme.createPageHeading("Job detail"),
            details,
            applyTitle,
            actions,
            statusLabel,
            footer
        );
    }

    private static void applyToJob(
        UiAppContext context,
        JobPosting job,
        ComboBox<ApplicantCv> cvBox,
        Label statusLabel
    ) {
        statusLabel.setTextFill(Color.web("#b00020"));
        statusLabel.setText("");

        ApplicantCv selectedCv = cvBox.getValue();
        if (selectedCv == null) {
            statusLabel.setText("Please create or choose a CV before applying.");
            return;
        }

        try {
            JobApplication application = context.services().jobApplicationService().applyToJobWithCv(
                context.session().userId(),
                job.jobId(),
                selectedCv.cvId()
            );
            context.selectApplication(application.applicationId());
            statusLabel.setTextFill(Color.web("#2e7d32"));
            statusLabel.setText("Application submitted successfully: " + application.applicationId());
        } catch (IllegalArgumentException exception) {
            statusLabel.setTextFill(Color.web("#b00020"));
            statusLabel.setText(exception.getMessage());
        }
    }

    private static JobPosting resolveSelectedJob(UiAppContext context) {
        String selectedJobId = context.selectedJobId();
        if (selectedJobId != null) {
            return context.services().jobRepository().findByJobId(selectedJobId).orElse(null);
        }

        return context.services().jobRepository().findAll().stream()
            .filter(job -> job.status() == JobStatus.OPEN)
            .sorted(Comparator.comparing(JobPosting::jobId))
            .findFirst()
            .orElse(null);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
