package UI;

import com.bupt.tarecruitment.applicant.ApplicantCvReview;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.job.JobPosting;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
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
import java.util.Set;
import java.util.stream.Collectors;

public class ApplicationReviewPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.APPLICATION_REVIEW, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        List<JobApplication> applications = loadReviewableApplications(context);

        VBox center = new VBox(18);
        center.setPadding(new Insets(35, 40, 28, 40));
        center.getChildren().addAll(
            UiTheme.createPageHeading("Application review"),
            UiTheme.createMutedText("This page now loads real application, profile, and CV data for jobs owned by the current organiser.")
        );

        if (applications.isEmpty()) {
            center.getChildren().add(UiTheme.createWhiteCard("No applications", "There are no applications yet for jobs owned by this organiser."));
            center.getChildren().add(new HBox(UiTheme.createBackButton(nav)));
        } else {
            ComboBox<JobApplication> applicationBox = new ComboBox<>();
            applicationBox.setPrefWidth(420);
            applicationBox.setPrefHeight(42);
            applicationBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(JobApplication application) {
                    if (application == null) {
                        return "";
                    }
                    return application.applicationId() + " | " + application.jobId() + " | " + application.applicantUserId();
                }

                @Override
                public JobApplication fromString(String string) {
                    return null;
                }
            });
            applicationBox.getItems().addAll(applications);

            String selectedApplicationId = context.selectedApplicationId();
            JobApplication initialSelection = applications.stream()
                .filter(application -> application.applicationId().equals(selectedApplicationId))
                .findFirst()
                .orElse(applications.getFirst());
            applicationBox.getSelectionModel().select(initialSelection);

            VBox detailContainer = new VBox(16);
            TextArea cvContentArea = new TextArea();
            cvContentArea.setEditable(false);
            cvContentArea.setWrapText(true);
            cvContentArea.setPrefRowCount(12);
            cvContentArea.setStyle(
                "-fx-control-inner-background: white;" +
                    "-fx-background-color: white;" +
                    "-fx-background-radius: 24;" +
                    "-fx-border-radius: 24;" +
                    "-fx-border-color: #f4d9e6;" +
                    "-fx-border-width: 2;" +
                    "-fx-font-size: 15px;"
            );

            javafx.scene.control.Label statusLabel = new javafx.scene.control.Label();
            statusLabel.setWrapText(true);
            statusLabel.setTextFill(Color.web("#b00020"));

            applicationBox.valueProperty().addListener((obs, oldValue, newValue) ->
                loadReview(context, newValue, detailContainer, cvContentArea, statusLabel)
            );
            loadReview(context, initialSelection, detailContainer, cvContentArea, statusLabel);

            HBox footer = new HBox(UiTheme.createBackButton(nav));
            footer.setAlignment(Pos.CENTER_LEFT);

            center.getChildren().addAll(applicationBox, detailContainer, cvContentArea, statusLabel, footer);
        }

        BorderPane root = UiTheme.createPage(
            "Application Review",
            UiTheme.createMoSidebar(nav, PageId.APPLICATION_REVIEW),
            center,
            nav,
            context
        );
        return UiTheme.createScene(root);
    }

    private static void loadReview(
        UiAppContext context,
        JobApplication application,
        VBox detailContainer,
        TextArea cvContentArea,
        javafx.scene.control.Label statusLabel
    ) {
        detailContainer.getChildren().clear();
        cvContentArea.clear();
        statusLabel.setText("");

        if (application == null) {
            return;
        }

        try {
            ApplicantCvReview review = context.services().cvReviewService().loadReviewByApplicationId(application.applicationId());
            context.selectApplication(application.applicationId());

            JobPosting job = context.services().jobRepository().findByJobId(application.jobId()).orElse(null);
            String jobTitle = job == null ? application.jobId() : job.title();

            HBox tags = new HBox(16,
                UiTheme.createTag("Application : " + application.applicationId(), 300),
                UiTheme.createTag("Job : " + jobTitle, 320),
                UiTheme.createTag("Status : " + application.status().name(), 220)
            );

            VBox profileCard = UiTheme.createWhiteCard(
                "Applicant profile",
                "Name: " + review.profile().fullName() + System.lineSeparator() +
                    "Student ID: " + review.profile().studentId() + System.lineSeparator() +
                    "Programme: " + review.profile().programme() + System.lineSeparator() +
                    "Year: " + review.profile().yearOfStudy() + System.lineSeparator() +
                    "Education level: " + review.profile().educationLevel()
            );

            VBox skillsCard = UiTheme.createWhiteCard(
                "Skills and availability",
                "Skills: " + String.join(", ", review.profile().skills()) + System.lineSeparator() +
                    System.lineSeparator() +
                    "Availability: " + String.join(", ", review.profile().availabilitySlots()) + System.lineSeparator() +
                    System.lineSeparator() +
                    "Desired positions: " + String.join(", ", review.profile().desiredPositions())
            );

            detailContainer.getChildren().addAll(tags, profileCard, skillsCard);
            cvContentArea.setText(review.cvContent());
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    private static List<JobApplication> loadReviewableApplications(UiAppContext context) {
        Set<String> ownedJobIds = context.services().jobRepository().findAll().stream()
            .filter(job -> job.organiserId().equals(context.session().userId()))
            .map(JobPosting::jobId)
            .collect(Collectors.toSet());

        List<JobApplication> filtered = context.services().applicationRepository().findAll().stream()
            .filter(application -> ownedJobIds.contains(application.jobId()))
            .sorted(Comparator.comparing(JobApplication::applicationId))
            .toList();

        String selectedJobId = context.selectedJobId();
        if (selectedJobId == null) {
            return filtered;
        }

        List<JobApplication> selectedJobApplications = filtered.stream()
            .filter(application -> application.jobId().equals(selectedJobId))
            .toList();
        return selectedJobApplications.isEmpty() ? filtered : selectedJobApplications;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
