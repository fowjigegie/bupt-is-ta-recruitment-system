package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.admin.AcceptedAssignment;
import com.bupt.tarecruitment.admin.WorkloadConflict;
import com.bupt.tarecruitment.admin.WorkloadSummary;
import com.bupt.tarecruitment.common.schedule.ScheduleSlot;
import com.bupt.tarecruitment.common.text.DisplayFormats;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;

/**
 * 管理员首页中的工作量控制面板，负责刷新和展示 TA 风险摘要。
 */
final class AdminWorkloadPanel {
    private static final int DEFAULT_WEEKLY_HOUR_LIMIT = 10;

    private final TextField weeklyHourLimitField;
    private final Label statusLabel;
    private final VBox workloadOverview;
    private final VBox workloadCards;
    private final VBox container;

    private AdminWorkloadPanel(
        TextField weeklyHourLimitField,
        Label statusLabel,
        VBox workloadOverview,
        VBox workloadCards,
        VBox container
    ) {
        this.weeklyHourLimitField = weeklyHourLimitField;
        this.statusLabel = statusLabel;
        this.workloadOverview = workloadOverview;
        this.workloadCards = workloadCards;
        this.container = container;
    }

    static AdminWorkloadPanel create(UiAppContext context) {
        TextField weeklyHourLimitField = new TextField(Integer.toString(DEFAULT_WEEKLY_HOUR_LIMIT));
        weeklyHourLimitField.setPrefWidth(120);
        weeklyHourLimitField.setPrefHeight(42);
        weeklyHourLimitField.setStyle(
            "-fx-background-color: white;" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: #f0a6e9;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 18;" +
                "-fx-font-size: 16px;"
        );

        var refreshButton = UiTheme.createOutlineButton("Refresh", 130, 42);
        Label statusLabel = UiTheme.createMutedText("");
        statusLabel.setTextFill(Color.web("#b00020"));

        VBox workloadCards = new VBox(16);
        VBox workloadOverview = new VBox(10);
        ScrollPane workloadScroll = new ScrollPane(workloadCards);
        workloadScroll.setFitToWidth(true);
        workloadScroll.setPrefViewportHeight(320);
        workloadScroll.setStyle("-fx-background-color: transparent;");

        HBox controlRow = new HBox(12,
            createControlLabel("Weekly hour limit"),
            weeklyHourLimitField,
            refreshButton
        );
        controlRow.setAlignment(Pos.CENTER_LEFT);

        VBox container = new VBox(16,
            UiTheme.createSectionTitle("TA workload control"),
            UiTheme.createMutedText("Live view of ACCEPTED TA assignments, overload warnings, schedule conflicts, and invalid schedule data."),
            controlRow,
            statusLabel,
            workloadOverview,
            workloadScroll
        );
        container.setStyle("-fx-text-fill: #2f3553;");

        AdminWorkloadPanel panel = new AdminWorkloadPanel(weeklyHourLimitField, statusLabel, workloadOverview, workloadCards, container);
        refreshButton.setOnAction(event -> panel.reload(context));
        panel.reload(context);
        return panel;
    }

    VBox container() {
        return container;
    }

    private void reload(UiAppContext context) {
        workloadCards.getChildren().clear();
        workloadOverview.getChildren().clear();
        statusLabel.setText("");
        statusLabel.setTextFill(Color.web("#b00020"));

        final int weeklyHourLimit;
        try {
            weeklyHourLimit = Integer.parseInt(weeklyHourLimitField.getText().trim());
        } catch (NumberFormatException exception) {
            statusLabel.setText("Weekly hour limit must be an integer.");
            return;
        }

        try {
            List<WorkloadSummary> summaries = context.services().adminWorkloadService()
                .listAcceptedTaWorkloads(weeklyHourLimit)
                .stream()
                .sorted(Comparator
                    .comparingInt(AdminWorkloadPanel::riskPriority)
                    .thenComparing(WorkloadSummary::applicantUserId))
                .toList();

            if (summaries.isEmpty()) {
                workloadCards.getChildren().add(
                    UiTheme.createWhiteCard(
                        "No accepted TA assignments",
                        "No TA is currently in ACCEPTED status. Review applications on the MO side to generate workload data."
                    )
                );
                return;
            }

            long flaggedCount = summaries.stream()
                .filter(summary -> summary.hasConflict() || summary.overloaded() || summary.hasInvalidScheduleData())
                .count();
            statusLabel.setTextFill(Color.web("#4969ad"));
            statusLabel.setText(
                "Loaded %d accepted TA summaries. %d currently flagged for overload, schedule conflict, or invalid schedule data."
                    .formatted(summaries.size(), flaggedCount)
            );

            workloadOverview.getChildren().add(createWorkloadOverview(summaries, weeklyHourLimit));

            for (WorkloadSummary summary : summaries) {
                workloadCards.getChildren().add(createWorkloadCard(summary));
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            statusLabel.setTextFill(Color.web("#b00020"));
            statusLabel.setText(exception.getMessage());
        }
    }

    private static VBox createWorkloadOverview(List<WorkloadSummary> summaries, int weeklyHourLimit) {
        Label title = createSubheading("TA workload overview");

        double maxHours = summaries.stream()
            .mapToDouble(WorkloadSummary::totalWeeklyHours)
            .max()
            .orElse(weeklyHourLimit);
        double scaleMax = Math.max(weeklyHourLimit, maxHours);

        VBox bars = new VBox(8);
        for (WorkloadSummary summary : summaries.stream()
            .sorted(Comparator.comparingDouble(WorkloadSummary::totalWeeklyHours).reversed())
            .toList()) {
            bars.getChildren().add(createWorkloadOverviewRow(summary, scaleMax, weeklyHourLimit));
        }

        VBox panel = new VBox(10, title, bars);
        panel.setPadding(new Insets(18));
        panel.setMaxWidth(Double.MAX_VALUE);
        panel.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(24), Insets.EMPTY)));
        panel.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(24),
            new BorderWidths(1.5)
        )));
        return panel;
    }

    private static HBox createWorkloadOverviewRow(WorkloadSummary summary, double scaleMax, int weeklyHourLimit) {
        Label name = new Label(summary.applicantDisplayName());
        name.setMinWidth(220);
        name.setPrefWidth(260);
        name.setWrapText(true);
        name.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        name.setStyle("-fx-text-fill: #4664a8;");

        Region bar = new Region();
        bar.setMinHeight(18);
        bar.setPrefWidth(Math.max(18, 520.0 * summary.totalWeeklyHours() / Math.max(1, scaleMax)));
        bar.setStyle(
            "-fx-background-color: " + (summary.totalWeeklyHours() > weeklyHourLimit ? "#ffd58a" : "#9fd6a6") + ";" +
                "-fx-background-radius: 9;"
        );

        Label hours = new Label(DisplayFormats.formatDecimal(summary.totalWeeklyHours()) + "h");
        hours.setMinWidth(70);
        hours.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        hours.setStyle("-fx-text-fill: #5c6481;");

        HBox row = new HBox(12, name, bar, hours);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static VBox createWorkloadCard(WorkloadSummary summary) {
        Color accentColor = riskColor(summary);
        String riskText = riskText(summary);

        VBox card = new VBox(14);
        card.setPadding(new Insets(22));
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(24), Insets.EMPTY)));
        card.setBorder(new Border(new BorderStroke(
            accentColor,
            BorderStrokeStyle.SOLID,
            new CornerRadii(24),
            new BorderWidths(3)
        )));

        Label titleLabel = new Label(summary.applicantDisplayName() + " (" + summary.applicantUserId() + ")");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web("#4664a8"));
        titleLabel.setStyle("-fx-text-fill: #4664a8; -fx-font-weight: bold; -fx-font-size: 24px;");

        Label riskChip = new Label(riskText);
        riskChip.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        riskChip.setTextFill(Color.WHITE);
        riskChip.setStyle(
            "-fx-background-color: " + toWeb(accentColor) + ";" +
                "-fx-background-radius: 16;" +
                "-fx-text-fill: white;" +
                "-fx-padding: 6 12 6 12;"
        );

        Button detailsButton = UiTheme.createOutlineButton("View details", 140, 40);
        detailsButton.setOnAction(event -> showDetailsDialog(summary, detailsButton));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(16, titleLabel, spacer, riskChip, detailsButton);
        header.setAlignment(Pos.CENTER_LEFT);

        HBox summaryRow = new HBox(18,
            createSummaryPill("Accepted jobs", Integer.toString(summary.acceptedAssignments().size())),
            createSummaryPill("Weekly hours", DisplayFormats.formatDecimal(summary.totalWeeklyHours())),
            createSummaryPill("Schedule conflicts", Integer.toString(summary.conflicts().size())),
            createSummaryPill("Invalid slots", Integer.toString(summary.invalidScheduleEntries().size()))
        );
        summaryRow.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(header, summaryRow);
        return card;
    }

    private static void showDetailsDialog(WorkloadSummary summary, Button ownerButton) {
        Stage dialog = new Stage();
        if (ownerButton.getScene() != null && ownerButton.getScene().getWindow() != null) {
            dialog.initOwner(ownerButton.getScene().getWindow());
        }
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("TA workload details");

        Label title = new Label(summary.applicantDisplayName() + " (" + summary.applicantUserId() + ")");
        title.setStyle("-fx-text-fill: #4664a8; -fx-font-weight: bold; -fx-font-size: 26px;");

        HBox summaryRow = new HBox(14,
            createSummaryPill("Accepted jobs", Integer.toString(summary.acceptedAssignments().size())),
            createSummaryPill("Weekly hours", DisplayFormats.formatDecimal(summary.totalWeeklyHours())),
            createSummaryPill("Schedule conflicts", Integer.toString(summary.conflicts().size())),
            createSummaryPill("Invalid slots", Integer.toString(summary.invalidScheduleEntries().size()))
        );
        summaryRow.setAlignment(Pos.CENTER_LEFT);

        VBox assignmentsBox = new VBox(10);
        assignmentsBox.getChildren().add(createSubheading("Accepted assignments"));
        for (AcceptedAssignment assignment : summary.acceptedAssignments()) {
            assignmentsBox.getChildren().add(createAssignmentRow(assignment));
        }

        VBox timetableBox = new VBox(10, createSubheading("Schedule view"), createTimetableGrid(summary));

        VBox conflictsBox = new VBox(10);
        conflictsBox.getChildren().add(createSubheading("Conflict details"));
        if (summary.conflicts().isEmpty()) {
            Label empty = UiTheme.createMutedText("No overlapping schedule detected across accepted jobs.");
            empty.setStyle("-fx-text-fill: #5c6481; -fx-font-size: 15px;");
            conflictsBox.getChildren().add(empty);
        } else {
            for (WorkloadConflict conflict : summary.conflicts()) {
                conflictsBox.getChildren().add(createConflictRow(conflict));
            }
        }

        VBox invalidSchedulesBox = new VBox(10);
        invalidSchedulesBox.getChildren().add(createSubheading("Invalid schedule data"));
        if (summary.invalidScheduleEntries().isEmpty()) {
            Label empty = UiTheme.createMutedText("No invalid schedule slot detected in accepted jobs.");
            empty.setStyle("-fx-text-fill: #5c6481; -fx-font-size: 15px;");
            invalidSchedulesBox.getChildren().add(empty);
        } else {
            for (String invalidEntry : summary.invalidScheduleEntries()) {
                invalidSchedulesBox.getChildren().add(createInvalidScheduleRow(invalidEntry));
            }
        }

        Button closeButton = UiTheme.createOutlineButton("Close", 120, 40);
        closeButton.setOnAction(event -> dialog.close());
        HBox actionRow = new HBox(closeButton);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(16, title, summaryRow, assignmentsBox, timetableBox, conflictsBox, invalidSchedulesBox, actionRow);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: white;");

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setStyle("-fx-background-color: white; -fx-background: white; -fx-border-color: transparent;");

        dialog.setMinWidth(900);
        dialog.setMinHeight(680);
        dialog.setScene(new Scene(scroll, 980, 720));
        dialog.showAndWait();
    }

    private static GridPane createTimetableGrid(WorkloadSummary summary) {
        GridPane grid = new GridPane();
        grid.setHgap(0);
        grid.setVgap(0);
        grid.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints timeColumn = new ColumnConstraints();
        timeColumn.setPrefWidth(112);
        grid.getColumnConstraints().add(timeColumn);
        for (int i = 0; i < FixedScheduleBands.WEEKDAY_CODES.size(); i++) {
            ColumnConstraints dayColumn = new ColumnConstraints();
            dayColumn.setPercentWidth(17.8);
            grid.getColumnConstraints().add(dayColumn);
        }

        grid.add(createTimetableHeader("Time"), 0, 0);
        List<String> dayLabels = FixedScheduleBands.WEEKDAY_CODES.stream()
            .map(code -> FixedScheduleBands.weekdayLabels().getOrDefault(code, code))
            .toList();
        for (int dayIndex = 0; dayIndex < dayLabels.size(); dayIndex++) {
            grid.add(createTimetableHeader(dayLabels.get(dayIndex)), dayIndex + 1, 0);
        }

        for (int rowIndex = 0; rowIndex < FixedScheduleBands.timeBands().size(); rowIndex++) {
            FixedScheduleBands.TimeBand band = FixedScheduleBands.timeBands().get(rowIndex);
            grid.add(createTimeCell(band.label()), 0, rowIndex + 1);

            for (int dayIndex = 0; dayIndex < FixedScheduleBands.WEEKDAY_CODES.size(); dayIndex++) {
                String dayCode = FixedScheduleBands.WEEKDAY_CODES.get(dayIndex);
                grid.add(createAssignmentCell(summary, dayCode, band), dayIndex + 1, rowIndex + 1);
            }
        }
        return grid;
    }

    private static StackPane createTimetableHeader(String text) {
        Label label = new Label(text);
        label.setStyle(
            "-fx-font-family: Arial;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4d588f;"
        );

        StackPane cell = new StackPane(label);
        cell.setAlignment(Pos.CENTER);
        cell.setMinHeight(36);
        cell.setPadding(new Insets(6));
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
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #3f4370;"
        );

        StackPane cell = new StackPane(label);
        cell.setAlignment(Pos.CENTER);
        cell.setMinHeight(54);
        cell.setPadding(new Insets(6));
        cell.setStyle(
            "-fx-background-color: white;" +
                "-fx-border-color: #9faeff;" +
                "-fx-border-width: 1;"
        );
        return cell;
    }

    private static VBox createAssignmentCell(
        WorkloadSummary summary,
        String dayCode,
        FixedScheduleBands.TimeBand band
    ) {
        List<AcceptedAssignment> assignments = summary.acceptedAssignments().stream()
            .filter(assignment -> assignmentCoversBand(assignment, dayCode, band))
            .toList();
        boolean conflict = assignments.size() > 1;

        VBox cell = new VBox(4);
        cell.setAlignment(Pos.TOP_LEFT);
        cell.setMinHeight(54);
        cell.setPadding(new Insets(6));
        cell.setStyle(
            "-fx-background-color: " + (conflict ? "#fff0f0" : assignments.isEmpty() ? "white" : "#fff8fb") + ";" +
                "-fx-border-color: " + (conflict ? "#e74c3c" : "#9faeff") + ";" +
                "-fx-border-width: 1;"
        );

        for (AcceptedAssignment assignment : assignments) {
            Label chip = new Label(assignment.jobId());
            chip.setMaxWidth(Double.MAX_VALUE);
            chip.setWrapText(true);
            chip.setStyle(
                "-fx-background-color: " + (conflict ? "#ffe1e1" : "#ffe6f2") + ";" +
                    "-fx-background-radius: 10;" +
                    "-fx-border-color: " + (conflict ? "#e74c3c" : "#f3b2df") + ";" +
                    "-fx-border-radius: 10;" +
                    "-fx-border-width: 1;" +
                    "-fx-font-size: 11px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: " + (conflict ? "#b00020" : "#4664a8") + ";" +
                    "-fx-padding: 3 6 3 6;"
            );
            cell.getChildren().add(chip);
        }
        return cell;
    }

    private static boolean assignmentCoversBand(
        AcceptedAssignment assignment,
        String dayCode,
        FixedScheduleBands.TimeBand band
    ) {
        for (String rawSlot : assignment.scheduleSlots()) {
            try {
                ScheduleSlot slot = ScheduleSlot.parse(rawSlot);
                if (slot.dayCode().equals(dayCode) && slot.startTime().isBefore(band.end()) && band.start().isBefore(slot.endTime())) {
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
                // Invalid entries are displayed separately in the admin workload panel.
            }
        }
        return false;
    }

    private static VBox createAssignmentRow(AcceptedAssignment assignment) {
        VBox row = new VBox(6);
        row.setPadding(new Insets(14));
        row.setBackground(new Background(new BackgroundFill(Color.web("#fff8fb"), new CornerRadii(18), Insets.EMPTY)));
        row.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(18),
            new BorderWidths(1.5)
        )));

        Label title = new Label(assignment.jobId() + " | " + assignment.title());
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#4664a8"));
        title.setStyle("-fx-text-fill: #4664a8; -fx-font-weight: bold; -fx-font-size: 18px;");

        Label meta = UiTheme.createMutedText(
            assignment.moduleOrActivity()
                + " | "
                + DisplayFormats.formatDecimal(assignment.weeklyHours())
                + "h/week | Schedule: "
                + FixedScheduleBands.formatScheduleList(assignment.scheduleSlots())
        );
        meta.setStyle("-fx-text-fill: #5c6481; -fx-font-size: 15px;");

        row.getChildren().addAll(title, meta);
        return row;
    }

    private static VBox createConflictRow(WorkloadConflict conflict) {
        VBox row = new VBox(6);
        row.setPadding(new Insets(14));
        row.setBackground(new Background(new BackgroundFill(Color.web("#fff2f2"), new CornerRadii(18), Insets.EMPTY)));
        row.setBorder(new Border(new BorderStroke(
            Color.web("#e74c3c"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(18),
            new BorderWidths(1.5)
        )));

        Label jobs = new Label(
            conflict.jobIdA() + " " + conflict.jobTitleA() + "  <->  " + conflict.jobIdB() + " " + conflict.jobTitleB()
        );
        jobs.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        jobs.setTextFill(Color.web("#b23b30"));
        jobs.setWrapText(true);

        Label overlap = UiTheme.createMutedText("Overlap slot: " + conflict.overlapSlot());
        overlap.setTextFill(Color.web("#b23b30"));

        row.getChildren().addAll(jobs, overlap);
        return row;
    }

    private static VBox createInvalidScheduleRow(String invalidEntry) {
        VBox row = new VBox(6);
        row.setPadding(new Insets(14));
        row.setBackground(new Background(new BackgroundFill(Color.web("#fff7ed"), new CornerRadii(18), Insets.EMPTY)));
        row.setBorder(new Border(new BorderStroke(
            Color.web("#f39c12"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(18),
            new BorderWidths(1.5)
        )));

        Label issue = new Label(invalidEntry);
        issue.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        issue.setTextFill(Color.web("#a65d03"));
        issue.setWrapText(true);

        row.getChildren().add(issue);
        return row;
    }

    private static Label createSummaryPill(String title, String value) {
        Label label = new Label(title + ": " + value);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        label.setTextFill(Color.web("#5c6481"));
        label.setStyle(
            "-fx-background-color: #fff3f7;" +
                "-fx-background-radius: 18;" +
                "-fx-text-fill: #5c6481;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 15px;" +
                "-fx-padding: 8 14 8 14;"
        );
        return label;
    }

    private static Label createControlLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        label.setTextFill(Color.web("#4664a8"));
        label.setStyle("-fx-text-fill: #4664a8; -fx-font-weight: bold; -fx-font-size: 16px;");
        return label;
    }

    private static Label createSubheading(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        label.setTextFill(Color.web("#4664a8"));
        label.setStyle("-fx-text-fill: #4664a8; -fx-font-weight: bold; -fx-font-size: 18px;");
        return label;
    }

    private static int riskPriority(WorkloadSummary summary) {
        if (summary.hasInvalidScheduleData()) {
            return 0;
        }
        if (summary.hasConflict()) {
            return 1;
        }
        if (summary.overloaded()) {
            return 2;
        }
        return 3;
    }

    private static String riskText(WorkloadSummary summary) {
        if (summary.hasInvalidScheduleData()) {
            return "Schedule data issue";
        }
        if (summary.hasConflict()) {
            return "Conflict detected";
        }
        if (summary.overloaded()) {
            return "Overload warning";
        }
        return "Normal workload";
    }

    private static Color riskColor(WorkloadSummary summary) {
        if (summary.hasInvalidScheduleData()) {
            return Color.web("#f39c12");
        }
        if (summary.hasConflict()) {
            return Color.web("#e74c3c");
        }
        if (summary.overloaded()) {
            return Color.web("#f39c12");
        }
        return Color.web("#2e7d32");
    }

    private static String toWeb(Color color) {
        return String.format("#%02x%02x%02x",
            (int) Math.round(color.getRed() * 255),
            (int) Math.round(color.getGreen() * 255),
            (int) Math.round(color.getBlue() * 255));
    }
}
