package com.bupt.tarecruitment.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/**
 * 提供 applicant availability 的固定时间段选择器。
 */
final class ScheduleSlotPicker {
    private static final int MAX_SLOT_COUNT = FixedScheduleBands.WEEKDAY_CODES.size() * FixedScheduleBands.timeBands().size();

    private final ObservableList<String> selectedSlots;
    private final VBox container;
    private final Label helperText;
    private final FlowPane tagsPane;
    private final ComboBox<String> dayBox;
    private final ComboBox<String> timeBandBox;
    private final String defaultHelperText;

    private ScheduleSlotPicker(
        ObservableList<String> selectedSlots,
        VBox container,
        Label helperText,
        FlowPane tagsPane,
        ComboBox<String> dayBox,
        ComboBox<String> timeBandBox,
        String defaultHelperText
    ) {
        this.selectedSlots = selectedSlots;
        this.container = container;
        this.helperText = helperText;
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
        ObservableList<String> selectedSlots = FXCollections.observableArrayList();
        ScheduleSlotPicker picker = new ScheduleSlotPicker(
            selectedSlots,
            container,
            helperText,
            tagsPane,
            dayBox,
            timeBandBox,
            defaultHelperText
        );
        addSlotButton.setOnAction(event -> picker.addSelectedSlot());
        picker.refreshTags();
        return picker;
    }

    VBox container() {
        return container;
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
    }
}
