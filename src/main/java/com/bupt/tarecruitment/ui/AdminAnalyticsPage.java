package com.bupt.tarecruitment.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.bupt.tarecruitment.admin.WorkloadSummary;
import com.bupt.tarecruitment.application.ApplicationStatus;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.common.text.DisplayFormats;
import com.bupt.tarecruitment.job.JobPosting;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

public class AdminAnalyticsPage extends Application {
    private static final int DEFAULT_WEEKLY_HOUR_LIMIT = 10;
    private static final Duration AUTO_REFRESH_INTERVAL = Duration.seconds(3);
    private static final Duration ENTRANCE_DURATION = Duration.millis(520);
    private static final double ENTRANCE_STAGGER_MILLIS = 85;
    private static final double ENTRANCE_OFFSET_Y = 18;
    private static final Duration CHART_ANIMATION_DELAY = Duration.millis(320);
    private static final Duration CHART_ANIMATION_DURATION = Duration.millis(760);
    private static final Duration DOT_POP_DURATION = Duration.millis(260);
    private static final double DOT_STAGGER_MILLIS = 75;
    private static final double BAR_STAGGER_MILLIS = 55;
    private static final double RING_STAGGER_MILLIS = 70;

    private static final String BLUE = "#4664a8";
    private static final String PURPLE = "#8b5cf6";
    private static final String CYAN = "#59c7dc";
    private static final String ORANGE = "#f39c12";
    private static final String RED = "#e74c3c";
    private static final String GREEN = "#2e7d32";
    private static final String MUTED = "#8b7fa0";
    private static final String DARK = "#2f3553";
    private static final String BORDER = "#f4d9e6";

    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.ADMIN_ANALYTICS, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        AnalyticsView view = new AnalyticsView(nav, context);
        return view.createScene();
    }

    private static final class AnalyticsView {
        private final NavigationManager nav;
        private final UiAppContext context;
        private final Tooltip hoverTooltip = new Tooltip();

        private final Label applicationsValue = metricValueLabel();
        private final Label jobsValue = metricValueLabel();
        private final Label acceptedValue = metricValueLabel();
        private final Label averageWorkloadValue = metricValueLabel();
        private final Label riskValue = metricValueLabel();
        private final Label analysisNote = new Label();

        private final TrendLineView trendLineView = new TrendLineView();
        private final DonutChartView applicationStatusView = new DonutChartView();
        private final DonutChartView riskStatusView = new DonutChartView();
        private final HorizontalDemandView jobDemandView = new HorizontalDemandView();
        private final RankingView workloadRankingView = new RankingView();

        private Timeline autoRefresh;

        private AnalyticsView(NavigationManager nav, UiAppContext context) {
            this.nav = nav;
            this.context = context;
            configureTooltip();
        }

        private Scene createScene() {
            VBox content = new VBox(24);
            content.setPadding(new Insets(30, 42, 32, 42));

            var backButton = UiTheme.createOutlineButton("Back to dashboard", 190, 42);
            backButton.setOnAction(event -> {
                stopAutoRefresh();
                hoverTooltip.hide();
                nav.goTo(PageId.ADMIN_DASHBOARD);
            });

            HBox actionRow = new HBox(12, backButton);
            actionRow.setAlignment(Pos.CENTER_LEFT);

            Label heading = UiTheme.createPageHeading("Admin data analytics");
            heading.setStyle("-fx-text-fill: " + DARK + ";");

            Label subtitle = UiTheme.createMutedText("Detailed visual report for application status, job demand, accepted TA workload, and risk signals.");
            subtitle.setStyle("-fx-text-fill: " + MUTED + ";");

            content.getChildren().addAll(
                heading,
                subtitle,
                actionRow,
                createTopAnalyticsRow(),
                createBottomAnalyticsRow(),
                createAnalysisNotes()
            );
            prepareEntranceAnimation(content);

            ScrollPane scrollPane = new ScrollPane(content);
            scrollPane.setFitToWidth(true);
            scrollPane.setPannable(true);
            scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

            BorderPane root = UiTheme.createPage("Admin Data Analytics", null, scrollPane, nav, context);
            Scene scene = UiTheme.createScene(root);

            reloadData();
            playEntranceAnimation(content);

            autoRefresh = new Timeline(new KeyFrame(AUTO_REFRESH_INTERVAL, event -> reloadData()));
            autoRefresh.setCycleCount(Timeline.INDEFINITE);
            autoRefresh.play();

            return scene;
        }

        private void prepareEntranceAnimation(VBox content) {
            for (Node node : content.getChildren()) {
                node.setOpacity(0);
                node.setTranslateY(ENTRANCE_OFFSET_Y);
            }
        }

        private void playEntranceAnimation(VBox content) {
            int index = 0;
            for (Node node : content.getChildren()) {
                Duration delay = Duration.millis(index * ENTRANCE_STAGGER_MILLIS);

                FadeTransition fade = new FadeTransition(ENTRANCE_DURATION, node);
                fade.setFromValue(0);
                fade.setToValue(1);
                fade.setDelay(delay);
                fade.setInterpolator(Interpolator.EASE_OUT);

                TranslateTransition slide = new TranslateTransition(ENTRANCE_DURATION, node);
                slide.setFromY(ENTRANCE_OFFSET_Y);
                slide.setToY(0);
                slide.setDelay(delay);
                slide.setInterpolator(Interpolator.EASE_OUT);

                new ParallelTransition(fade, slide).play();
                index++;
            }
        }

        private HBox createKpiRow() {
            HBox row = new HBox(18,
                createKpiCard("Applications", applicationsValue, "Total submitted", PURPLE, "A"),
                createKpiCard("Jobs", jobsValue, "Current postings", ORANGE, "J"),
                createKpiCard("Accepted", acceptedValue, "Accepted records", "#ff8a65", "✓"),
                createKpiCard("Avg workload", averageWorkloadValue, "Hours per TA", CYAN, "h"),
                createKpiCard("Risk TAs", riskValue, "Need attention", RED, "!")
            );
            row.setMaxWidth(Double.MAX_VALUE);
            row.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));
            return row;
        }

        private HBox createTopAnalyticsRow() {
            VBox trendCard = createDashboardCard(
                "Application overview",
                "Smooth trend-style visual based on current application status counts.",
                trendLineView.container()
            );

            VBox statusCard = createDashboardCard(
                "Application status",
                "Donut distribution of submitted, accepted, withdrawn and other statuses.",
                applicationStatusView.container()
            );

            HBox row = new HBox(22, trendCard, statusCard);
            HBox.setHgrow(trendCard, Priority.ALWAYS);
            HBox.setHgrow(statusCard, Priority.ALWAYS);
            return row;
        }

        private HBox createBottomAnalyticsRow() {
            VBox demandCard = createDashboardCard(
                "Job demand",
                "Horizontal ranking of jobs by application count, with the highest-demand role highlighted.",
                jobDemandView.container()
            );

            VBox rightColumn = new VBox(22,
                createDashboardCard(
                    "Risk ring",
                    "Accepted TA risk categories from workload analysis.",
                    riskStatusView.container()
                ),
                createDashboardCard(
                    "TA workload ranking",
                    "Horizontal workload leaderboard for accepted TAs.",
                    workloadRankingView.container()
                )
            );
            rightColumn.setMinWidth(500);
            rightColumn.setPrefWidth(520);
            rightColumn.setMaxWidth(560);

            HBox row = new HBox(22, demandCard, rightColumn);
            HBox.setHgrow(demandCard, Priority.ALWAYS);
            return row;
        }

        private VBox createAnalysisNotes() {
            VBox box = cardBase();
            box.setSpacing(10);
            box.setMinHeight(120);

            Label title = titleLabel("Analysis summary");
            analysisNote.setFont(Font.font("Arial", 15));
            analysisNote.setStyle("-fx-text-fill: " + DARK + ";");
            analysisNote.setWrapText(true);

            box.getChildren().addAll(title, analysisNote);
            return box;
        }

        private void reloadData() {
            List<JobApplication> applications = context.services().applicationRepository().findAll();
            List<JobPosting> jobs = context.services().jobRepository().findAll();
            List<WorkloadSummary> workloads = context.services()
                .adminWorkloadService()
                .listAcceptedTaWorkloads(DEFAULT_WEEKLY_HOUR_LIMIT);

            updateKpis(applications, jobs, workloads);
            updateTrend(applications);
            updateApplicationStatus(applications);
            updateRiskStatus(workloads);
            updateJobDemand(applications, jobs);
            updateWorkloadRanking(workloads);
            analysisNote.setText(buildAnalysisText(applications, jobs, workloads));
        }

        private void updateKpis(List<JobApplication> applications, List<JobPosting> jobs, List<WorkloadSummary> workloads) {
            long acceptedCount = applications.stream()
                .filter(application -> application.status() == ApplicationStatus.ACCEPTED)
                .count();

            double totalHours = workloads.stream()
                .mapToDouble(WorkloadSummary::totalWeeklyHours)
                .sum();

            double averageHours = workloads.isEmpty() ? 0 : totalHours / workloads.size();

            long riskCount = workloads.stream()
                .filter(summary -> summary.overloaded() || summary.hasConflict() || summary.hasInvalidScheduleData())
                .count();

            applicationsValue.setText(Long.toString(applications.size()));
            jobsValue.setText(Long.toString(jobs.size()));
            acceptedValue.setText(Long.toString(acceptedCount));
            averageWorkloadValue.setText(DisplayFormats.formatDecimal(averageHours));
            riskValue.setText(Long.toString(riskCount));
        }

        private void updateTrend(List<JobApplication> applications) {
            Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
            for (ApplicationStatus status : ApplicationStatus.values()) {
                counts.put(status, 0L);
            }
            for (JobApplication application : applications) {
                counts.compute(application.status(), (status, count) -> count == null ? 1L : count + 1L);
            }

            List<ChartItem> items = new ArrayList<>();
            for (ApplicationStatus status : ApplicationStatus.values()) {
                long count = counts.getOrDefault(status, 0L);
                items.add(new ChartItem(status.name(), count, status.name()));
            }

            trendLineView.update(items, hoverTooltip);
        }

        private void updateApplicationStatus(List<JobApplication> applications) {
            Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
            for (ApplicationStatus status : ApplicationStatus.values()) {
                counts.put(status, 0L);
            }
            for (JobApplication application : applications) {
                counts.compute(application.status(), (status, oldCount) -> oldCount == null ? 1L : oldCount + 1L);
            }

            Map<String, Double> values = new LinkedHashMap<>();
            for (Map.Entry<ApplicationStatus, Long> entry : counts.entrySet()) {
                if (entry.getValue() > 0) {
                    values.put(entry.getKey().name(), entry.getValue().doubleValue());
                }
            }
            if (values.isEmpty()) {
                values.put("No applications", 1.0);
            }

            applicationStatusView.update(values, hoverTooltip);
        }

        private void updateRiskStatus(List<WorkloadSummary> workloads) {
            long invalidScheduleCount = workloads.stream().filter(WorkloadSummary::hasInvalidScheduleData).count();
            long conflictCount = workloads.stream()
                .filter(summary -> !summary.hasInvalidScheduleData() && summary.hasConflict())
                .count();
            long overloadedCount = workloads.stream()
                .filter(summary -> !summary.hasInvalidScheduleData() && !summary.hasConflict() && summary.overloaded())
                .count();
            long normalCount = workloads.stream()
                .filter(summary -> !summary.hasInvalidScheduleData() && !summary.hasConflict() && !summary.overloaded())
                .count();

            Map<String, Double> values = new LinkedHashMap<>();
            if (normalCount > 0) {
                values.put("Normal", (double) normalCount);
            }
            if (overloadedCount > 0) {
                values.put("Overloaded", (double) overloadedCount);
            }
            if (conflictCount > 0) {
                values.put("Conflict", (double) conflictCount);
            }
            if (invalidScheduleCount > 0) {
                values.put("Invalid schedule", (double) invalidScheduleCount);
            }
            if (values.isEmpty()) {
                values.put("No accepted TA data", 1.0);
            }

            riskStatusView.update(values, hoverTooltip);
        }

        private void updateJobDemand(List<JobApplication> applications, List<JobPosting> jobs) {
            Map<String, Long> countByJob = applications.stream()
                .collect(Collectors.groupingBy(JobApplication::jobId, Collectors.counting()));

            Map<String, String> titleByJob = jobs.stream()
                .collect(Collectors.toMap(JobPosting::jobId, JobPosting::title, (left, right) -> left));

            List<ChartItem> items = countByJob.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(entry -> new ChartItem(
                    shorten(entry.getKey(), 10),
                    entry.getValue(),
                    entry.getKey() + " | " + titleByJob.getOrDefault(entry.getKey(), entry.getKey())
                ))
                .toList();

            if (items.isEmpty()) {
                items = List.of(new ChartItem("No data", 0, "No job application data"));
            }

            jobDemandView.update(items, applications.size(), hoverTooltip);
        }

        private void updateWorkloadRanking(List<WorkloadSummary> workloads) {
            List<ChartItem> items = workloads.stream()
                .sorted(Comparator.comparingDouble(WorkloadSummary::totalWeeklyHours).reversed())
                .limit(8)
                .map(summary -> new ChartItem(
                    shorten(summary.applicantDisplayName(), 18),
                    summary.totalWeeklyHours(),
                    summary.applicantDisplayName() + " (" + summary.applicantUserId() + ")"
                ))
                .toList();

            if (items.isEmpty()) {
                items = List.of(new ChartItem("No accepted TA data", 0, "No accepted TA workload data"));
            }

            workloadRankingView.update(items, hoverTooltip);
        }

        private String buildAnalysisText(List<JobApplication> applications, List<JobPosting> jobs, List<WorkloadSummary> workloads) {
            StringBuilder builder = new StringBuilder();
            builder.append("This dashboard reads real repository data and updates each custom component in place. ");

            if (!applications.isEmpty()) {
                Map<ApplicationStatus, Long> statusCounts = new EnumMap<>(ApplicationStatus.class);
                for (ApplicationStatus status : ApplicationStatus.values()) {
                    statusCounts.put(status, 0L);
                }
                for (JobApplication application : applications) {
                    statusCounts.compute(application.status(), (status, count) -> count == null ? 1L : count + 1L);
                }

                ApplicationStatus topStatus = statusCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

                if (topStatus != null) {
                    builder.append("The most common application status is ")
                        .append(topStatus.name())
                        .append(". ");
                }
            }

            Map<String, Long> countByJob = applications.stream()
                .collect(Collectors.groupingBy(JobApplication::jobId, LinkedHashMap::new, Collectors.counting()));

            if (!countByJob.isEmpty()) {
                String topJobId = countByJob.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

                if (topJobId != null) {
                    String topTitle = jobs.stream()
                        .filter(job -> job.jobId().equals(topJobId))
                        .map(JobPosting::title)
                        .findFirst()
                        .orElse(topJobId);

                    builder.append("The highest-demand job is ")
                        .append(topJobId)
                        .append(" ")
                        .append(topTitle)
                        .append(". ");
                }
            }

            if (!workloads.isEmpty()) {
                WorkloadSummary heaviest = workloads.stream()
                    .max(Comparator.comparingDouble(WorkloadSummary::totalWeeklyHours))
                    .orElse(null);

                if (heaviest != null) {
                    builder.append("The highest accepted TA workload is ")
                        .append(heaviest.applicantDisplayName())
                        .append(" with ")
                        .append(DisplayFormats.formatDecimal(heaviest.totalWeeklyHours()))
                        .append(" hours per week. ");
                }

                long riskCount = workloads.stream()
                    .filter(summary -> summary.overloaded() || summary.hasConflict() || summary.hasInvalidScheduleData())
                    .count();

                builder.append(riskCount)
                    .append(" accepted TA record(s) currently have at least one risk signal.");
            }

            return builder.toString();
        }

        private void stopAutoRefresh() {
            if (autoRefresh != null) {
                autoRefresh.stop();
            }
        }

        private void configureTooltip() {
            hoverTooltip.setShowDelay(Duration.ZERO);
            hoverTooltip.setHideDelay(Duration.ZERO);
            hoverTooltip.setShowDuration(Duration.INDEFINITE);
            hoverTooltip.setStyle(
                "-fx-font-size: 13px;" +
                    "-fx-background-color: " + DARK + ";" +
                    "-fx-text-fill: white;" +
                    "-fx-padding: 9;" +
                    "-fx-background-radius: 10;"
            );
        }
    }

    private static final class TrendLineView {
        private final VBox root = new VBox(12);
        private final Pane chartPane = new Pane();
        private final HBox labels = new HBox();
        private boolean animated;

        private TrendLineView() {
            chartPane.setPrefSize(600, 260);
            chartPane.setMinHeight(260);
            chartPane.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #fbfcff, #ffffff);" +
                    "-fx-background-radius: 22;" +
                    "-fx-border-color: #eef0fb;" +
                    "-fx-border-radius: 22;"
            );

            labels.setAlignment(Pos.CENTER);
            labels.setSpacing(24);

            root.getChildren().addAll(chartPane, labels);
        }

        private VBox container() {
            return root;
        }

        private void update(List<ChartItem> items, Tooltip tooltip) {
            boolean animate = !animated;
            chartPane.getChildren().clear();
            labels.getChildren().clear();

            double width = 560;
            double height = 210;
            double left = 24;
            double top = 24;

            double max = items.stream().mapToDouble(ChartItem::value).max().orElse(1);
            if (max <= 0) {
                max = 1;
            }

            for (int i = 0; i <= 4; i++) {
                double y = top + i * height / 4;
                Rectangle line = new Rectangle(left, y, width, 1);
                line.setFill(Color.web("#eef0fb"));
                chartPane.getChildren().add(line);
            }

            List<Double> xs = new ArrayList<>();
            List<Double> ys = new ArrayList<>();

            for (int i = 0; i < items.size(); i++) {
                double x = left + (items.size() == 1 ? width / 2 : i * width / (items.size() - 1));
                double y = top + height - (items.get(i).value() / max * (height - 20));
                xs.add(x);
                ys.add(y);
            }

            Path area = new Path();
            if (!items.isEmpty()) {
                area.getElements().add(new MoveTo(xs.get(0), top + height));
                area.getElements().add(new LineTo(xs.get(0), ys.get(0)));

                for (int i = 1; i < items.size(); i++) {
                    double cx1 = (xs.get(i - 1) + xs.get(i)) / 2;
                    double cy1 = ys.get(i - 1);
                    double cx2 = (xs.get(i - 1) + xs.get(i)) / 2;
                    double cy2 = ys.get(i);
                    area.getElements().add(new CubicCurveTo(cx1, cy1, cx2, cy2, xs.get(i), ys.get(i)));
                }

                area.getElements().add(new LineTo(xs.get(xs.size() - 1), top + height));
                area.getElements().add(new LineTo(xs.get(0), top + height));
            }

            area.setFill(new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#8b5cf633")),
                new Stop(1, Color.web("#8b5cf600"))
            ));

            Path line = new Path();
            if (!items.isEmpty()) {
                line.getElements().add(new MoveTo(xs.get(0), ys.get(0)));
                for (int i = 1; i < items.size(); i++) {
                    double cx1 = (xs.get(i - 1) + xs.get(i)) / 2;
                    double cy1 = ys.get(i - 1);
                    double cx2 = (xs.get(i - 1) + xs.get(i)) / 2;
                    double cy2 = ys.get(i);
                    line.getElements().add(new CubicCurveTo(cx1, cy1, cx2, cy2, xs.get(i), ys.get(i)));
                }
            }

            line.setFill(Color.TRANSPARENT);
            line.setStroke(Color.web(PURPLE));
            line.setStrokeWidth(4);
            line.setStrokeLineCap(StrokeLineCap.ROUND);

            Pane plotLayer = new Pane(area, line);
            plotLayer.setPickOnBounds(false);
            if (animate) {
                Rectangle revealClip = new Rectangle(left, top - 4, 0, height + 12);
                plotLayer.setClip(revealClip);
                animateRectangleWidth(revealClip, width + 8, CHART_ANIMATION_DELAY);
            }
            chartPane.getChildren().add(plotLayer);

            for (int i = 0; i < items.size(); i++) {
                ChartItem item = items.get(i);
                Circle dot = new Circle(xs.get(i), ys.get(i), 6);
                dot.setFill(Color.WHITE);
                dot.setStroke(Color.web(PURPLE));
                dot.setStrokeWidth(3);

                installManualHover(dot, tooltip, () ->
                    item.label()
                        + "\nCount: " + DisplayFormats.formatDecimal(item.value())
                );

                if (animate) {
                    animateNodePop(dot, CHART_ANIMATION_DELAY.add(Duration.millis(160 + i * DOT_STAGGER_MILLIS)));
                }

                chartPane.getChildren().add(dot);

                Label label = new Label(item.label());
                label.setFont(Font.font("Arial", 12));
                label.setStyle("-fx-text-fill: " + MUTED + ";");
                labels.getChildren().add(label);
            }

            animated = true;
        }
    }

    private static final class DonutChartView {
        private final VBox root = new VBox(14);
        private final Pane chartPane = new Pane();
        private final VBox legendBox = new VBox(8);
        private final Label centerValue = new Label("0");
        private final Label centerText = new Label("Total");
        private final List<Arc> arcs = new ArrayList<>();
        private boolean animated;

        private DonutChartView() {
            chartPane.setPrefSize(320, 250);
            chartPane.setMinHeight(250);

            Circle track = new Circle(160, 125, 82);
            track.setFill(Color.TRANSPARENT);
            track.setStroke(Color.web("#eef0fb"));
            track.setStrokeWidth(28);

            centerValue.setFont(Font.font("Arial", FontWeight.BOLD, 30));
            centerValue.setStyle("-fx-text-fill: " + BLUE + ";");
            centerText.setFont(Font.font("Arial", 13));
            centerText.setStyle("-fx-text-fill: " + MUTED + ";");

            VBox centerBox = new VBox(0, centerValue, centerText);
            centerBox.setAlignment(Pos.CENTER);
            centerBox.setLayoutX(116);
            centerBox.setLayoutY(96);
            centerBox.setPrefWidth(88);

            chartPane.getChildren().addAll(track, centerBox);

            HBox body = new HBox(18, chartPane, legendBox);
            body.setAlignment(Pos.CENTER_LEFT);

            root.getChildren().add(body);
        }

        private VBox container() {
            return root;
        }

        private void update(Map<String, Double> values, Tooltip tooltip) {
            boolean animate = !animated;
            double total = values.values().stream().mapToDouble(Double::doubleValue).sum();
            centerValue.setText(DisplayFormats.formatDecimal(total));

            chartPane.getChildren().removeAll(arcs);
            arcs.clear();
            legendBox.getChildren().clear();

            if (total <= 0) {
                return;
            }

            double start = 90;
            int index = 0;
            for (Map.Entry<String, Double> entry : values.entrySet()) {
                double value = entry.getValue();
                double length = -360.0 * value / total;
                String color = colorAt(index);

                Arc arc = new Arc(160, 125, 82, 82, start, length);
                arc.setType(ArcType.OPEN);
                arc.setFill(Color.TRANSPARENT);
                arc.setStroke(Color.web(color));
                arc.setStrokeWidth(28);
                arc.setStrokeLineCap(StrokeLineCap.ROUND);
                if (animate) {
                    arc.setLength(0);
                    animateArcLength(arc, length, CHART_ANIMATION_DELAY.add(Duration.millis(index * RING_STAGGER_MILLIS)));
                }

                String text = entry.getKey()
                    + "\nCount: " + DisplayFormats.formatDecimal(value)
                    + "\nShare: " + DisplayFormats.formatDecimal(value / total * 100) + "%";

                installManualHover(arc, tooltip, () -> text);
                arcs.add(arc);
                chartPane.getChildren().add(arc);

                legendBox.getChildren().add(createLegendRow(color, entry.getKey(), value, total));

                start += length;
                index++;
            }
            animated = true;
        }
    }

    private static final class HorizontalDemandView {
        private final VBox root = new VBox(12);
        private final VBox rows = new VBox(9);
        private final VBox insightBox = new VBox(6);
        private boolean animated;

        private HorizontalDemandView() {
            rows.setPadding(new Insets(12, 0, 4, 0));
            insightBox.setPadding(new Insets(14));
            insightBox.setStyle(
                "-fx-background-color: #fff7fb;" +
                    "-fx-background-radius: 18;" +
                    "-fx-border-color: #eef0fb;" +
                    "-fx-border-radius: 18;"
            );

            root.getChildren().addAll(rows, insightBox);
        }

        private VBox container() {
            return root;
        }

        private void update(List<ChartItem> items, int totalApplications, Tooltip tooltip) {
            boolean animate = !animated;
            rows.getChildren().clear();
            insightBox.getChildren().clear();

            if (items.isEmpty() || "No data".equals(items.get(0).label())) {
                Label empty = new Label("No application demand data yet.");
                empty.setStyle("-fx-text-fill: " + MUTED + ";");
                rows.getChildren().add(empty);
                updateInsight(items, totalApplications, tooltip);
                return;
            }

            double max = items.stream().mapToDouble(ChartItem::value).max().orElse(1);
            if (max <= 0) {
                max = 1;
            }

            for (int i = 0; i < items.size(); i++) {
                rows.getChildren().add(createDemandRow(items.get(i), max, tooltip, animate, i));
            }

            updateInsight(items, totalApplications, tooltip);
            animated = true;
        }

        private HBox createDemandRow(ChartItem item, double max, Tooltip tooltip, boolean animate, int index) {
            Label name = new Label(item.detail());
            name.setMinWidth(220);
            name.setPrefWidth(260);
            name.setWrapText(true);
            name.setFont(Font.font("Arial", FontWeight.BOLD, 13));
            name.setStyle("-fx-text-fill: " + BLUE + ";");

            Region bar = new Region();
            bar.setMinHeight(18);
            double targetWidth = Math.max(18, 340.0 * item.value() / max);
            bar.setMinWidth(0);
            bar.setPrefWidth(animate ? 0 : targetWidth);
            bar.setStyle("-fx-background-color: #b8d5ff; -fx-background-radius: 9;");
            if (animate) {
                animateRegionPrefWidth(bar, targetWidth, CHART_ANIMATION_DELAY.add(Duration.millis(index * BAR_STAGGER_MILLIS)));
            }

            HBox barTrack = new HBox(bar);
            barTrack.setAlignment(Pos.CENTER_LEFT);
            barTrack.setMinWidth(340);
            barTrack.setPrefWidth(340);
            barTrack.setMaxWidth(340);

            Label count = new Label(DisplayFormats.formatDecimal(item.value()) + " applications");
            count.setMinWidth(108);
            count.setFont(Font.font("Arial", FontWeight.BOLD, 13));
            count.setStyle("-fx-text-fill: #5c6481;");

            HBox row = new HBox(12, name, barTrack, count);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMinHeight(32);

            installManualHover(row, tooltip, () ->
                item.detail()
                    + "\nApplications: " + DisplayFormats.formatDecimal(item.value())
            );

            return row;
        }

        private void updateInsight(List<ChartItem> items, int totalApplications, Tooltip tooltip) {
            Label heading = new Label("Top demand insight");
            heading.setFont(Font.font("Arial", FontWeight.BOLD, 15));
            heading.setStyle("-fx-text-fill: " + BLUE + ";");

            insightBox.getChildren().add(heading);

            if (items.isEmpty() || "No data".equals(items.get(0).label())) {
                Label empty = new Label("No application demand data yet.");
                empty.setStyle("-fx-text-fill: " + MUTED + ";");
                insightBox.getChildren().add(empty);
                return;
            }

            ChartItem top = items.get(0);
            double concentration = totalApplications <= 0 ? 0 : top.value() / totalApplications * 100;

            Label topLine = new Label("#1 " + top.detail());
            topLine.setFont(Font.font("Arial", FontWeight.BOLD, 13));
            topLine.setStyle("-fx-text-fill: " + DARK + ";");
            topLine.setWrapText(true);

            Label meta = new Label(
                DisplayFormats.formatDecimal(top.value())
                    + " applications · "
                    + DisplayFormats.formatDecimal(concentration)
                    + "% of all applications"
            );
            meta.setStyle("-fx-text-fill: " + MUTED + ";");
            meta.setWrapText(true);

            HBox hotJobs = new HBox(10);
            hotJobs.setAlignment(Pos.CENTER_LEFT);

            for (int i = 0; i < Math.min(3, items.size()); i++) {
                ChartItem item = items.get(i);
                Label chip = new Label((i + 1) + ". " + item.label() + "  " + DisplayFormats.formatDecimal(item.value()));
                chip.setFont(Font.font("Arial", FontWeight.BOLD, 12));
                chip.setStyle(
                    "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: #eef0fb;" +
                        "-fx-border-radius: 14;" +
                        "-fx-text-fill: " + BLUE + ";" +
                        "-fx-padding: 6 10 6 10;"
                );

                installManualHover(chip, tooltip, () ->
                    item.detail()
                        + "\nApplications: " + DisplayFormats.formatDecimal(item.value())
                );

                hotJobs.getChildren().add(chip);
            }

            insightBox.getChildren().addAll(topLine, meta, hotJobs);
        }
    }

    private static final class RankingView {
        private final VBox root = new VBox(12);
        private boolean animated;

        private RankingView() {
            root.setMinHeight(300);
        }

        private VBox container() {
            return root;
        }

        private void update(List<ChartItem> items, Tooltip tooltip) {
            boolean animate = !animated;
            root.getChildren().clear();

            double max = items.stream().mapToDouble(ChartItem::value).max().orElse(1);
            if (max <= 0) {
                max = 1;
            }

            for (int i = 0; i < items.size(); i++) {
                ChartItem item = items.get(i);
                Label name = new Label(item.label());
                name.setFont(Font.font("Arial", FontWeight.BOLD, 13));
                name.setStyle("-fx-text-fill: " + DARK + ";");
                name.setPrefWidth(160);

                Label value = new Label(DisplayFormats.formatDecimal(item.value()) + "h");
                value.setFont(Font.font("Arial", FontWeight.BOLD, 13));
                value.setStyle("-fx-text-fill: " + BLUE + ";");
                value.setPrefWidth(52);

                double targetWidth = Math.max(8, 250 * item.value() / max);
                Rectangle bar = new Rectangle(animate ? 0 : targetWidth, 14);
                bar.setArcWidth(14);
                bar.setArcHeight(14);
                bar.setFill(new LinearGradient(
                    0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web(PURPLE)),
                    new Stop(1, Color.web(CYAN))
                ));
                if (animate) {
                    animateRectangleWidth(bar, targetWidth, CHART_ANIMATION_DELAY.add(Duration.millis(i * BAR_STAGGER_MILLIS)));
                }

                StackPane track = new StackPane();
                track.setAlignment(Pos.CENTER_LEFT);
                track.setPrefHeight(24);
                track.setMaxWidth(Double.MAX_VALUE);
                track.setStyle("-fx-background-color: #eef0fb; -fx-background-radius: 14;");
                track.getChildren().add(bar);

                HBox row = new HBox(10, name, track, value);
                row.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(track, Priority.ALWAYS);

                installManualHover(row, tooltip, () ->
                    "TA: " + item.detail()
                        + "\nWeekly hours: " + DisplayFormats.formatDecimal(item.value())
                );

                root.getChildren().add(row);
            }

            animated = true;
        }
    }

    private record ChartItem(String label, double value, String detail) {
    }

    private static VBox createKpiCard(String title, Label valueLabel, String subtitle, String color, String iconText) {
        Label icon = new Label(iconText);
        icon.setAlignment(Pos.CENTER);
        icon.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        icon.setStyle(
            "-fx-text-fill: white;" +
                "-fx-background-color: " + color + ";" +
                "-fx-background-radius: 22;"
        );
        icon.setPrefSize(44, 44);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        titleLabel.setStyle("-fx-text-fill: " + DARK + ";");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setFont(Font.font("Arial", 12));
        subtitleLabel.setStyle("-fx-text-fill: " + MUTED + ";");
        subtitleLabel.setWrapText(true);

        VBox textBox = new VBox(2, titleLabel, valueLabel, subtitleLabel);
        HBox row = new HBox(12, icon, textBox);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(row);
        card.setPadding(new Insets(16));
        card.setMinHeight(108);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(
            "-fx-background-color: white;" +
                "-fx-background-radius: 24;" +
                "-fx-effect: dropshadow(gaussian, rgba(70, 100, 168, 0.13), 18, 0, 0, 6);" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-width: 1.4;" +
                "-fx-border-radius: 24;"
        );
        return card;
    }

    private static VBox createDashboardCard(String title, String subtitle, Node body) {
        VBox card = cardBase();
        card.setSpacing(14);

        Label titleLabel = titleLabel(title);
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setWrapText(true);
        subtitleLabel.setFont(Font.font("Arial", 13));
        subtitleLabel.setStyle("-fx-text-fill: " + MUTED + ";");

        card.getChildren().addAll(titleLabel, subtitleLabel, body);
        return card;
    }

    private static VBox cardBase() {
        VBox card = new VBox();
        card.setPadding(new Insets(22));
        card.setMinHeight(430);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(
            "-fx-background-color: white;" +
                "-fx-background-radius: 26;" +
                "-fx-effect: dropshadow(gaussian, rgba(70, 100, 168, 0.14), 22, 0, 0, 8);" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-width: 1.4;" +
                "-fx-border-radius: 26;"
        );
        return card;
    }

    private static Label titleLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 21));
        label.setStyle("-fx-text-fill: " + BLUE + ";");
        return label;
    }

    private static Label metricValueLabel() {
        Label label = new Label("0");
        label.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        label.setStyle("-fx-text-fill: " + BLUE + ";");
        return label;
    }

    private static HBox createLegendRow(String color, String name, double value, double total) {
        Circle dot = new Circle(6, Color.web(color));

        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        nameLabel.setStyle("-fx-text-fill: " + DARK + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label valueLabel = new Label(DisplayFormats.formatDecimal(value / total * 100) + "%");
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        valueLabel.setStyle("-fx-text-fill: " + BLUE + ";");

        HBox row = new HBox(8, dot, nameLabel, spacer, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMinWidth(190);
        return row;
    }

    private static void animateArcLength(Arc arc, double targetLength, Duration delay) {
        Timeline draw = new Timeline(
            new KeyFrame(
                CHART_ANIMATION_DURATION,
                new KeyValue(arc.lengthProperty(), targetLength, Interpolator.EASE_OUT)
            )
        );
        draw.setDelay(delay);
        draw.play();
    }

    private static void animateRegionPrefWidth(Region region, double targetWidth, Duration delay) {
        Timeline grow = new Timeline(
            new KeyFrame(
                CHART_ANIMATION_DURATION,
                new KeyValue(region.prefWidthProperty(), targetWidth, Interpolator.EASE_OUT)
            )
        );
        grow.setDelay(delay);
        grow.play();
    }

    private static void animateRectangleWidth(Rectangle rectangle, double targetWidth, Duration delay) {
        Timeline grow = new Timeline(
            new KeyFrame(
                CHART_ANIMATION_DURATION,
                new KeyValue(rectangle.widthProperty(), targetWidth, Interpolator.EASE_OUT)
            )
        );
        grow.setDelay(delay);
        grow.play();
    }

    private static void animateNodePop(Node node, Duration delay) {
        node.setScaleX(0);
        node.setScaleY(0);

        ScaleTransition pop = new ScaleTransition(DOT_POP_DURATION, node);
        pop.setFromX(0);
        pop.setFromY(0);
        pop.setToX(1);
        pop.setToY(1);
        pop.setDelay(delay);
        pop.setInterpolator(Interpolator.EASE_OUT);
        pop.play();
    }

    private static void installManualHover(Node node, Tooltip tooltip, Supplier<String> textSupplier) {
        node.setOnMouseEntered(event -> {
            tooltip.setText(textSupplier.get());
            tooltip.show(node, event.getScreenX() + 12, event.getScreenY() + 12);
        });

        node.setOnMouseMoved(event -> {
            tooltip.setText(textSupplier.get());
            if (!tooltip.isShowing()) {
                tooltip.show(node, event.getScreenX() + 12, event.getScreenY() + 12);
            } else {
                tooltip.setAnchorX(event.getScreenX() + 12);
                tooltip.setAnchorY(event.getScreenY() + 12);
            }
        });

        node.setOnMouseExited(event -> tooltip.hide());
        node.setStyle(node.getStyle() + "; -fx-cursor: hand;");
    }

    private static String colorAt(int index) {
        String[] colors = {BLUE, ORANGE, GREEN, RED, PURPLE, CYAN};
        return colors[index % colors.length];
    }

    private static String shorten(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    public static void main(String[] args) {
        launch(args);
    }
}
