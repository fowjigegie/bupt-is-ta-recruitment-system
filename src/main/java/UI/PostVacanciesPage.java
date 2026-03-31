package UI;

import com.bupt.tarecruitment.job.JobPosting;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.List;

public class PostVacanciesPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.POST_VACANCIES, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        VBox center = new VBox(22);
        center.setPadding(new Insets(30, 40, 30, 40));

        TextField titleField = createField("Course title / job title");
        TextField organiserField = createField("Module organiser");
        organiserField.setText(context.session().userId());
        organiserField.setEditable(false);

        TextField moduleField = createField("Classes in need of assistance / module");
        TextField weeklyHoursField = createField("Weekly hours");
        TextField scheduleField = createField("Schedule slots, separated by ';' or ','");
        TextArea descriptionArea = createArea("Describe the work expected from the TA.");
        TextArea requirementArea = createArea("List required skills, separated by ';' or ','.");

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setTextFill(Color.web("#b00020"));

        HBox tagRow = new HBox(16,
            UiTheme.createTag("Role: Module Organiser", 260),
            UiTheme.createTag("Organiser: " + context.session().userId(), 260),
            UiTheme.createTag("Posts will be saved to data/jobs.txt", 330)
        );

        var publishButton = UiTheme.createPrimaryButton("Publish vacancy", 240, 70);
        publishButton.setOnAction(event -> publish(
            context,
            titleField,
            organiserField,
            moduleField,
            weeklyHoursField,
            scheduleField,
            descriptionArea,
            requirementArea,
            statusLabel
        ));

        var clearButton = UiTheme.createSoftButton("Clear", 110, 46);
        clearButton.setOnAction(event -> {
            titleField.clear();
            moduleField.clear();
            weeklyHoursField.clear();
            scheduleField.clear();
            descriptionArea.clear();
            requirementArea.clear();
            statusLabel.setText("");
        });

        HBox footer = new HBox(16, UiTheme.createBackButton(nav), publishButton, clearButton);
        footer.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        center.getChildren().addAll(
            UiTheme.createPageHeading("Post vacancies"),
            UiTheme.createMutedText("This page now publishes real jobs through JobPostingService."),
            tagRow,
            titleField,
            organiserField,
            moduleField,
            weeklyHoursField,
            scheduleField,
            descriptionArea,
            requirementArea,
            statusLabel,
            spacer,
            footer
        );

        BorderPane root = UiTheme.createPage(
            "Post Vacancies",
            UiTheme.createMoSidebar(nav, PageId.POST_VACANCIES),
            center,
            nav,
            context
        );
        return UiTheme.createScene(root);
    }

    private static void publish(
        UiAppContext context,
        TextField titleField,
        TextField organiserField,
        TextField moduleField,
        TextField weeklyHoursField,
        TextField scheduleField,
        TextArea descriptionArea,
        TextArea requirementArea,
        Label statusLabel
    ) {
        statusLabel.setTextFill(Color.web("#b00020"));
        statusLabel.setText("");

        final int weeklyHours;
        try {
            weeklyHours = Integer.parseInt(weeklyHoursField.getText().trim());
        } catch (NumberFormatException exception) {
            statusLabel.setText("Weekly hours must be an integer.");
            return;
        }

        try {
            JobPosting posting = context.services().jobPostingService().publish(
                organiserField.getText().trim(),
                titleField.getText().trim(),
                moduleField.getText().trim(),
                descriptionArea.getText().trim(),
                splitList(requirementArea.getText()),
                weeklyHours,
                splitList(scheduleField.getText())
            );
            context.selectJob(posting.jobId());
            statusLabel.setTextFill(Color.web("#2e7d32"));
            statusLabel.setText("Published successfully: " + posting.jobId());
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    private static List<String> splitList(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }

        return List.of(rawValue.replace(',', ';').split(";")).stream()
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList();
    }

    private static TextField createField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefHeight(52);
        field.setStyle(
            "-fx-background-color: white;" +
                "-fx-background-radius: 24;" +
                "-fx-border-radius: 24;" +
                "-fx-border-color: #f4d9e6;" +
                "-fx-border-width: 2;" +
                "-fx-padding: 0 18 0 18;" +
                "-fx-font-size: 15px;"
        );
        return field;
    }

    private static TextArea createArea(String prompt) {
        TextArea area = new TextArea();
        area.setPromptText(prompt);
        area.setPrefRowCount(4);
        area.setWrapText(true);
        area.setStyle(
            "-fx-control-inner-background: white;" +
                "-fx-background-color: white;" +
                "-fx-background-radius: 24;" +
                "-fx-border-radius: 24;" +
                "-fx-border-color: #f4d9e6;" +
                "-fx-border-width: 2;" +
                "-fx-font-size: 15px;"
        );
        return area;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
