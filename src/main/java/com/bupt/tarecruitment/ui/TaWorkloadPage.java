package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.admin.AcceptedAssignment;
import com.bupt.tarecruitment.admin.WorkloadSummary;
import com.bupt.tarecruitment.common.schedule.ScheduleSlot;
import com.bupt.tarecruitment.common.text.DisplayFormats;
import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 展示 applicant 当前已录用岗位的固定时间表视图。
 */
public final class TaWorkloadPage extends Application {
    private static final int DISPLAY_WEEKLY_LIMIT = 99;
    private static final List<String> JOB_COLORS = List.of(
        "#4969ad",
        "#d56ca8",
        "#6e7ad2",
        "#57a0d3",
        "#b05a88",
        "#5f8b55"
    );

    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.TA_WORKLOAD, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        VBox center = new VBox(20);
        center.setPadding(new Insets(28, 34, 28, 34));

        center.getChildren().add(UiTheme.createPageHeading("TA Workload"));

        Optional<WorkloadSummary> summary = context.services().adminWorkloadService()
            .getAcceptedTaWorkload(context.session().userId(), DISPLAY_WEEKLY_LIMIT);

        center.getChildren().add(UiTheme.createMutedText(
            "This timetable shows only your ACCEPTED TA jobs. Each accepted job is mapped onto the fixed teaching-day time bands."
        ));

        if (summary.isEmpty()) {
            center.getChildren().add(UiTheme.createWhiteCard(
                "No accepted TA jobs yet",
                "Once an application is accepted by an MO, its time slot will appear here as a visual timetable."
            ));
        } else {
            center.getChildren().add(createSummaryRow(summary.get()));
            center.getChildren().add(createTimetableCard(summary.get()));
        }

        center.getChildren().add(new HBox(UiTheme.createBackButton(nav)));

        ScrollPane scrollPane = new ScrollPane(center);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(
            "-fx-background:#ffffff;" +
                "-fx-background-color:transparent;" +
                "-fx-border-color:transparent;"
        );

        BorderPane root = UiTheme.createPage(
            "TA Workload",
            UiTheme.createApplicantSidebar(nav, PageId.TA_WORKLOAD),
            scrollPane,
            nav,
            context
        );
        return UiTheme.createScene(root);
    }

    private static HBox createSummaryRow(WorkloadSummary summary) {
        HBox row = new HBox(16,
            UiTheme.createStatCard("Accepted jobs", Integer.toString(summary.acceptedAssignments().size()), "Visible in this timetable"),
            UiTheme.createStatCard("Weekly hours", DisplayFormats.formatDecimal(summary.totalWeeklyHours()), "Total across accepted jobs")
        );
        return row;
    }

    private static VBox createTimetableCard(WorkloadSummary summary) {
        VBox card = new VBox(18);
        card.setPadding(new Insets(20, 20, 20, 20));
        card.setBackground(new Background(new BackgroundFill(
            Color.WHITE,
            new CornerRadii(26),
            Insets.EMPTY
        )));
        card.setBorder(new Border(new BorderStroke(
            Color.web("#f1d8e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(26),
            new BorderWidths(2)
        )));

        HBox topBar = new HBox(16);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label sectionTitle = UiTheme.createSectionTitle("Accepted timetable");
        Label weekTag = new Label("Current accepted schedule");
        weekTag.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        weekTag.setTextFill(Color.web("#d56ca8"));
        weekTag.setPadding(new Insets(8, 14, 8, 14));
        weekTag.setStyle(
            "-fx-background-color: #ffe5f2;" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: #f3b9d6;" +
                "-fx-border-width: 1.5;" +
                "-fx-border-radius: 18;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topBar.getChildren().addAll(sectionTitle, spacer, weekTag);

        GridPane table = createTimetableGrid(summary.acceptedAssignments());
        card.getChildren().addAll(topBar, table);
        return card;
    }

    private static GridPane createTimetableGrid(List<AcceptedAssignment> assignments) {
        GridPane grid = new GridPane();
        grid.setHgap(0);
        grid.setVgap(0);

        ColumnConstraints timeColumn = new ColumnConstraints();
        timeColumn.setPrefWidth(128);
        grid.getColumnConstraints().add(timeColumn);
        for (int i = 0; i < FixedScheduleBands.WEEKDAY_CODES.size(); i++) {
            ColumnConstraints dayColumn = new ColumnConstraints();
            dayColumn.setPercentWidth(17.4);
            grid.getColumnConstraints().add(dayColumn);
        }

        grid.add(createHeaderCell("Time"), 0, 0);
        List<String> dayLabels = FixedScheduleBands.WEEKDAY_CODES.stream()
            .map(code -> FixedScheduleBands.weekdayLabels().getOrDefault(code, code))
            .toList();
        for (int dayIndex = 0; dayIndex < dayLabels.size(); dayIndex++) {
            grid.add(createHeaderCell(dayLabels.get(dayIndex)), dayIndex + 1, 0);
        }

        Map<String, String> colorByJobId = buildColorMap(assignments);
        for (int rowIndex = 0; rowIndex < FixedScheduleBands.timeBands().size(); rowIndex++) {
            FixedScheduleBands.TimeBand band = FixedScheduleBands.timeBands().get(rowIndex);
            grid.add(createTimeCell(band.label()), 0, rowIndex + 1);

            for (int dayIndex = 0; dayIndex < FixedScheduleBands.WEEKDAY_CODES.size(); dayIndex++) {
                String dayCode = FixedScheduleBands.WEEKDAY_CODES.get(dayIndex);
                grid.add(createScheduleCell(assignments, dayCode, band, colorByJobId), dayIndex + 1, rowIndex + 1);
            }
        }

        return grid;
    }

    private static Map<String, String> buildColorMap(List<AcceptedAssignment> assignments) {
        Map<String, String> colorByJobId = new LinkedHashMap<>();
        int index = 0;
        for (AcceptedAssignment assignment : assignments) {
            colorByJobId.putIfAbsent(assignment.jobId(), JOB_COLORS.get(index % JOB_COLORS.size()));
            if (colorByJobId.containsKey(assignment.jobId())) {
                index++;
            }
        }
        return colorByJobId;
    }

    private static StackPane createHeaderCell(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        label.setTextFill(Color.web("#4d588f"));

        StackPane cell = new StackPane(label);
        cell.setAlignment(Pos.CENTER);
        cell.setMinHeight(48);
        cell.setPadding(new Insets(8));
        cell.setBackground(new Background(new BackgroundFill(
            Color.web("#dde4ff"),
            CornerRadii.EMPTY,
            Insets.EMPTY
        )));
        cell.setBorder(createTableBorder());
        GridPane.setHalignment(cell, HPos.CENTER);
        return cell;
    }

    private static StackPane createTimeCell(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        label.setTextFill(Color.web("#3f4370"));
        label.setWrapText(true);

        StackPane cell = new StackPane(label);
        cell.setAlignment(Pos.CENTER);
        cell.setMinHeight(92);
        cell.setPadding(new Insets(10));
        cell.setBackground(new Background(new BackgroundFill(
            Color.WHITE,
            CornerRadii.EMPTY,
            Insets.EMPTY
        )));
        cell.setBorder(createTableBorder());
        return cell;
    }

    private static VBox createScheduleCell(
        List<AcceptedAssignment> assignments,
        String dayCode,
        FixedScheduleBands.TimeBand band,
        Map<String, String> colorByJobId
    ) {
        VBox cell = new VBox(8);
        cell.setAlignment(Pos.TOP_LEFT);
        cell.setMinHeight(92);
        cell.setPadding(new Insets(10, 10, 10, 10));
        cell.setBackground(new Background(new BackgroundFill(
            Color.WHITE,
            CornerRadii.EMPTY,
            Insets.EMPTY
        )));
        cell.setBorder(createTableBorder());

        for (AcceptedAssignment assignment : assignments) {
            for (String rawSlot : assignment.scheduleSlots()) {
                try {
                    ScheduleSlot slot = ScheduleSlot.parse(rawSlot);
                    if (!slot.dayCode().equals(dayCode) || !overlapsBand(slot, band)) {
                        continue;
                    }
                    cell.getChildren().add(createAssignmentChip(assignment, colorByJobId.getOrDefault(assignment.jobId(), "#4969ad")));
                    break;
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid slot strings in this visual page to avoid crashing the whole timetable.
                }
            }
        }

        return cell;
    }

    private static VBox createAssignmentChip(AcceptedAssignment assignment, String color) {
        Label moduleLabel = new Label(assignment.moduleOrActivity());
        moduleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        moduleLabel.setTextFill(Color.web(color));
        moduleLabel.setWrapText(true);

        Label titleLabel = new Label(assignment.title());
        titleLabel.setFont(Font.font("Arial", 11));
        titleLabel.setTextFill(Color.web(color));
        titleLabel.setWrapText(true);

        VBox chip = new VBox(2, moduleLabel, titleLabel);
        chip.setPadding(new Insets(6, 8, 6, 8));
        chip.setStyle(
            "-fx-background-color: rgba(255,255,255,0.96);" +
                "-fx-border-color: " + color + ";" +
                "-fx-border-width: 1.2;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;"
        );
        return chip;
    }

    private static Border createTableBorder() {
        return new Border(new BorderStroke(
            Color.web("#9faeff"),
            BorderStrokeStyle.SOLID,
            CornerRadii.EMPTY,
            new BorderWidths(1.2)
        ));
    }

    private static boolean overlapsBand(ScheduleSlot slot, FixedScheduleBands.TimeBand band) {
        return slot.startTime().isBefore(band.end()) && band.start().isBefore(slot.endTime());
    }
}
