package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.common.skill.SkillCatalog;
import com.bupt.tarecruitment.common.schedule.ScheduleSlot;
import com.bupt.tarecruitment.common.text.DisplayFormats;
import com.bupt.tarecruitment.job.JobActivityType;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobStatus;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 封装岗位发布页的表单控件和排期选择状态。
 */
final class PostVacancyForm {
    private final TextField titleField;
    private final TextField organiserField;
    private final TextField moduleField;
    private final ComboBox<String> activityTypeBox;
    private final TextField weeklyHoursField;
    private final TextArea descriptionArea;
    private final ObservableList<String> selectedScheduleSlots;
    private final LinkedHashSet<String> selectedSkills;
    private final Map<String, List<String>> categorizedSkillSuggestions;
    private final List<Runnable> changeListeners = new ArrayList<>();
    private ComboBox<String> dayBox;
    private ComboBox<String> timeBandBox;
    private FlowPane selectedSkillsPane;
    private Label skillsHelperLabel;

    private PostVacancyForm(
        TextField titleField,
        TextField organiserField,
        TextField moduleField,
        ComboBox<String> activityTypeBox,
        TextField weeklyHoursField,
        TextArea descriptionArea,
        ObservableList<String> selectedScheduleSlots,
        LinkedHashSet<String> selectedSkills,
        Map<String, List<String>> categorizedSkillSuggestions
    ) {
        this.titleField = titleField;
        this.organiserField = organiserField;
        this.moduleField = moduleField;
        this.activityTypeBox = activityTypeBox;
        this.weeklyHoursField = weeklyHoursField;
        this.descriptionArea = descriptionArea;
        this.selectedScheduleSlots = selectedScheduleSlots;
        this.selectedSkills = selectedSkills;
        this.categorizedSkillSuggestions = categorizedSkillSuggestions;

        titleField.textProperty().addListener((obs, oldValue, newValue) -> notifyChanged());
        moduleField.textProperty().addListener((obs, oldValue, newValue) -> notifyChanged());
        weeklyHoursField.textProperty().addListener((obs, oldValue, newValue) -> notifyChanged());
        descriptionArea.textProperty().addListener((obs, oldValue, newValue) -> notifyChanged());
        activityTypeBox.valueProperty().addListener((obs, oldValue, newValue) -> notifyChanged());
        selectedScheduleSlots.addListener((ListChangeListener<String>) change -> notifyChanged());
    }

    static PostVacancyForm create(String organiserUserId, Map<String, List<String>> categorizedSkillSuggestions) {
        TextField titleField = createField();
        TextField organiserField = createField();
        organiserField.setText(organiserUserId);
        organiserField.setEditable(false);
        ComboBox<String> activityTypeBox = createComboBox(250);
        activityTypeBox.getItems().addAll(JobActivityType.values());
        activityTypeBox.getSelectionModel().select(JobActivityType.OTHER);

        return new PostVacancyForm(
            titleField,
            organiserField,
            createField(),
            activityTypeBox,
            createField(),
            createArea(),
            FXCollections.observableArrayList(),
            new LinkedHashSet<>(),
            categorizedSkillSuggestions
        );
    }

    void load(JobPosting job) {
        titleField.setText(job.title());
        moduleField.setText(job.moduleOrActivity());
        activityTypeBox.getSelectionModel().select(JobActivityType.normalize(job.activityType()));
        weeklyHoursField.setText(DisplayFormats.formatDecimal(job.weeklyHours()));
        descriptionArea.setText(job.description());
        selectedScheduleSlots.setAll(job.scheduleSlots());
        setSkills(job.requiredSkills());
        clearPendingScheduleSelection();
        notifyChanged();
    }

    void clearForCreate() {
        titleField.clear();
        moduleField.clear();
        activityTypeBox.getSelectionModel().select(JobActivityType.OTHER);
        weeklyHoursField.clear();
        descriptionArea.clear();
        selectedScheduleSlots.clear();
        selectedSkills.clear();
        refreshSelectedSkillsPane();
        clearPendingScheduleSelection();
        notifyChanged();
    }

    String organiserId() {
        return organiserField.getText().trim();
    }

    String title() {
        return titleField.getText().trim();
    }

    String moduleOrActivity() {
        return moduleField.getText().trim();
    }

    String activityType() {
        return JobActivityType.normalize(activityTypeBox.getValue());
    }

    String description() {
        return descriptionArea.getText().trim();
    }

    List<String> requiredSkills() {
        return List.copyOf(selectedSkills);
    }

    double parseWeeklyHours() {
        return Double.parseDouble(weeklyHoursField.getText().trim());
    }

    List<String> scheduleSlots() {
        List<String> slots = FXCollections.observableArrayList(selectedScheduleSlots);
        pendingScheduleSlot().ifPresent(slot -> {
            if (!slots.contains(slot) && !hasScheduleOverlap(slots, slot)) {
                slots.add(slot);
            }
        });
        return List.copyOf(slots);
    }

    void onChange(Runnable listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    JobPosting toDraftJobPosting(String draftJobId, JobStatus status, double fallbackWeeklyHours) {
        double weeklyHours = fallbackWeeklyHours;
        try {
            weeklyHours = parseWeeklyHours();
        } catch (NumberFormatException ignored) {
            // Keep fallback value so the quality assistant can still evaluate other fields.
        }

        return new JobPosting(
            draftJobId,
            organiserId(),
            title(),
            moduleOrActivity(),
            activityType(),
            description(),
            requiredSkills(),
            weeklyHours,
            scheduleSlots(),
            status
        );
    }

    private void notifyChanged() {
        for (Runnable listener : changeListeners) {
            listener.run();
        }
    }

    HBox createFirstRow() {
        return new HBox(18,
            createLabeledFieldBox("Course Title", titleField, 390),
            createLabeledFieldBox("Taught By", organiserField, 390)
        );
    }

    HBox createMetaRow() {
        return new HBox(18,
            createLabeledFieldBox("Classes in Need of Assistance", moduleField, 300),
            createLabeledComboBox("Activity Type", activityTypeBox, 250),
            createLabeledFieldBox("Weekly Hours", weeklyHoursField, 210)
        );
    }

    HBox createDetailRow() {
        VBox descriptionBox = createLabeledAreaBox("Job Description", descriptionArea, 430, 250);
        VBox requirementBox = createRequiredSkillsSection();
        HBox.setHgrow(descriptionBox, Priority.ALWAYS);
        HBox.setHgrow(requirementBox, Priority.ALWAYS);
        HBox detailRow = new HBox(18, descriptionBox, requirementBox);
        detailRow.setAlignment(Pos.CENTER);
        detailRow.setFillHeight(true);
        return detailRow;
    }

    private VBox createRequiredSkillsSection() {
        Label label = new Label("Required Skills :");
        label.setStyle(
            "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4664a8;"
        );

        skillsHelperLabel = new Label(
            selectedSkills.isEmpty()
                ? "Choose the skills applicants should have for this job."
                : "Selected required skills are shown below. Open the selector to search, edit, or add more."
        );
        skillsHelperLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #8b7fa0;");
        skillsHelperLabel.setWrapText(true);

        selectedSkillsPane = new FlowPane();
        selectedSkillsPane.setHgap(8);
        selectedSkillsPane.setVgap(8);
        selectedSkillsPane.setPrefWrapLength(380);

        Button chooseSkillsButton = UiTheme.createOutlineButton("Choose required skills", 220, 46);
        chooseSkillsButton.setOnAction(event -> {
            List<String> chosenSkills = SkillPickerDialog.chooseSkills(
                chooseSkillsButton.getScene() == null ? null : chooseSkillsButton.getScene().getWindow(),
                "Required Skills",
                "Required Skills",
                "Search across all categories, expand any category you want, or type a custom skill to add it as a requirement for this job.",
                "Use selected skills",
                categorizedSkillSuggestions,
                requiredSkills()
            );
            setSkills(chosenSkills);
        });

        refreshSelectedSkillsPane();

        VBox box = new VBox(10, label, skillsHelperLabel, selectedSkillsPane, chooseSkillsButton);
        box.setPadding(new Insets(12));
        box.setStyle(
            "-fx-background-color: white;" +
                "-fx-background-radius: 20;" +
                "-fx-border-color: #f3b2df;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 20;"
        );
        box.setPrefWidth(430);
        box.setMinHeight(250);
        return box;
    }

    VBox createScheduleSelectorBox() {
        VBox box = new VBox(8);

        Label label = new Label("Schedule Slots :");
        label.setStyle(
            "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4664a8;"
        );

        dayBox = new ComboBox<>();
        dayBox.getItems().addAll(FixedScheduleBands.WEEKDAY_CODES);
        dayBox.setPromptText("Weekday");
        dayBox.setPrefWidth(150);

        timeBandBox = new ComboBox<>();
        timeBandBox.getItems().addAll(FixedScheduleBands.timeBandLabels());
        timeBandBox.setPromptText("Time slot");
        timeBandBox.setPrefWidth(210);

        Button addSlotButton = UiTheme.createSoftButton("Add Slot", 110, 42);
        Label helperText = new Label("Choose a weekday and one of the fixed teaching time bands. You can add up to 5 slots.");
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
                Label chipText = new Label(FixedScheduleBands.formatSlotForDisplay(slot));
                chipText.setStyle("-fx-text-fill: #4664a8; -fx-font-weight: bold; -fx-font-size: 13px;");

                Button removeBtn = new Button("脳");
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
            Optional<String> pendingSlot = pendingScheduleSlot();
            if (pendingSlot.isEmpty()) {
                helperText.setText("Please choose weekday and time slot first.");
                return;
            }
            if (selectedScheduleSlots.size() >= 5) {
                helperText.setText("At most 5 schedule slots are allowed.");
                return;
            }
            String slot = pendingSlot.get();
            if (hasScheduleOverlap(selectedScheduleSlots, slot)) {
                helperText.setText("Invalid slot: overlaps with an existing schedule.");
                return;
            }
            if (!selectedScheduleSlots.contains(slot)) {
                selectedScheduleSlots.add(slot);
                clearPendingScheduleSelection();
                refreshTagsRef[0].run();
                helperText.setText("Choose a weekday and one of the fixed teaching time bands. You can add up to 5 slots. A currently selected slot will also be saved when you publish.");
            } else {
                helperText.setText("This slot already exists.");
            }
        });

        HBox selectorRow = new HBox(12, dayBox, timeBandBox, addSlotButton);
        selectorRow.setAlignment(Pos.CENTER_LEFT);
        refreshTagsRef[0].run();

        box.getChildren().addAll(label, selectorRow, helperText, tagsPane);
        return box;
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

    private void setSkills(List<String> skills) {
        selectedSkills.clear();
        for (String skill : skills) {
            String normalized = normalizeDisplaySkill(skill);
            if (!normalized.isBlank()) {
                selectedSkills.add(normalized);
            }
        }
        refreshSelectedSkillsPane();
        notifyChanged();
    }

    private void refreshSelectedSkillsPane() {
        if (selectedSkillsPane == null) {
            return;
        }
        selectedSkillsPane.getChildren().clear();
        if (selectedSkills.isEmpty()) {
            selectedSkillsPane.getChildren().add(UiTheme.createMutedText("No required skills selected yet."));
        } else {
            for (String skill : selectedSkills) {
                Label tag = new Label(skill);
                tag.setPadding(new Insets(6, 10, 6, 10));
                tag.setStyle(
                    "-fx-background-color: #ffe6f2;" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: #f3b2df;" +
                        "-fx-border-radius: 18;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #5c3f6b;"
                );
                selectedSkillsPane.getChildren().add(tag);
            }
        }

        if (skillsHelperLabel != null) {
            skillsHelperLabel.setText(
                selectedSkills.isEmpty()
                    ? "Choose the skills applicants should have for this job."
                    : "Selected required skills are shown below. Open the selector to search, edit, or add more."
            );
        }
    }

    private static String normalizeDisplaySkill(String rawSkill) {
        if (rawSkill == null || rawSkill.isBlank()) {
            return "";
        }
        String normalized = SkillCatalog.normalize(rawSkill);
        if (normalized.isBlank()) {
            return "";
        }
        return List.of(normalized.split(" ")).stream()
            .filter(part -> !part.isBlank())
            .map(part -> part.length() <= 2
                ? part.toUpperCase()
                : Character.toUpperCase(part.charAt(0)) + part.substring(1).toLowerCase())
            .reduce((left, right) -> left + " " + right)
            .orElse("");
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

    private static VBox createLabeledComboBox(String labelText, ComboBox<String> comboBox, double width) {
        VBox box = new VBox(8);

        Label label = new Label(labelText + " :");
        label.setStyle(
            "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4664a8;"
        );

        comboBox.setPrefWidth(width);
        box.getChildren().addAll(label, comboBox);
        return box;
    }

    private static boolean hasScheduleOverlap(List<String> existingSlots, String candidateSlot) {
        try {
            ScheduleSlot candidate = ScheduleSlot.parse(candidateSlot);
            for (String slot : existingSlots) {
                try {
                    if (candidate.overlaps(ScheduleSlot.parse(slot))) {
                        return true;
                    }
                } catch (IllegalArgumentException ignored) {
                    // Ignore malformed historic schedule strings in overlap checks.
                }
            }
            return false;
        } catch (IllegalArgumentException exception) {
            return true;
        }
    }

    private Optional<String> pendingScheduleSlot() {
        if (dayBox == null || timeBandBox == null) {
            return Optional.empty();
        }
        String day = dayBox.getValue();
        String bandLabel = timeBandBox.getValue();
        if (day == null || bandLabel == null) {
            return Optional.empty();
        }
        return FixedScheduleBands.bandForLabel(bandLabel)
            .map(band -> FixedScheduleBands.toSlotValue(day, band));
    }

    private void clearPendingScheduleSelection() {
        if (dayBox != null) {
            dayBox.getSelectionModel().clearSelection();
        }
        if (timeBandBox != null) {
            timeBandBox.getSelectionModel().clearSelection();
        }
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

    private static ComboBox<String> createComboBox(double width) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setPrefWidth(width);
        comboBox.setPrefHeight(52);
        comboBox.setStyle(
            "-fx-background-color: #fff3f7;" +
                "-fx-background-radius: 20;" +
                "-fx-border-color: #f4d9e6;" +
                "-fx-border-radius: 20;" +
                "-fx-border-width: 2;" +
                "-fx-font-size: 15px;"
        );
        return comboBox;
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
}
