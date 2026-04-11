package UI;

import com.bupt.tarecruitment.admin.AcceptedAssignment;
import com.bupt.tarecruitment.admin.WorkloadConflict;
import com.bupt.tarecruitment.admin.WorkloadSummary;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;

public class AdminDashboardPage extends Application {
    private static final int DEFAULT_WEEKLY_HOUR_LIMIT = 10;

    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.ADMIN_DASHBOARD, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        long userCount = context.services().userRepository().findAll().size();
        long jobCount = context.services().jobRepository().findAll().size();
        long applicationCount = context.services().applicationRepository().findAll().size();

        VBox center = new VBox(24);
        center.setPadding(new Insets(34, 46, 34, 46));

        HBox stats = new HBox(20,
            UiTheme.createStatCard("Users", Long.toString(userCount), "User records come directly from data/users.txt."),
            UiTheme.createStatCard("Jobs", Long.toString(jobCount), "Jobs currently stored across the project."),
            UiTheme.createStatCard("Applications", Long.toString(applicationCount), "Applications currently stored across the project.")
        );

        HBox lowerCards = new HBox(20,
            UiTheme.createPlaceholderCard("Current user", context.session().displayName() + " is signed in as " + context.session().role() + "."),
            UiTheme.createPlaceholderCard("Data directory", context.startupReport().dataDirectory().toString()),
            UiTheme.createPlaceholderCard("Workload rule", "Only ACCEPTED TA assignments are counted. The default overload limit is 10 weekly hours.")
        );

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
        ScrollPane workloadScroll = new ScrollPane(workloadCards);
        workloadScroll.setFitToWidth(true);
        workloadScroll.setPrefViewportHeight(420);
        workloadScroll.setStyle("-fx-background-color: transparent;");

        // US14: 根据管理员设置的周工时上限，刷新 TA 工作量统计
        refreshButton.setOnAction(event -> reloadWorkloads(context, weeklyHourLimitField, workloadCards, statusLabel));
        reloadWorkloads(context, weeklyHourLimitField, workloadCards, statusLabel);

        HBox controlRow = new HBox(12,
            createControlLabel("Weekly hour limit"),
            weeklyHourLimitField,
            refreshButton
        );
        controlRow.setAlignment(Pos.CENTER_LEFT);

        VBox workloadPanel = new VBox(16,
            UiTheme.createSectionTitle("TA workload control"),
            UiTheme.createMutedText("Live view of ACCEPTED TA assignments, overload warnings, schedule conflicts, and invalid schedule data."),
            controlRow,
            statusLabel,
            workloadScroll
        );

        center.getChildren().addAll(
            UiTheme.createPageHeading("Admin dashboard"),
            UiTheme.createMutedText("Use this page to monitor accepted TA allocations and spot overload, schedule conflict, or schedule-data risks."),
            stats,
            lowerCards,
            workloadPanel
        );

        BorderPane root = UiTheme.createPage("Admin Dashboard", null, center, nav, context);
        return UiTheme.createScene(root);
    }

    private static void reloadWorkloads(
        UiAppContext context,
        TextField weeklyHourLimitField,
        VBox workloadCards,
        Label statusLabel
    ) {
        workloadCards.getChildren().clear();
        statusLabel.setText("");
        statusLabel.setTextFill(Color.web("#b00020"));

        final int weeklyHourLimit;
        try {
            // UI 输入的是字符串，这里先转成 int
            weeklyHourLimit = Integer.parseInt(weeklyHourLimitField.getText().trim());
        } catch (NumberFormatException exception) {
            statusLabel.setText("Weekly hour limit must be an integer.");
            return;
        }

        try {
            // 拉取所有 ACCEPTED 记录，按 TA 汇总，并对冲突/超时做排序提示
            List<WorkloadSummary> summaries = context.services().adminWorkloadService()
                .listAcceptedTaWorkloads(weeklyHourLimit)
                .stream()
                .sorted(Comparator
                    .comparingInt(AdminDashboardPage::riskPriority)
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

            // 每个 summary 对应一张卡片
            for (WorkloadSummary summary : summaries) {
                workloadCards.getChildren().add(createWorkloadCard(summary));
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            statusLabel.setTextFill(Color.web("#b00020"));
            statusLabel.setText(exception.getMessage());
        }
    }

    private static VBox createWorkloadCard(WorkloadSummary summary) {
        // 一张卡片对应一个 TA 的整体工作量情况
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

        Label riskChip = new Label(riskText);
        riskChip.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        riskChip.setTextFill(Color.WHITE);
        riskChip.setStyle(
            "-fx-background-color: " + toWeb(accentColor) + ";" +
                "-fx-background-radius: 16;" +
                "-fx-padding: 6 12 6 12;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(16, titleLabel, spacer, riskChip);
        header.setAlignment(Pos.CENTER_LEFT);

        HBox summaryRow = new HBox(18,
            createSummaryPill("Accepted jobs", Integer.toString(summary.acceptedAssignments().size())),
            createSummaryPill("Weekly hours", Integer.toString(summary.totalWeeklyHours())),
            createSummaryPill("Schedule conflicts", Integer.toString(summary.conflicts().size())),
            createSummaryPill("Invalid slots", Integer.toString(summary.invalidScheduleEntries().size()))
        );
        summaryRow.setAlignment(Pos.CENTER_LEFT);

        VBox assignmentsBox = new VBox(10);
        assignmentsBox.getChildren().add(createSubheading("Accepted assignments"));
        // 把每个已录用岗位展开列出来，便于管理员核对
        for (AcceptedAssignment assignment : summary.acceptedAssignments()) {
            assignmentsBox.getChildren().add(createAssignmentRow(assignment));
        }

        VBox conflictsBox = new VBox(10);
        conflictsBox.getChildren().add(createSubheading("Conflict details"));
        if (summary.conflicts().isEmpty()) {
            conflictsBox.getChildren().add(UiTheme.createMutedText("No overlapping schedule detected across accepted jobs."));
        } else {
            // 把每个冲突的岗位对列出来
            for (WorkloadConflict conflict : summary.conflicts()) {
                conflictsBox.getChildren().add(createConflictRow(conflict));
            }
        }

        VBox invalidSchedulesBox = new VBox(10);
        invalidSchedulesBox.getChildren().add(createSubheading("Invalid schedule data"));
        if (summary.invalidScheduleEntries().isEmpty()) {
            invalidSchedulesBox.getChildren().add(UiTheme.createMutedText("No invalid schedule slot detected in accepted jobs."));
        } else {
            // 如果排期格式非法，这里列出原始字符串，方便排查数据源
            for (String invalidEntry : summary.invalidScheduleEntries()) {
                invalidSchedulesBox.getChildren().add(createInvalidScheduleRow(invalidEntry));
            }
        }

        card.getChildren().addAll(header, summaryRow, assignmentsBox, conflictsBox, invalidSchedulesBox);
        return card;
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

        Label meta = UiTheme.createMutedText(
            assignment.moduleOrActivity()
                + " | "
                + assignment.weeklyHours()
                + "h/week | Schedule: "
                + (assignment.scheduleSlots().isEmpty() ? "(none listed)" : String.join(", ", assignment.scheduleSlots()))
        );

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
                "-fx-padding: 8 14 8 14;"
        );
        return label;
    }

    private static Label createControlLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        label.setTextFill(Color.web("#4664a8"));
        return label;
    }

    private static Label createSubheading(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        label.setTextFill(Color.web("#4664a8"));
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

    public static void main(String[] args) {
        launch(args);
    }
}
