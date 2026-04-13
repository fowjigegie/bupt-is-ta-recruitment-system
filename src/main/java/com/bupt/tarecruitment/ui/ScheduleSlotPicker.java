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
import java.util.Objects;

/**
 * 提供按工作日和整点选择时间槽的 UI 组件。
 */
final class ScheduleSlotPicker {
    private static final int MAX_SLOT_COUNT = 5;
    private static final List<String> WEEKDAYS = List.of("MON", "TUE", "WED", "THU", "FRI");
    private static final List<String> START_TIMES = List.of(
        "08:00", "09:00", "10:00", "11:00", "12:00",
        "13:00", "14:00", "15:00", "16:00", "17:00"
    );
    private static final List<String> END_TIMES = List.of(
        "09:00", "10:00", "11:00", "12:00", "13:00",
        "14:00", "15:00", "16:00", "17:00", "18:00"
    );

    private final ObservableList<String> selectedSlots;
    private final VBox container;
    private final Label helperText;
    private final FlowPane tagsPane;
    private final ComboBox<String> dayBox;
    private final ComboBox<String> startBox;
    private final ComboBox<String> endBox;
    private final String defaultHelperText;

    private ScheduleSlotPicker(
        ObservableList<String> selectedSlots,
        VBox container,
        Label helperText,
        FlowPane tagsPane,
        ComboBox<String> dayBox,
        ComboBox<String> startBox,
        ComboBox<String> endBox,
        String defaultHelperText
    ) {
        this.selectedSlots = selectedSlots;
        this.container = container;
        this.helperText = helperText;
        this.tagsPane = tagsPane;
        this.dayBox = dayBox;
        this.startBox = startBox;
        this.endBox = endBox;
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
        dayBox.getItems().addAll(WEEKDAYS);
        dayBox.setPromptText("Weekday");
        dayBox.setPrefWidth(130);

        ComboBox<String> startBox = new ComboBox<>();
        startBox.getItems().addAll(START_TIMES);
        startBox.setPromptText("Start");
        startBox.setPrefWidth(130);

        ComboBox<String> endBox = new ComboBox<>();
        endBox.getItems().addAll(END_TIMES);
        endBox.setPromptText("End");
        endBox.setPrefWidth(130);

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

        HBox selectorRow = new HBox(12, dayBox, startBox, endBox, addSlotButton);
        selectorRow.setAlignment(Pos.CENTER_LEFT);

        VBox container = new VBox(8, label, selectorRow, helperText, tagsPane);
        ObservableList<String> selectedSlots = FXCollections.observableArrayList();
        ScheduleSlotPicker picker = new ScheduleSlotPicker(
            selectedSlots,
            container,
            helperText,
            tagsPane,
            dayBox,
            startBox,
            endBox,
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
        return String.join("; ", selectedSlots);
    }

    void setSlots(List<String> slots) {
        selectedSlots.setAll(
            slots.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList()
        );
        refreshTags();
        helperText.setText(defaultHelperText);
    }

    private void addSelectedSlot() {
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
        if (selectedSlots.size() >= MAX_SLOT_COUNT) {
            helperText.setText("At most 5 availability slots are allowed.");
            return;
        }
        if (hasScheduleOverlap(selectedSlots, day, start, end)) {
            helperText.setText("Invalid slot: overlaps with an existing availability slot.");
            return;
        }

        String slot = day + "-" + start + "-" + end;
        if (selectedSlots.contains(slot)) {
            helperText.setText("This slot already exists.");
            return;
        }

        selectedSlots.add(slot);
        dayBox.setValue(null);
        startBox.setValue(null);
        endBox.setValue(null);
        helperText.setText(defaultHelperText);
        refreshTags();
    }

    private void refreshTags() {
        tagsPane.getChildren().clear();
        for (String slot : selectedSlots) {
            Label chipText = new Label(slot);
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
}
