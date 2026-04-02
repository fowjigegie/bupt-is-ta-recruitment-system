package UI;

import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobStatus;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
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
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

public class PostVacanciesPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.POST_VACANCIES, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        VBox center = new VBox(22);
        center.setPadding(new Insets(35, 40, 28, 40));

        Optional<JobPosting> editJob = Optional.ofNullable(context.editingJobId())
            .flatMap(jobId -> context.services().jobRepository().findByJobId(jobId))
            .filter(job -> job.organiserId().equals(context.session().userId()))
            .filter(job -> {
                if (job.status() == JobStatus.CLOSED) {
                    // Closed postings are not editable; fall back to "new posting".
                    context.clearJobEdit();
                    return false;
                }
                return true;
            });
        boolean isEditMode = editJob.isPresent();

        TextField titleField = createField();
        TextField organiserField = createField();
        organiserField.setText(context.session().userId());
        organiserField.setEditable(false);

        TextField moduleField = createField();
        TextField weeklyHoursField = createField();
        TextField scheduleField = createField();
        TextArea descriptionArea = createArea();
        TextArea requirementArea = createArea();

        if (isEditMode) {
            JobPosting job = editJob.get();
            titleField.setText(job.title());
            moduleField.setText(job.moduleOrActivity());
            weeklyHoursField.setText(Integer.toString(job.weeklyHours()));
            scheduleField.setText(String.join("; ", job.scheduleSlots()));
            descriptionArea.setText(job.description());
            requirementArea.setText(String.join("; ", job.requiredSkills()));
        }

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setTextFill(Color.web("#b00020"));

        Label titleLabel = new Label(isEditMode ? "Edit Posting" : "New Posting");
        titleLabel.setStyle(
            "-fx-font-size: 32px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #ff66b3;"
        );

        VBox formBox = new VBox(18);
        formBox.setPadding(new Insets(28));
        formBox.setPrefWidth(930);
        formBox.setBackground(new Background(
            new BackgroundFill(Color.WHITE, new CornerRadii(26), Insets.EMPTY)
        ));
        formBox.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(26),
            new BorderWidths(2)
        )));

        HBox firstRow = new HBox(18,
            createLabeledFieldBox("Course Title", titleField, 390),
            createLabeledFieldBox("Taught By", organiserField, 390)
        );

        HBox metaRow = new HBox(18,
            createLabeledFieldBox("Classes in Need of Assistance", moduleField, 390),
            createLabeledFieldBox("Weekly Hours", weeklyHoursField, 390)
        );

        VBox scheduleBox = createLabeledFieldBox("Schedule Slots", scheduleField, 798);
        VBox descriptionBox = createLabeledAreaBox("Job Description", descriptionArea, 798, 130);
        VBox requirementBox = createLabeledAreaBox("Job Requirements", requirementArea, 798, 150);

        var publishButton = UiTheme.createPrimaryButton(isEditMode ? "Update" : "Publish", 180, 52);
        publishButton.setOnAction(event -> {
            if (isEditMode) {
                update(
                    context,
                    editJob.get(),
                    titleField,
                    organiserField,
                    moduleField,
                    weeklyHoursField,
                    scheduleField,
                    descriptionArea,
                    requirementArea,
                    statusLabel
                );
            } else {
                publish(
                    context,
                    titleField,
                    organiserField,
                    moduleField,
                    weeklyHoursField,
                    scheduleField,
                    descriptionArea,
                    requirementArea,
                    statusLabel
                );
            }
        });

        var clearButton = UiTheme.createSoftButton("Clear", 110, 46);
        clearButton.setOnAction(event -> {
            if (isEditMode) {
                JobPosting job = editJob.get();
                titleField.setText(job.title());
                moduleField.setText(job.moduleOrActivity());
                weeklyHoursField.setText(Integer.toString(job.weeklyHours()));
                scheduleField.setText(String.join("; ", job.scheduleSlots()));
                descriptionArea.setText(job.description());
                requirementArea.setText(String.join("; ", job.requiredSkills()));
                statusLabel.setText("");
            } else {
                titleField.clear();
                moduleField.clear();
                weeklyHoursField.clear();
                scheduleField.clear();
                descriptionArea.clear();
                requirementArea.clear();
                statusLabel.setText("");
            }
        });

        HBox actionRow = new HBox(12, clearButton, publishButton);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        formBox.getChildren().addAll(
            firstRow,
            metaRow,
            scheduleBox,
            descriptionBox,
            requirementBox,
            statusLabel,
            actionRow
        );

        HBox footer = new HBox(UiTheme.createBackButton(nav));
        footer.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        center.getChildren().addAll(
            titleLabel,
            formBox,
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

    private static void update(
        UiAppContext context,
        JobPosting existingJob,
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
            JobPosting updated = new JobPosting(
                existingJob.jobId(),
                organiserField.getText().trim(),
                titleField.getText().trim(),
                moduleField.getText().trim(),
                descriptionArea.getText().trim(),
                splitList(requirementArea.getText()),
                weeklyHours,
                splitList(scheduleField.getText()),
                existingJob.status() == null ? JobStatus.OPEN : existingJob.status()
            );

            context.services().jobPostingService().publish(updated);
            context.selectJob(updated.jobId());
            context.clearJobEdit();
            statusLabel.setTextFill(Color.web("#2e7d32"));
            statusLabel.setText("Updated successfully: " + updated.jobId());
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

    private static VBox createLabeledFieldBox(String labelText, TextField field, double width) {
        VBox box = new VBox(8);

        Label label = new Label(labelText + " :");
        label.setStyle(
            "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4664a8;"
        );

        field.setPrefWidth(width);
        box.getChildren().addAll(label, field);
        return box;
    }

    private static VBox createLabeledAreaBox(String labelText, TextArea area, double width, double height) {
        VBox box = new VBox(8);

        Label label = new Label(labelText + " :");
        label.setStyle(
            "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4664a8;"
        );

        area.setPrefSize(width, height);
        box.getChildren().addAll(label, area);
        return box;
    }

    private static TextField createField() {
        TextField field = new TextField();
        field.setPrefHeight(52);
        field.setStyle(
            "-fx-background-color: #fff3f7;" +
                "-fx-background-radius: 20;" +
                "-fx-border-color: #f4d9e6;" +
                "-fx-border-radius: 20;" +
                "-fx-border-width: 2;" +
                "-fx-font-size: 16px;"
        );
        return field;
    }

    private static TextArea createArea() {
        TextArea area = new TextArea();
        area.setPrefRowCount(4);
        area.setWrapText(true);
        area.setStyle(
            "-fx-control-inner-background: #fff3f7;" +
                "-fx-background-color: #fff3f7;" +
                "-fx-background-radius: 20;" +
                "-fx-border-color: #ffd6e8;" +
                "-fx-border-radius: 20;" +
                "-fx-border-width: 2;" +
                "-fx-font-size: 16px;"
        );
        return area;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
