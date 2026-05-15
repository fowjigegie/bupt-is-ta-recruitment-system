package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.admin.WorkloadSummary;
import com.bupt.tarecruitment.application.ApplicationStatus;
import com.bupt.tarecruitment.application.ApplicationStatusPresenter;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.common.text.DisplayFormats;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobStatus;
import com.bupt.tarecruitment.recommendation.MissingSkillsFeedback;
import com.bupt.tarecruitment.recommendation.RecommendationResult;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class TaInsightsPage extends Application {
    private static final int WORKLOAD_LIMIT = 10;

    private static final String BLUE = "#4664a8";
    private static final String PURPLE = "#8b5cf6";
    private static final String CYAN = "#59c7dc";
    private static final String GREEN = "#2e7d32";
    private static final String ORANGE = "#f39c12";
    private static final String RED = "#e74c3c";
    private static final String DARK = "#2f3553";
    private static final String MUTED = "#8b7fa0";
    private static final String BORDER = "#f4d9e6";

    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.TA_INSIGHTS, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        String applicantUserId = context.session().userId();
        InsightData data = InsightData.load(context, applicantUserId);

        VBox content = new VBox(22);
        content.setPadding(new Insets(28, 34, 28, 34));
        content.getChildren().addAll(
            createHeader(context, applicantUserId),
            createKpiRow(data),
            createTopRow(data),
            createMiddleRow(data),
            createRecommendationSection(nav, context, data)
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(
            "-fx-background:#ffffff;" +
                "-fx-background-color:transparent;" +
                "-fx-border-color:transparent;"
        );

        BorderPane root = UiTheme.createPage(
            "TA Insights",
            UiTheme.createApplicantSidebar(nav, PageId.TA_INSIGHTS),
            scrollPane,
            nav,
            context
        );
        return UiTheme.createScene(root);
    }

    private static VBox createHeader(UiAppContext context, String applicantUserId) {
        VBox header = new VBox(6);
        header.getChildren().addAll(
            UiTheme.createPageHeading("TA Insights"),
            UiTheme.createMutedText(
                "Personal view for " + context.formatUserLabel(applicantUserId)
                    + ": application progress, skill fit, availability, and recommended jobs."
            )
        );
        return header;
    }

    private static HBox createKpiRow(InsightData data) {
        HBox row = new HBox(16,
            createKpiCard(
                "Active applications",
                Integer.toString(data.activeApplicationCount()),
                "Not withdrawn",
                KpiIconType.APPLICATIONS,
                "#eef3ff",
                BLUE
            ),
            createKpiCard(
                "Accepted hours",
                DisplayFormats.formatDecimal(data.acceptedWeeklyHours()),
                "Per week",
                KpiIconType.HOURS,
                "#fff3df",
                ORANGE
            ),
            createKpiCard(
                "Best matches",
                Integer.toString(data.fullMatchCount()),
                "Open jobs fully matched",
                KpiIconType.MATCHES,
                "#edf8ef",
                GREEN
            ),
            createKpiCard(
                "Missing skills",
                Integer.toString(data.uniqueMissingSkillCount()),
                "Across open jobs",
                KpiIconType.MISSING_SKILLS,
                "#fff0f0",
                RED
            )
        );
        row.setMaxWidth(Double.MAX_VALUE);
        row.getChildren().forEach(child -> HBox.setHgrow(child, Priority.ALWAYS));
        return row;
    }

    private static HBox createTopRow(InsightData data) {
        VBox funnel = createCard("Application funnel", "Your own application status distribution.");
        funnel.getChildren().add(createApplicationDonut(data.applicationStatusCounts()));

        VBox skillFit = createCard("Skill match overview", "How current open jobs compare with your profile skills.");
        skillFit.getChildren().add(createSkillMatchBars(data));

        HBox row = new HBox(20, funnel, skillFit);
        HBox.setHgrow(funnel, Priority.ALWAYS);
        HBox.setHgrow(skillFit, Priority.ALWAYS);
        return row;
    }

    private static HBox createMiddleRow(InsightData data) {
        VBox missingSkills = createCard("Top missing skills", "Most common missing requirements in open jobs.");
        missingSkills.getChildren().add(createMissingSkillBars(data.missingSkillCounts()));

        VBox availability = createCard("Availability heatmap", "Green is available; blue is already accepted workload.");
        availability.getChildren().add(createAvailabilityHeatmap(data.availableSlots(), data.acceptedSlots()));

        HBox row = new HBox(20, missingSkills, availability);
        HBox.setHgrow(missingSkills, Priority.ALWAYS);
        HBox.setHgrow(availability, Priority.ALWAYS);
        return row;
    }

    private static VBox createRecommendationSection(NavigationManager nav, UiAppContext context, InsightData data) {
        VBox section = createCard("Recommended jobs", "Top open roles based on your skills and preferences.");
        if (data.recommendedJobs().isEmpty()) {
            section.getChildren().add(UiTheme.createMutedText(
                data.hasProfile()
                    ? "No strong recommended jobs yet. Try browsing More Jobs or updating your skills."
                    : "Create your Resume Database profile to unlock recommendations."
            ));
            return section;
        }

        for (RecommendedJobView recommendedJob : data.recommendedJobs()) {
            section.getChildren().add(createRecommendedJobRow(nav, context, recommendedJob));
        }
        return section;
    }

    private static VBox createKpiCard(
        String title,
        String value,
        String subtitle,
        KpiIconType iconType,
        String iconBackground,
        String iconColor
    ) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(16, 18, 14, 18));
        card.setMinHeight(106);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(24), Insets.EMPTY)));
        card.setBorder(new Border(new BorderStroke(
            Color.web(BORDER),
            BorderStrokeStyle.SOLID,
            new CornerRadii(24),
            new BorderWidths(1.4)
        )));

        Label titleLabel = UiTheme.createMutedText(title);
        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 30));
        valueLabel.setTextFill(Color.web(BLUE));
        Label subtitleLabel = UiTheme.createMutedText(subtitle);
        subtitleLabel.setFont(Font.font("Arial", 13));

        VBox copy = new VBox(4, titleLabel, valueLabel, subtitleLabel);
        copy.setMinWidth(0);
        copy.setMaxWidth(Double.MAX_VALUE);

        Region spacer = new Region();
        HBox.setHgrow(copy, Priority.ALWAYS);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox body = new HBox(12, copy, spacer, createKpiIcon(iconType, iconBackground, iconColor));
        body.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().add(body);
        return card;
    }

    private static StackPane createKpiIcon(KpiIconType type, String backgroundColor, String strokeColor) {
        StackPane icon = new StackPane();
        icon.setMinSize(66, 66);
        icon.setPrefSize(66, 66);
        icon.setMaxSize(66, 66);

        Circle background = new Circle(31);
        background.setFill(Color.web(backgroundColor));
        background.setStroke(Color.web(strokeColor));
        background.setStrokeWidth(1.6);

        Pane drawing = new Pane();
        drawing.setMinSize(66, 66);
        drawing.setPrefSize(66, 66);
        drawing.setMaxSize(66, 66);

        switch (type) {
            case APPLICATIONS -> drawApplicationsIcon(drawing, strokeColor);
            case HOURS -> drawHoursIcon(drawing, strokeColor);
            case MATCHES -> drawMatchesIcon(drawing, strokeColor);
            case MISSING_SKILLS -> drawMissingSkillsIcon(drawing, strokeColor);
        }

        icon.getChildren().addAll(background, drawing);
        return icon;
    }

    private static void drawApplicationsIcon(Pane drawing, String color) {
        Rectangle paper = new Rectangle(23, 18, 22, 30);
        paper.setArcWidth(5);
        paper.setArcHeight(5);
        paper.setFill(Color.TRANSPARENT);
        paper.setStroke(Color.web(color));
        paper.setStrokeWidth(2.0);

        drawing.getChildren().addAll(
            paper,
            iconLine(28, 27, 40, 27, color, 1.9),
            iconLine(28, 33, 39, 33, color, 1.9),
            iconLine(27, 41, 31, 45, color, 2.3),
            iconLine(31, 45, 42, 35, color, 2.3)
        );
    }

    private static void drawHoursIcon(Pane drawing, String color) {
        Circle face = iconCircle(33, 33, 18, color, 2.2);
        Circle center = new Circle(33, 33, 2.5);
        center.setFill(Color.web(color));

        drawing.getChildren().addAll(
            face,
            iconLine(33, 33, 33, 22, color, 2.3),
            iconLine(33, 33, 43, 39, color, 2.3),
            center
        );
    }

    private static void drawMatchesIcon(Pane drawing, String color) {
        drawing.getChildren().addAll(
            iconCircle(33, 33, 18, color, 2.0),
            iconCircle(33, 33, 10, color, 1.8),
            iconLine(24, 34, 30, 40, color, 2.5),
            iconLine(30, 40, 43, 26, color, 2.5)
        );
    }

    private static void drawMissingSkillsIcon(Pane drawing, String color) {
        drawing.getChildren().addAll(
            iconLine(33, 20, 45, 46, color, 2.1),
            iconLine(45, 46, 21, 46, color, 2.1),
            iconLine(21, 46, 33, 20, color, 2.1),
            iconLine(33, 29, 33, 38, color, 2.5)
        );

        Circle dot = new Circle(33, 43, 2.4);
        dot.setFill(Color.web(color));
        drawing.getChildren().add(dot);
    }

    private static Circle iconCircle(double centerX, double centerY, double radius, String color, double strokeWidth) {
        Circle circle = new Circle(centerX, centerY, radius);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.web(color));
        circle.setStrokeWidth(strokeWidth);
        return circle;
    }

    private static Line iconLine(
        double startX,
        double startY,
        double endX,
        double endY,
        String color,
        double strokeWidth
    ) {
        Line line = new Line(startX, startY, endX, endY);
        line.setStroke(Color.web(color));
        line.setStrokeWidth(strokeWidth);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        return line;
    }

    private static VBox createCard(String title, String subtitle) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(20));
        card.setMinHeight(320);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(24), Insets.EMPTY)));
        card.setBorder(new Border(new BorderStroke(
            Color.web(BORDER),
            BorderStrokeStyle.SOLID,
            new CornerRadii(24),
            new BorderWidths(1.4)
        )));

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        titleLabel.setTextFill(Color.web(BLUE));

        Label subtitleLabel = UiTheme.createMutedText(subtitle);
        subtitleLabel.setWrapText(true);
        card.getChildren().addAll(titleLabel, subtitleLabel);
        return card;
    }

    private static HBox createApplicationDonut(Map<ApplicationStatus, Integer> counts) {
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        Pane chartPane = new Pane();
        chartPane.setPrefSize(260, 230);
        chartPane.setMinSize(260, 230);

        Circle track = new Circle(130, 112, 72);
        track.setFill(Color.TRANSPARENT);
        track.setStroke(Color.web("#eef0fb"));
        track.setStrokeWidth(24);
        chartPane.getChildren().add(track);

        if (total > 0) {
            double start = 90;
            int index = 0;
            for (ApplicationStatus status : ApplicationStatus.values()) {
                int value = counts.getOrDefault(status, 0);
                if (value <= 0) {
                    continue;
                }
                double length = -360.0 * value / total;
                Arc arc = new Arc(130, 112, 72, 72, start, length);
                arc.setType(ArcType.OPEN);
                arc.setFill(Color.TRANSPARENT);
                arc.setStroke(Color.web(statusColor(status, index)));
                arc.setStrokeWidth(24);
                chartPane.getChildren().add(arc);
                start += length;
                index++;
            }
        }

        Label totalValue = new Label(Integer.toString(total));
        totalValue.setFont(Font.font("Arial", FontWeight.BOLD, 30));
        totalValue.setTextFill(Color.web(BLUE));
        Label totalText = UiTheme.createMutedText("Applications");
        VBox center = new VBox(0, totalValue, totalText);
        center.setAlignment(Pos.CENTER);
        center.setLayoutX(74);
        center.setLayoutY(87);
        center.setPrefWidth(112);
        chartPane.getChildren().add(center);

        VBox legend = new VBox(8);
        int index = 0;
        for (ApplicationStatus status : ApplicationStatus.values()) {
            legend.getChildren().add(createLegendRow(
                statusColor(status, index),
                ApplicationStatusPresenter.toDisplayText(status),
                counts.getOrDefault(status, 0),
                total
            ));
            index++;
        }

        HBox body = new HBox(18, chartPane, legend);
        body.setAlignment(Pos.CENTER_LEFT);
        return body;
    }

    private static VBox createSkillMatchBars(InsightData data) {
        int total = Math.max(1, data.openJobCount());
        VBox box = new VBox(12,
            createProgressRow("Fully matched", data.fullMatchCount(), total, GREEN),
            createProgressRow("Partially matched", data.partialMatchCount(), total, ORANGE),
            createProgressRow("Needs skill work", data.lowMatchCount(), total, RED)
        );
        box.setPadding(new Insets(12, 0, 0, 0));

        Label note = UiTheme.createMutedText(data.hasProfile()
            ? data.openJobCount() + " open job(s) checked against your current profile."
            : "No profile found yet. Create one in Resume Database to calculate matches.");
        note.setWrapText(true);
        box.getChildren().add(note);
        return box;
    }

    private static VBox createMissingSkillBars(Map<String, Integer> missingSkillCounts) {
        VBox box = new VBox(10);
        if (missingSkillCounts.isEmpty()) {
            box.getChildren().add(UiTheme.createMutedText("No missing skill pattern found from current open jobs."));
            return box;
        }

        int max = missingSkillCounts.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        missingSkillCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
            .limit(6)
            .forEach(entry -> box.getChildren().add(createProgressRow(entry.getKey(), entry.getValue(), max, PURPLE)));
        return box;
    }

    private static HBox createProgressRow(String label, int value, int max, String color) {
        Label name = new Label(label);
        name.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        name.setTextFill(Color.web(DARK));
        name.setMinWidth(150);
        name.setPrefWidth(170);

        Rectangle fill = new Rectangle(value <= 0 ? 0 : Math.max(8, 260.0 * value / Math.max(1, max)), 16);
        fill.setArcWidth(16);
        fill.setArcHeight(16);
        fill.setFill(Color.web(color));

        StackPane track = new StackPane(fill);
        track.setAlignment(Pos.CENTER_LEFT);
        track.setPrefHeight(24);
        track.setMinWidth(260);
        track.setMaxWidth(Double.MAX_VALUE);
        track.setStyle("-fx-background-color: #eef0fb; -fx-background-radius: 14;");
        HBox.setHgrow(track, Priority.ALWAYS);

        Label count = new Label(Integer.toString(value));
        count.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        count.setTextFill(Color.web(BLUE));
        count.setMinWidth(40);

        HBox row = new HBox(10, name, track, count);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static VBox createAvailabilityHeatmap(Set<String> availableSlots, Set<String> acceptedSlots) {
        VBox box = new VBox(12);
        box.getChildren().add(createHeatmapLegend());

        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        grid.add(createGridHeader("Time", 92), 0, 0);
        for (int dayIndex = 0; dayIndex < FixedScheduleBands.WEEKDAY_CODES.size(); dayIndex++) {
            String day = FixedScheduleBands.WEEKDAY_CODES.get(dayIndex);
            grid.add(createGridHeader(FixedScheduleBands.weekdayLabels().getOrDefault(day, day), 70), dayIndex + 1, 0);
        }

        for (int row = 0; row < FixedScheduleBands.timeBands().size(); row++) {
            FixedScheduleBands.TimeBand band = FixedScheduleBands.timeBands().get(row);
            grid.add(createGridHeader(band.label(), 92), 0, row + 1);
            for (int dayIndex = 0; dayIndex < FixedScheduleBands.WEEKDAY_CODES.size(); dayIndex++) {
                String day = FixedScheduleBands.WEEKDAY_CODES.get(dayIndex);
                String slot = FixedScheduleBands.toSlotValue(day, band);
                grid.add(createHeatCell(slot, availableSlots, acceptedSlots), dayIndex + 1, row + 1);
            }
        }

        box.getChildren().add(grid);
        return box;
    }

    private static HBox createHeatmapLegend() {
        HBox legend = new HBox(12,
            createLegendPill("#eaf8ee", "#54a66b", "Available"),
            createLegendPill("#e8efff", BLUE, "Accepted"),
            createLegendPill("#fffafd", "#d8c6d3", "Open")
        );
        legend.setAlignment(Pos.CENTER_LEFT);
        return legend;
    }

    private static StackPane createGridHeader(String text, double width) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        label.setTextFill(Color.web("#4d588f"));
        label.setWrapText(true);

        StackPane cell = new StackPane(label);
        cell.setAlignment(Pos.CENTER);
        cell.setPrefSize(width, 34);
        cell.setMinSize(width, 34);
        cell.setStyle("-fx-background-color: #eef0fb; -fx-background-radius: 8;");
        return cell;
    }

    private static StackPane createHeatCell(String slot, Set<String> availableSlots, Set<String> acceptedSlots) {
        boolean accepted = acceptedSlots.contains(slot);
        boolean available = availableSlots.contains(slot);
        String fill = accepted ? "#e8efff" : available ? "#eaf8ee" : "#fffafd";
        String border = accepted ? BLUE : available ? "#54a66b" : "#ead8e4";

        StackPane cell = new StackPane();
        cell.setPrefSize(70, 34);
        cell.setMinSize(70, 34);
        cell.setStyle(
            "-fx-background-color: " + fill + ";" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: " + border + ";" +
                "-fx-border-radius: 8;" +
                "-fx-border-width: 1.2;"
        );
        return cell;
    }

    private static HBox createLegendPill(String background, String border, String text) {
        Region swatch = new Region();
        swatch.setPrefSize(18, 18);
        swatch.setStyle(
            "-fx-background-color: " + background + ";" +
                "-fx-background-radius: 9;" +
                "-fx-border-color: " + border + ";" +
                "-fx-border-radius: 9;"
        );
        Label label = UiTheme.createMutedText(text);
        HBox pill = new HBox(6, swatch, label);
        pill.setAlignment(Pos.CENTER_LEFT);
        return pill;
    }

    private static HBox createRecommendedJobRow(
        NavigationManager nav,
        UiAppContext context,
        RecommendedJobView recommendedJob
    ) {
        JobPosting job = recommendedJob.job();
        RecommendationResult recommendation = recommendedJob.recommendation();

        VBox text = new VBox(5);
        Label title = new Label(job.title());
        title.setFont(Font.font("Arial", FontWeight.BOLD, 17));
        title.setTextFill(Color.web(BLUE));
        title.setWrapText(true);

        Label meta = UiTheme.createMutedText(
            job.jobId() + " | " + job.moduleOrActivity()
                + " | " + DisplayFormats.formatDecimal(job.weeklyHours()) + "h"
        );
        meta.setWrapText(true);

        Label reasons = UiTheme.createMutedText(String.join(" | ", recommendation.reasons()));
        reasons.setWrapText(true);
        text.getChildren().addAll(title, meta, reasons);

        Label score = new Label("Score " + recommendation.matchScore());
        score.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        score.setTextFill(Color.WHITE);
        score.setStyle("-fx-background-color: " + GREEN + "; -fx-background-radius: 18; -fx-padding: 7 12 7 12;");

        Button detailButton = UiTheme.createOutlineButton("Details", 110, 40);
        detailButton.setOnAction(event -> {
            context.selectJob(job.jobId());
            nav.goTo(PageId.JOB_DETAIL);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(14, text, spacer, score, detailButton);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 16, 14, 16));
        row.setStyle(
            "-fx-background-color: #fffafd;" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: #eef0fb;" +
                "-fx-border-radius: 18;"
        );
        return row;
    }

    private static HBox createLegendRow(String color, String name, int value, int total) {
        Circle dot = new Circle(5, Color.web(color));
        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        nameLabel.setTextFill(Color.web(DARK));
        Label valueLabel = UiTheme.createMutedText(total == 0 ? "0" : value + " (" + Math.round(value * 100.0 / total) + "%)");
        HBox row = new HBox(8, dot, nameLabel, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static String statusColor(ApplicationStatus status, int index) {
        return switch (status) {
            case SUBMITTED -> BLUE;
            case SHORTLISTED -> PURPLE;
            case ACCEPTED -> GREEN;
            case REJECTED -> RED;
            case WITHDRAWN -> MUTED;
        };
    }

    private static Set<String> acceptedSlots(Optional<WorkloadSummary> workloadSummary) {
        if (workloadSummary.isEmpty()) {
            return Set.of();
        }

        return workloadSummary.get().acceptedAssignments().stream()
            .flatMap(assignment -> FixedScheduleBands.normalizeToFixedBandSlots(assignment.scheduleSlots()).stream())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> availableSlots(UiAppContext context, String applicantUserId) {
        return context.services().profileRepository()
            .findByUserId(applicantUserId)
            .map(profile -> FixedScheduleBands.normalizeToFixedBandSlots(profile.availabilitySlots()))
            .orElse(List.of())
            .stream()
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Map<ApplicationStatus, Integer> applicationStatusCounts(List<JobApplication> applications) {
        Map<ApplicationStatus, Integer> counts = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            counts.put(status, 0);
        }
        for (JobApplication application : applications) {
            counts.compute(application.status(), (status, oldValue) -> oldValue == null ? 1 : oldValue + 1);
        }
        return counts;
    }

    private static SkillSummary summarizeSkills(
        UiAppContext context,
        String applicantUserId,
        List<JobPosting> openJobs
    ) {
        int full = 0;
        int partial = 0;
        int low = 0;
        Map<String, Integer> missingSkillCounts = new LinkedHashMap<>();

        for (JobPosting job : openJobs) {
            Optional<MissingSkillsFeedback> feedback = context.services().missingSkillsFeedbackService()
                .feedbackForApplicantAndJob(applicantUserId, job.jobId());
            if (feedback.isEmpty()) {
                continue;
            }

            MissingSkillsFeedback item = feedback.get();
            if (item.totalRequiredSkillCount() == 0 || item.fullyMatched()) {
                full++;
            } else if (item.coveragePercent() >= 50 || item.matchedRequiredSkillCount() + item.weaklyMatchedRequiredSkillCount() > 0) {
                partial++;
            } else {
                low++;
            }

            for (String skill : item.missingSkills()) {
                String normalized = skill == null ? "" : skill.trim();
                if (!normalized.isBlank()) {
                    missingSkillCounts.merge(normalized, 1, Integer::sum);
                }
            }
        }

        return new SkillSummary(full, partial, low, missingSkillCounts);
    }

    private static List<RecommendedJobView> recommendedJobs(UiAppContext context, String applicantUserId, List<JobPosting> jobs) {
        Map<String, JobPosting> jobsById = jobs.stream()
            .collect(Collectors.toMap(JobPosting::jobId, Function.identity(), (left, right) -> left));

        return context.services().recommendationService()
            .recommendJobsForApplicant(applicantUserId, 3)
            .stream()
            .map(recommendation -> new RecommendedJobView(jobsById.get(recommendation.jobId()), recommendation))
            .filter(view -> view.job() != null)
            .toList();
    }

    private enum KpiIconType {
        APPLICATIONS,
        HOURS,
        MATCHES,
        MISSING_SKILLS
    }

    private record InsightData(
        Map<ApplicationStatus, Integer> applicationStatusCounts,
        int activeApplicationCount,
        int openJobCount,
        int fullMatchCount,
        int partialMatchCount,
        int lowMatchCount,
        int uniqueMissingSkillCount,
        Map<String, Integer> missingSkillCounts,
        Set<String> availableSlots,
        Set<String> acceptedSlots,
        double acceptedWeeklyHours,
        boolean hasProfile,
        List<RecommendedJobView> recommendedJobs
    ) {
        private static InsightData load(UiAppContext context, String applicantUserId) {
            List<JobPosting> allJobs = context.services().jobRepository().findAll();
            List<JobPosting> openJobs = allJobs.stream()
                .filter(job -> job.status() == JobStatus.OPEN)
                .toList();
            List<JobApplication> applications = context.services().applicationRepository()
                .findByApplicantUserId(applicantUserId);

            Optional<WorkloadSummary> workloadSummary = context.services().adminWorkloadService()
                .getAcceptedTaWorkload(applicantUserId, WORKLOAD_LIMIT);
            SkillSummary skillSummary = summarizeSkills(context, applicantUserId, openJobs);

            return new InsightData(
                TaInsightsPage.applicationStatusCounts(applications),
                (int) applications.stream().filter(application -> application.status() != ApplicationStatus.WITHDRAWN).count(),
                openJobs.size(),
                skillSummary.fullMatchCount(),
                skillSummary.partialMatchCount(),
                skillSummary.lowMatchCount(),
                skillSummary.missingSkillCounts().size(),
                skillSummary.missingSkillCounts(),
                TaInsightsPage.availableSlots(context, applicantUserId),
                TaInsightsPage.acceptedSlots(workloadSummary),
                workloadSummary.map(WorkloadSummary::totalWeeklyHours).orElse(0.0),
                context.services().profileRepository().findByUserId(applicantUserId).isPresent(),
                TaInsightsPage.recommendedJobs(context, applicantUserId, allJobs)
                    .stream()
                    .sorted(Comparator.comparingInt((RecommendedJobView view) -> view.recommendation().matchScore()).reversed())
                    .toList()
            );
        }
    }

    private record SkillSummary(
        int fullMatchCount,
        int partialMatchCount,
        int lowMatchCount,
        Map<String, Integer> missingSkillCounts
    ) {
    }

    private record RecommendedJobView(JobPosting job, RecommendationResult recommendation) {
    }
}
