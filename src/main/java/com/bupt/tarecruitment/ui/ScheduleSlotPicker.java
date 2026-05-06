package com.bupt.tarecruitment.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * 提供 applicant availability 的固定时间段选择器。
 */
final class ScheduleSlotPicker {
    private static final int MAX_SLOT_COUNT = FixedScheduleBands.WEEKDAY_CODES.size() * FixedScheduleBands.timeBands().size();

    private final ObservableList<String> selectedSlots;
    private final VBox container;
    private final VBox compactContainer;
    private final Label helperText;
    private final Label compactSummaryText;
    private final FlowPane tagsPane;
    private final ComboBox<String> dayBox;
    private final ComboBox<String> timeBandBox;
    private final String defaultHelperText;

    private ScheduleSlotPicker(
        ObservableList<String> selectedSlots,
        VBox container,
        VBox compactContainer,
        Label helperText,
        Label compactSummaryText,
        FlowPane tagsPane,
        ComboBox<String> dayBox,
        ComboBox<String> timeBandBox,
        String defaultHelperText
    ) {
        this.selectedSlots = selectedSlots;
        this.container = container;
        this.compactContainer = compactContainer;
        this.helperText = helperText;
        this.compactSummaryText = compactSummaryText;
        this.tagsPane = tagsPane;
        this.dayBox = dayBox;
        this.timeBandBox = timeBandBox;
        this.defaultHelperText = defaultHelperText;
    }

    static ScheduleSlotPicker create(String labelText, String defaultHelperText) {
        Label label = new Label(labelText);
        label.setStyle(
            "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4664a8;"
        );

        ComboBox<String> dayBox = new ComboBox<>();
        dayBox.getItems().addAll(FixedScheduleBands.WEEKDAY_CODES);
        dayBox.setPromptText("Weekday");
        dayBox.setPrefWidth(130);

        ComboBox<String> timeBandBox = new ComboBox<>();
        timeBandBox.getItems().addAll(FixedScheduleBands.timeBandLabels());
        timeBandBox.setPromptText("Time slot");
        timeBandBox.setPrefWidth(180);

        Button addSlotButton = UiTheme.createSoftButton("Add Slot", 110, 42);
        Label helperText = new Label(defaultHelperText);
        helperText.setStyle("-fx-font-size: 13px; -fx-text-fill: #8b7fa0;");

        FlowPane tagsPane = new FlowPane();
        tagsPane.setHgap(8);
        tagsPane.setVgap(8);
        tagsPane.setPrefWrapLength(520);
        tagsPane.setStyle(
            "-fx-background-color: #fff3f7;" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: #f4d9e6;" +
                "-fx-border-radius: 14;" +
                "-fx-border-width: 2;" +
                "-fx-padding: 10;"
        );

        HBox selectorRow = new HBox(12, dayBox, timeBandBox, addSlotButton);
        selectorRow.setAlignment(Pos.CENTER_LEFT);

        VBox container = new VBox(8, label, selectorRow, helperText, tagsPane);
        Label compactLabel = new Label(labelText);
        compactLabel.setStyle(
            "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4664a8;"
        );
        Label compactSummaryText = new Label("No slots selected.");
        compactSummaryText.setStyle(
            "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4664a8;"
        );
        Button editTimetableButton = UiTheme.createOutlineButton("Edit timetable", 170, 42);
        Region compactSpacer = new Region();
        HBox.setHgrow(compactSpacer, Priority.ALWAYS);
        HBox compactRow = new HBox(12, compactSummaryText, compactSpacer, editTimetableButton);
        compactRow.setAlignment(Pos.CENTER_LEFT);

        VBox compactContainer = new VBox(8, compactLabel, compactRow);
        compactContainer.setPadding(new Insets(12));
        compactContainer.setStyle(
            "-fx-background-color: #fff8fb;" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: #f3b2df;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 18;"
        );

        ObservableList<String> selectedSlots = FXCollections.observableArrayList();
        ScheduleSlotPicker picker = new ScheduleSlotPicker(
            selectedSlots,
            container,
            compactContainer,
            helperText,
            compactSummaryText,
            tagsPane,
            dayBox,
            timeBandBox,
            defaultHelperText
        );
        addSlotButton.setOnAction(event -> picker.addSelectedSlot());
        editTimetableButton.setOnAction(event -> picker.openTimetableDialog());
        picker.refreshTags();
        return picker;
    }

    VBox container() {
        return container;
    }

    VBox compactContainer() {
        return compactContainer;
    }

    List<String> slots() {
        return List.copyOf(selectedSlots);
    }

    String formattedSlots() {
        return selectedSlots.stream()
            .map(FixedScheduleBands::formatSlotForDisplay)
            .reduce((left, right) -> left + "; " + right)
            .orElse("");
    }

    void setSlots(List<String> slots) {
        selectedSlots.setAll(FixedScheduleBands.normalizeToFixedBandSlots(slots));
        refreshTags();
        helperText.setText(defaultHelperText);
    }

    private void addSelectedSlot() {
        String day = dayBox.getValue();
        String bandLabel = timeBandBox.getValue();

        if (day == null || bandLabel == null) {
            helperText.setText("Please choose weekday and time slot first.");
            return;
        }

        if (selectedSlots.size() >= MAX_SLOT_COUNT) {
            helperText.setText("All fixed teaching time bands are already selected.");
            return;
        }

        Optional<FixedScheduleBands.TimeBand> selectedBand = FixedScheduleBands.bandForLabel(bandLabel);
        if (selectedBand.isEmpty()) {
            helperText.setText("This time slot is not supported.");
            return;
        }

        String slot = FixedScheduleBands.toSlotValue(day, selectedBand.get());
        if (selectedSlots.contains(slot)) {
            helperText.setText("This slot already exists.");
            return;
        }

        selectedSlots.add(slot);
        selectedSlots.setAll(FixedScheduleBands.normalizeToFixedBandSlots(selectedSlots));
        dayBox.getSelectionModel().clearSelection();
        timeBandBox.getSelectionModel().clearSelection();
        helperText.setText(defaultHelperText);
        refreshTags();
    }

    private void refreshTags() {
        tagsPane.getChildren().clear();
        for (String slot : selectedSlots) {
            Label chipText = new Label(FixedScheduleBands.formatSlotForDisplay(slot));
            chipText.setStyle("-fx-text-fill: #4664a8; -fx-font-weight: bold; -fx-font-size: 13px;");

            Button removeButton = new Button("x");
            removeButton.setStyle(
                "-fx-background-color: transparent;" +
                    "-fx-text-fill: #8a4f7a;" +
                    "-fx-font-size: 14px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-cursor: hand;" +
                    "-fx-padding: 0 2 0 6;"
            );
            removeButton.setOnAction(event -> {
                selectedSlots.remove(slot);
                refreshTags();
                helperText.setText(defaultHelperText);
            });

            HBox chip = new HBox(4, chipText, removeButton);
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
        refreshCompactSummary();
    }

    private void refreshCompactSummary() {
        int count = selectedSlots.size();
        compactSummaryText.setText(count == 0 ? "No slots selected." : count + " slots selected.");
    }

    private void openTimetableDialog() {
        LinkedHashSet<String> draftSlots = new LinkedHashSet<>(selectedSlots);

        Stage dialog = new Stage();
        if (compactContainer.getScene() != null && compactContainer.getScene().getWindow() != null) {
            dialog.initOwner(compactContainer.getScene().getWindow());
        }
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Edit availability");

        Label title = new Label("Edit availability");
        title.setStyle(
            "-fx-font-family: Arial;" +
                "-fx-font-size: 22px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4664a8;"
        );
        Label countLabel = UiTheme.createMutedText("");
        updateCountLabel(countLabel, draftSlots.size());

        GridPane timetable = createTimetableGrid(draftSlots, countLabel);

        Button clearButton = UiTheme.createOutlineButton("Clear", 110, 40);
        clearButton.setOnAction(event -> {
            draftSlots.clear();
            rebuildTimetableGrid(timetable, draftSlots, countLabel);
            updateCountLabel(countLabel, draftSlots.size());
        });

        Button cancelButton = UiTheme.createOutlineButton("Cancel", 110, 40);
        cancelButton.setOnAction(event -> dialog.close());

        Button doneButton = UiTheme.createSoftButton("Done", 110, 40);
        doneButton.setOnAction(event -> {
            selectedSlots.setAll(FixedScheduleBands.normalizeToFixedBandSlots(new ArrayList<>(draftSlots)));
            helperText.setText(defaultHelperText);
            refreshTags();
            dialog.close();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(12, clearButton, spacer, cancelButton, doneButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(14, title, countLabel, timetable, actions);
        root.setPadding(new Insets(22));
        root.setFillWidth(true);
        root.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #fff8fb, #fff9e8);"
        );

        dialog.setMinWidth(880);
        dialog.setMinHeight(680);
        dialog.setScene(new javafx.scene.Scene(root, 900, 700));
        dialog.showAndWait();
    }

    private static GridPane createTimetableGrid(LinkedHashSet<String> draftSlots, Label countLabel) {
        GridPane grid = new GridPane();
        grid.setHgap(0);
        grid.setVgap(0);
        grid.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints timeColumn = new ColumnConstraints();
        timeColumn.setPrefWidth(116);
        grid.getColumnConstraints().add(timeColumn);
        for (int i = 0; i < FixedScheduleBands.WEEKDAY_CODES.size(); i++) {
            ColumnConstraints dayColumn = new ColumnConstraints();
            dayColumn.setPercentWidth(17.6);
            grid.getColumnConstraints().add(dayColumn);
        }

        rebuildTimetableGrid(grid, draftSlots, countLabel);
        return grid;
    }

    private static void rebuildTimetableGrid(
        GridPane grid,
        LinkedHashSet<String> draftSlots,
        Label countLabel
    ) {
        grid.getChildren().clear();
        grid.add(createHeaderCell("Time"), 0, 0);
        List<String> dayLabels = FixedScheduleBands.WEEKDAY_CODES.stream()
            .map(code -> FixedScheduleBands.weekdayLabels().getOrDefault(code, code))
            .toList();
        for (int dayIndex = 0; dayIndex < dayLabels.size(); dayIndex++) {
            grid.add(createHeaderCell(dayLabels.get(dayIndex)), dayIndex + 1, 0);
        }

        for (int rowIndex = 0; rowIndex < FixedScheduleBands.timeBands().size(); rowIndex++) {
            FixedScheduleBands.TimeBand band = FixedScheduleBands.timeBands().get(rowIndex);
            grid.add(createTimeCell(band.label()), 0, rowIndex + 1);

            for (int dayIndex = 0; dayIndex < FixedScheduleBands.WEEKDAY_CODES.size(); dayIndex++) {
                String dayCode = FixedScheduleBands.WEEKDAY_CODES.get(dayIndex);
                String slot = FixedScheduleBands.toSlotValue(dayCode, band);
                Button cell = createSlotCell(draftSlots.contains(slot));
                cell.setOnAction(event -> {
                    if (draftSlots.contains(slot)) {
                        draftSlots.remove(slot);
                    } else {
                        draftSlots.add(slot);
                    }
                    updateSlotCellStyle(cell, draftSlots.contains(slot));
                    updateCountLabel(countLabel, draftSlots.size());
                });
                grid.add(cell, dayIndex + 1, rowIndex + 1);
            }
        }
    }

    private static StackPane createHeaderCell(String text) {
        Label label = new Label(text);
        label.setStyle(
            "-fx-font-family: Arial;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4d588f;"
        );

        StackPane cell = new StackPane(label);
        cell.setAlignment(Pos.CENTER);
        cell.setMinHeight(42);
        cell.setStyle(
            "-fx-background-color: #dde4ff;" +
                "-fx-border-color: #9faeff;" +
                "-fx-border-width: 1;"
        );
        GridPane.setHalignment(cell, HPos.CENTER);
        return cell;
    }

    private static StackPane createTimeCell(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle(
            "-fx-font-family: Arial;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #3f4370;"
        );

        StackPane cell = new StackPane(label);
        cell.setAlignment(Pos.CENTER);
        cell.setMinHeight(58);
        cell.setPadding(new Insets(6));
        cell.setStyle(
            "-fx-background-color: white;" +
                "-fx-border-color: #9faeff;" +
                "-fx-border-width: 1;"
        );
        return cell;
    }

    private static Button createSlotCell(boolean selected) {
        Button cell = new Button(selected ? "Available" : "");
        cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        cell.setMinHeight(58);
        cell.setFocusTraversable(false);
        updateSlotCellStyle(cell, selected);
        return cell;
    }

    private static void updateSlotCellStyle(Button cell, boolean selected) {
        cell.setText(selected ? "Available" : "");
        cell.setStyle(
            "-fx-background-color: " + (selected ? "#ffe2f2" : "white") + ";" +
                "-fx-border-color: #9faeff;" +
                "-fx-border-width: 1;" +
                "-fx-font-family: Arial;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + (selected ? "#4664a8" : "transparent") + ";" +
                "-fx-cursor: hand;"
        );
    }

    private static void updateCountLabel(Label countLabel, int count) {
        countLabel.setText(count == 0 ? "No slots selected." : count + " slots selected.");
    }
}
