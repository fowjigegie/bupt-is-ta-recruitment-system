package UI;

import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobStatus;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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
import javafx.scene.layout.FlowPane;
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
        center.setPadding(new Insets(20, 40, 28, 40));

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
        TextArea descriptionArea = createArea();
        TextArea requirementArea = createArea();
        ObservableList<String> selectedScheduleSlots = FXCollections.observableArrayList();

        if (isEditMode) {
            JobPosting job = editJob.get();
            titleField.setText(job.title());
            moduleField.setText(job.moduleOrActivity());
            weeklyHoursField.setText(Integer.toString(job.weeklyHours()));
            descriptionArea.setText(job.description());
            requirementArea.setText(String.join("; ", job.requiredSkills()));
            selectedScheduleSlots.setAll(job.scheduleSlots());
        }

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setTextFill(Color.web("#b00020"));
        // Avoid leaving blank vertical space when no status message is shown.
        statusLabel.managedProperty().bind(Bindings.createBooleanBinding(
            () -> statusLabel.getText() != null && !statusLabel.getText().isBlank(),
            statusLabel.textProperty()
        ));
        statusLabel.visibleProperty().bind(statusLabel.managedProperty());

        Label titleLabel = new Label(isEditMode ? "Edit Posting" : "New Posting");
        titleLabel.setStyle(
            "-fx-font-size: 32px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #ff66b3;"
        );

        VBox formBox = new VBox(14);
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

        VBox scheduleBox = createScheduleSelectorBox(selectedScheduleSlots);
        VBox descriptionBox = createLabeledAreaBox("Job Description", descriptionArea, 430, 250);
        VBox requirementBox = createLabeledAreaBox("Job Requirements", requirementArea, 430, 250);
        HBox.setHgrow(descriptionBox, Priority.ALWAYS);
        HBox.setHgrow(requirementBox, Priority.ALWAYS);
        HBox detailRow = new HBox(18, descriptionBox, requirementBox);
        detailRow.setAlignment(Pos.CENTER);
        detailRow.setFillHeight(true);

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
                    List.copyOf(selectedScheduleSlots),
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
                    List.copyOf(selectedScheduleSlots),
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
                descriptionArea.setText(job.description());
                requirementArea.setText(String.join("; ", job.requiredSkills()));
                selectedScheduleSlots.setAll(job.scheduleSlots());
                statusLabel.setText("");
            } else {
                titleField.clear();
                moduleField.clear();
                weeklyHoursField.clear();
                descriptionArea.clear();
                requirementArea.clear();
                selectedScheduleSlots.clear();
                statusLabel.setText("");
            }
        });

        HBox actionRow = new HBox(14, clearButton, publishButton);
        actionRow.setAlignment(Pos.CENTER);
        actionRow.setPadding(new Insets(2, 0, 0, 0));

        formBox.getChildren().addAll(
            firstRow,
            metaRow,
            scheduleBox,
            detailRow,
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
        List<String> scheduleSlots,
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
                scheduleSlots
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
        List<String> scheduleSlots,
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
                scheduleSlots,
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
        box.setFillWidth(true);
        box.setMaxWidth(Double.MAX_VALUE);

        Label label = new Label(labelText + " :");
        label.setStyle(
            "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4664a8;"
        );

        area.setPrefSize(width, height);
        area.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().addAll(label, area);
        return box;
    }

    private static VBox createScheduleSelectorBox(ObservableList<String> selectedScheduleSlots) {
        VBox box = new VBox(8);

        Label label = new Label("Schedule Slots :");
        label.setStyle(
            "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4664a8;"
        );

        ComboBox<String> dayBox = new ComboBox<>();
        dayBox.getItems().addAll("MON", "TUE", "WED", "THU", "FRI");
        dayBox.setPromptText("Weekday");
        dayBox.setPrefWidth(130);

        ComboBox<String> startBox = new ComboBox<>();
        startBox.getItems().addAll(
            "08:00", "09:00", "10:00", "11:00", "12:00",
            "13:00", "14:00", "15:00", "16:00", "17:00"
        );
        startBox.setPromptText("Start");
        startBox.setPrefWidth(130);

        ComboBox<String> endBox = new ComboBox<>();
        endBox.getItems().addAll(
            "09:00", "10:00", "11:00", "12:00", "13:00",
            "14:00", "15:00", "16:00", "17:00", "18:00"
        );
        endBox.setPromptText("End");
        endBox.setPrefWidth(130);

        Button addSlotButton = UiTheme.createSoftButton("Add Slot", 110, 42);
        Label helperText = new Label("You can add up to 5 slots.");
        helperText.setStyle("-fx-font-size: 13px; -fx-text-fill: #8b7fa0;");

        FlowPane tagsPane = new FlowPane();
        tagsPane.setHgap(8);
        tagsPane.setVgap(8);
        tagsPane.setPrefWrapLength(760);
        tagsPane.setStyle(
            "-fx-background-color: #fff3f7;" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: #f4d9e6;" +
                "-fx-border-radius: 14;" +
                "-fx-border-width: 2;" +
                "-fx-padding: 10;"
        );

        Runnable[] refreshTagsRef = new Runnable[1];
        refreshTagsRef[0] = () -> {
            tagsPane.getChildren().clear();
            for (String slot : selectedScheduleSlots) {
                Label chipText = new Label(slot);
                chipText.setStyle("-fx-text-fill: #4664a8; -fx-font-weight: bold; -fx-font-size: 13px;");

                Button removeBtn = new Button("×");
                removeBtn.setStyle(
                    "-fx-background-color: transparent;" +
                        "-fx-text-fill: #8a4f7a;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 0 2 0 6;"
                );
                removeBtn.setOnAction(event -> {
                    selectedScheduleSlots.remove(slot);
                    refreshTagsRef[0].run();
                    helperText.setText("You can add up to 5 slots.");
                });

                HBox chip = new HBox(4, chipText, removeBtn);
                chip.setAlignment(Pos.CENTER_LEFT);
                chip.setStyle(
                    "-fx-background-color: #ffe7f3;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: #f3b7db;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-width: 1.2;" +
                        "-fx-padding: 4 8 4 10;"
                );
                tagsPane.getChildren().add(chip);
            }
        };

        addSlotButton.setOnAction(event -> {
            String day = dayBox.getValue();
            String start = startBox.getValue();
            String end = endBox.getValue();
            if (day == null || start == null || end == null) {
                helperText.setText("Please choose weekday, start and end time first.");
                return;
            }
            if (end.compareTo(start) <= 0) {
                helperText.setText("End time must be later than start time.");
                return;
            }
            if (selectedScheduleSlots.size() >= 5) {
                helperText.setText("At most 5 schedule slots are allowed.");
                return;
            }
            if (hasScheduleOverlap(selectedScheduleSlots, day, start, end)) {
                helperText.setText("Invalid slot: overlaps with an existing schedule.");
                return;
            }
            String slot = day + "-" + start + "-" + end;
            if (!selectedScheduleSlots.contains(slot)) {
                selectedScheduleSlots.add(slot);
                refreshTagsRef[0].run();
                helperText.setText("You can add up to 5 slots.");
            } else {
                helperText.setText("This slot already exists.");
            }
        });
        HBox selectorRow = new HBox(12, dayBox, startBox, endBox, addSlotButton);
        selectorRow.setAlignment(Pos.CENTER_LEFT);
        refreshTagsRef[0].run();

        box.getChildren().addAll(label, selectorRow, helperText, tagsPane);
        return box;
    }

    private static boolean hasScheduleOverlap(List<String> existingSlots, String day, String start, String end) {
        for (String slot : existingSlots) {
            String[] parts = slot.split("-");
            if (parts.length != 3) {
                continue;
            }
            if (!day.equals(parts[0])) {
                continue;
            }
            String existingStart = parts[1];
            String existingEnd = parts[2];
            if (start.compareTo(existingEnd) < 0 && existingStart.compareTo(end) < 0) {
                return true;
            }
        }
        return false;
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
        area.setPrefRowCount(5);
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
