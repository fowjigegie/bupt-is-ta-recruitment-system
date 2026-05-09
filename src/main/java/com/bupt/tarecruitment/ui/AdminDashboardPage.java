package com.bupt.tarecruitment.ui;

import javafx.application.Application;
import com.bupt.tarecruitment.admin.WorkloadSummary;
import com.bupt.tarecruitment.application.ApplicationStatus;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.auth.UserRole;
import com.bupt.tarecruitment.common.text.DisplayFormats;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobStatus;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.EnumMap;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员首页，负责系统概览和页面装配。
 */
public class AdminDashboardPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.ADMIN_DASHBOARD, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        long userCount = context.services().userRepository().findAll().stream()
            .filter(user -> user.role() != UserRole.ADMIN)
            .count();
        long jobCount = context.services().jobRepository().findAll().size();
        long applicationCount = context.services().applicationRepository().findAll().size();
        long acceptedCount = countApplications(context, ApplicationStatus.ACCEPTED);
        List<WorkloadSummary> workloads = context.services().adminWorkloadService().listAcceptedTaWorkloads(10);
        double averageWorkload = workloads.isEmpty()
            ? 0
            : workloads.stream().mapToDouble(WorkloadSummary::totalWeeklyHours).sum() / workloads.size();
        long applicantCount = context.services().userRepository().findAll().stream()
            .filter(user -> user.role() == UserRole.APPLICANT)
            .count();
        long moCount = context.services().userRepository().findAll().stream()
            .filter(user -> user.role() == UserRole.MO)
            .count();
        long openJobCount = context.services().jobRepository().findAll().stream()
            .filter(job -> job.status() == JobStatus.OPEN)
            .count();
        long closedJobCount = context.services().jobRepository().findAll().stream()
            .filter(job -> job.status() == JobStatus.CLOSED)
            .count();

        VBox center = new VBox(18);
        center.setPadding(new Insets(32, 42, 30, 42));
        center.setStyle("-fx-text-fill: #2f3553;");

        Button userManagementButton = createDashboardActionButton("User management", 190);
        userManagementButton.setOnAction(event -> showUserManagementDialog(context));
        Button jobManagementButton = createDashboardActionButton("Job management", 170);
        jobManagementButton.setOnAction(event -> showJobManagementDialog(context));
        Button applicationManagementButton = createDashboardActionButton("Application management", 230);
        applicationManagementButton.setOnAction(event -> showApplicationManagementDialog(context));
        Button aiSystemAnalysisButton = createDashboardActionButton("AI system analysis", 210);
        aiSystemAnalysisButton.setOnAction(event -> AdminAiAnalysisDialog.showSystemAnalysis(
            aiSystemAnalysisButton.getScene() == null ? null : aiSystemAnalysisButton.getScene().getWindow(),
            context
        ));

        HBox stats = new HBox(20,
            createAdminStatCard(
                "Users",
                Long.toString(userCount),
                "MO + Applicant records",
                userManagementButton,
                "U",
                "#8b5cf6"
            ),
            createAdminStatCard(
                "Jobs",
                Long.toString(jobCount),
                "Current postings",
                jobManagementButton,
                "J",
                "#f39c12"
            ),
            createAdminStatCard(
                "Applications",
                Long.toString(applicationCount),
                "Submitted records",
                applicationManagementButton,
                "A",
                "#4664a8"
            ),
            createAdminStatCard(
                "Accepted",
                Long.toString(acceptedCount),
                "Accepted records",
                null,
                "OK",
                "#ff8a65"
            ),
            createAdminStatCard(
                "Avg workload",
                DisplayFormats.formatDecimal(averageWorkload),
                "Hours per TA",
                null,
                "h",
                "#59c7dc"
            )
        );
        stats.setMaxWidth(Double.MAX_VALUE);
        stats.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));

        HBox toolRow = new HBox(20, AdminDataIntegrityPanel.create(context), AdminGlobalSearchPanel.create(context));
        toolRow.setMaxWidth(Double.MAX_VALUE);
        toolRow.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));

        AdminWorkloadPanel workloadPanel = AdminWorkloadPanel.create(context);

        center.getChildren().addAll(
            createAdminHeader(aiSystemAnalysisButton),
            UiTheme.createMutedText("Use this page to monitor accepted TA allocations and spot overload, schedule conflict, or schedule-data risks."),
            stats,
            createQuickOverviewPanel(context, workloads, nav),
            createTopJobsPanel(context),
            toolRow,
            workloadPanel.container()
        );
        forceReadableText(center);

        ScrollPane pageScroll = new ScrollPane(center);
        pageScroll.setFitToWidth(true);
        pageScroll.setPannable(true);
        pageScroll.setStyle(
            "-fx-background-color: transparent;" +
                "-fx-background: transparent;" +
                "-fx-border-color: transparent;"
        );

        BorderPane root = UiTheme.createPage("Admin Dashboard", null, pageScroll, nav, context);
        return UiTheme.createScene(root);
    }

    private static HBox createAdminHeader(Button aiSystemAnalysisButton) {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        Label heading = UiTheme.createPageHeading("Admin dashboard");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(heading, spacer, aiSystemAnalysisButton);
        return header;
    }

    private static void forceReadableText(Node node) {
        if (node instanceof Label label) {
            String existingStyle = label.getStyle() == null ? "" : label.getStyle();
            String normalizedStyle = existingStyle.toLowerCase();
            if (!normalizedStyle.contains("-fx-text-fill: white")
                && !normalizedStyle.contains("-fx-text-fill: #ffffff")) {
                label.setStyle(existingStyle + "; -fx-text-fill: #2f3553;");
            }
        }

        if (node instanceof ScrollPane scrollPane && scrollPane.getContent() != null) {
            forceReadableText(scrollPane.getContent());
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                forceReadableText(child);
            }
        }
    }

    private static VBox createAdminStatCard(String title, String value, String subtitle) {
        return createAdminStatCard(title, value, subtitle, null, title.substring(0, 1), "#4664a8");
    }

    private static VBox createAdminStatCard(String title, String value, String subtitle, Button actionButton) {
        return createAdminStatCard(title, value, subtitle, actionButton, title.substring(0, 1), "#4664a8");
    }

    private static VBox createAdminStatCard(
        String title,
        String value,
        String subtitle,
        Button actionButton,
        String iconText,
        String iconColor
    ) {
        Label icon = new Label(iconText);
        icon.setAlignment(Pos.CENTER);
        icon.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        icon.setPrefSize(42, 42);
        icon.setMinSize(42, 42);
        icon.setStyle(
            "-fx-background-color: " + iconColor + ";" +
                "-fx-background-radius: 21;" +
                "-fx-text-fill: white;"
        );

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        titleLabel.setStyle("-fx-text-fill: #2f3553;");

        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        valueLabel.setStyle("-fx-text-fill: #4664a8;");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setFont(Font.font("Arial", 12));
        subtitleLabel.setStyle("-fx-text-fill: #8b7fa0;");
        subtitleLabel.setWrapText(true);

        VBox textBox = new VBox(1, titleLabel, valueLabel, subtitleLabel);
        HBox topRow = new HBox(12, icon, textBox);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(10, topRow);
        if (actionButton != null) {
            actionButton.setMinWidth(0);
            actionButton.setMaxWidth(Double.MAX_VALUE);
            actionButton.setPrefHeight(34);
            card.getChildren().add(actionButton);
        }
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16));
        card.setMinHeight(126);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setBackground(new Background(new BackgroundFill(
            Color.WHITE,
            new CornerRadii(24),
            Insets.EMPTY
        )));
        card.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(24),
            new BorderWidths(1.5)
        )));
        return card;
    }

    private static long countApplications(UiAppContext context, ApplicationStatus status) {
        return context.services().applicationRepository().findAll().stream()
            .filter(application -> application.status() == status)
            .count();
    }

    private static VBox createQuickOverviewPanel(UiAppContext context, List<WorkloadSummary> workloads, NavigationManager nav) {
        Label title = new Label("Quick overview");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        title.setStyle("-fx-text-fill: #2f3553;");

        Label subtitle = new Label("A compact snapshot. Open the analytics report for full charts and trends.");
        subtitle.setFont(Font.font("Arial", 13));
        subtitle.setStyle("-fx-text-fill: #8b7fa0;");
        subtitle.setWrapText(true);

        VBox heading = new VBox(4, title, subtitle);

        Button analyticsButton = createDashboardActionButton("View detailed analytics", 220);
        analyticsButton.setOnAction(event -> nav.goTo(PageId.ADMIN_ANALYTICS));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(16, heading, spacer, analyticsButton);
        header.setAlignment(Pos.CENTER_LEFT);

        HBox summaryRow = new HBox(14,
            createOverviewChip("Applications", buildApplicationStatusSummary(context), "#4664a8"),
            createOverviewChip("Risk", buildRiskSummary(workloads), riskCount(workloads) == 0 ? "#2e7d32" : "#e74c3c"),
            createOverviewChip("Top job", buildTopJobSummary(context), "#f39c12")
        );
        summaryRow.setMaxWidth(Double.MAX_VALUE);
        summaryRow.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));

        VBox panel = new VBox(14, header, summaryRow);
        panel.setPadding(new Insets(18, 24, 18, 24));
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

    private static VBox createOverviewChip(String label, String value, String accentColor) {
        Label labelNode = new Label(label);
        labelNode.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        labelNode.setStyle("-fx-text-fill: " + accentColor + ";");

        Label valueNode = new Label(value);
        valueNode.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        valueNode.setStyle("-fx-text-fill: #2f3553;");
        valueNode.setWrapText(true);

        VBox chip = new VBox(4, labelNode, valueNode);
        chip.setPadding(new Insets(12, 14, 12, 14));
        chip.setMinHeight(72);
        chip.setMaxWidth(Double.MAX_VALUE);
        chip.setBackground(new Background(new BackgroundFill(Color.web("#fff7fb"), new CornerRadii(18), Insets.EMPTY)));
        chip.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(18),
            new BorderWidths(1.2)
        )));
        return chip;
    }

    private static String buildApplicationStatusSummary(UiAppContext context) {
        Map<String, Double> values = applicationStatusValues(context);
        if (values.containsKey("No applications")) {
            return "No applications yet";
        }
        return values.entrySet().stream()
            .map(entry -> toDisplayStatus(entry.getKey()) + ": " + DisplayFormats.formatDecimal(entry.getValue()))
            .collect(Collectors.joining(" | "));
    }

    private static String buildRiskSummary(List<WorkloadSummary> workloads) {
        long count = riskCount(workloads);
        if (workloads.isEmpty()) {
            return "No accepted TA data";
        }
        return count == 0 ? "0 risky TAs" : count + " risky TA(s)";
    }

    private static long riskCount(List<WorkloadSummary> workloads) {
        return workloads.stream()
            .filter(summary -> summary.hasInvalidScheduleData() || summary.hasConflict() || summary.overloaded())
            .count();
    }

    private static String buildTopJobSummary(UiAppContext context) {
        Map<String, Long> applicationsByJob = context.services().applicationRepository().findAll().stream()
            .collect(Collectors.groupingBy(JobApplication::jobId, Collectors.counting()));
        return context.services().jobRepository().findAll().stream()
            .max(Comparator
                .comparingLong((JobPosting job) -> applicationsByJob.getOrDefault(job.jobId(), 0L))
                .thenComparing(JobPosting::jobId))
            .map(job -> job.title() + " (" + applicationsByJob.getOrDefault(job.jobId(), 0L) + ")")
            .orElse("No jobs yet");
    }

    private static String toDisplayStatus(String status) {
        String normalized = status == null ? "" : status.trim().toLowerCase().replace('_', ' ');
        if (normalized.isBlank()) {
            return "Unknown";
        }
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private static HBox createDashboardAnalyticsRow(UiAppContext context, List<WorkloadSummary> workloads) {
        VBox applicationStatusCard = createAnalyticsCard(
            "Application status analysis",
            "Donut distribution of submitted, accepted, withdrawn and other statuses.",
            createDonutChart(applicationStatusValues(context), "Total")
        );
        VBox riskRingCard = createAnalyticsCard(
            "Risk ring",
            "Accepted TA risk categories from workload analysis.",
            createDonutChart(riskStatusValues(workloads), "TAs")
        );

        HBox row = new HBox(20, applicationStatusCard, riskRingCard);
        row.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(applicationStatusCard, Priority.ALWAYS);
        HBox.setHgrow(riskRingCard, Priority.ALWAYS);
        return row;
    }

    private static Map<String, Double> applicationStatusValues(UiAppContext context) {
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            counts.put(status, 0L);
        }
        for (JobApplication application : context.services().applicationRepository().findAll()) {
            counts.compute(application.status(), (status, oldCount) -> oldCount == null ? 1L : oldCount + 1L);
        }

        Map<String, Double> values = new LinkedHashMap<>();
        for (ApplicationStatus status : ApplicationStatus.values()) {
            long count = counts.getOrDefault(status, 0L);
            if (count > 0) {
                values.put(status.name(), (double) count);
            }
        }
        if (values.isEmpty()) {
            values.put("No applications", 1.0);
        }
        return values;
    }

    private static Map<String, Double> riskStatusValues(List<WorkloadSummary> workloads) {
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
        return values;
    }

    private static VBox createAnalyticsCard(String title, String subtitle, Node body) {
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 21));
        titleLabel.setStyle("-fx-text-fill: #4664a8;");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setFont(Font.font("Arial", 13));
        subtitleLabel.setStyle("-fx-text-fill: #8b7fa0;");
        subtitleLabel.setWrapText(true);

        VBox card = new VBox(12, titleLabel, subtitleLabel, body);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setMinHeight(230);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(24), Insets.EMPTY)));
        card.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(24),
            new BorderWidths(1.5)
        )));
        return card;
    }

    private static HBox createDonutChart(Map<String, Double> values, String centerText) {
        double total = values.values().stream().mapToDouble(Double::doubleValue).sum();

        Pane chartPane = new Pane();
        chartPane.setPrefSize(210, 150);
        chartPane.setMinSize(210, 150);

        double centerX = 105;
        double centerY = 75;
        double radius = 48;

        Circle track = new Circle(centerX, centerY, radius);
        track.setFill(Color.TRANSPARENT);
        track.setStroke(Color.web("#eef0fb"));
        track.setStrokeWidth(17);
        chartPane.getChildren().add(track);

        if (total > 0) {
            double start = 90;
            int index = 0;
            for (Map.Entry<String, Double> entry : values.entrySet()) {
                double length = -360.0 * entry.getValue() / total;
                Arc arc = new Arc(centerX, centerY, radius, radius, start, length);
                arc.setType(ArcType.OPEN);
                arc.setFill(Color.TRANSPARENT);
                arc.setStroke(Color.web(colorAt(index)));
                arc.setStrokeWidth(17);
                arc.setStrokeLineCap(StrokeLineCap.ROUND);
                chartPane.getChildren().add(arc);
                start += length;
                index++;
            }
        }

        Label centerValue = new Label(DisplayFormats.formatDecimal(total));
        centerValue.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        centerValue.setStyle("-fx-text-fill: #4664a8;");
        Label centerCaption = new Label(centerText);
        centerCaption.setFont(Font.font("Arial", 12));
        centerCaption.setStyle("-fx-text-fill: #8b7fa0;");
        VBox centerBox = new VBox(0, centerValue, centerCaption);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setLayoutX(68);
        centerBox.setLayoutY(51);
        centerBox.setPrefWidth(74);
        chartPane.getChildren().add(centerBox);

        VBox legend = new VBox(8);
        int index = 0;
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            legend.getChildren().add(createLegendRow(colorAt(index), entry.getKey(), entry.getValue(), total));
            index++;
        }

        HBox body = new HBox(16, chartPane, legend);
        body.setAlignment(Pos.CENTER_LEFT);
        return body;
    }

    private static HBox createLegendRow(String color, String name, double value, double total) {
        Circle dot = new Circle(6, Color.web(color));

        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        nameLabel.setStyle("-fx-text-fill: #2f3553;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label valueLabel = new Label(DisplayFormats.formatDecimal(total <= 0 ? 0 : value / total * 100) + "%");
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        valueLabel.setStyle("-fx-text-fill: #4664a8;");

        HBox row = new HBox(8, dot, nameLabel, spacer, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMinWidth(190);
        return row;
    }

    private static String colorAt(int index) {
        String[] colors = {"#4664a8", "#f39c12", "#2e7d32", "#e74c3c", "#8b5cf6", "#59c7dc"};
        return colors[index % colors.length];
    }

    private static VBox createTopJobsPanel(UiAppContext context) {
        Map<String, Long> applicationsByJob = context.services().applicationRepository().findAll().stream()
            .collect(Collectors.groupingBy(JobApplication::jobId, Collectors.counting()));
        List<JobPosting> topJobs = context.services().jobRepository().findAll().stream()
            .sorted(Comparator
                .comparingLong((JobPosting job) -> applicationsByJob.getOrDefault(job.jobId(), 0L))
                .reversed()
                .thenComparing(JobPosting::jobId))
            .limit(3)
            .toList();
        long maxApplications = topJobs.stream()
            .mapToLong(job -> applicationsByJob.getOrDefault(job.jobId(), 0L))
            .max()
            .orElse(1);

        Label title = new Label("Top jobs preview");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        title.setStyle("-fx-text-fill: #2f3553;");

        Label subtitle = new Label("Showing the three highest-demand jobs. Detailed charts are in the analytics report.");
        subtitle.setFont(Font.font("Arial", 13));
        subtitle.setStyle("-fx-text-fill: #8b7fa0;");
        subtitle.setWrapText(true);

        VBox rows = new VBox(8);
        if (topJobs.isEmpty()) {
            rows.getChildren().add(UiTheme.createMutedText("No jobs have been posted yet."));
        } else {
            for (JobPosting job : topJobs) {
                rows.getChildren().add(createTopJobRow(job, applicationsByJob.getOrDefault(job.jobId(), 0L), maxApplications));
            }
        }

        VBox panel = new VBox(10, title, subtitle, rows);
        panel.setPadding(new Insets(18, 24, 18, 24));
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

    private static HBox createTopJobRow(JobPosting job, long applicationCount, long maxApplications) {
        Label name = new Label(job.title());
        name.setMinWidth(260);
        name.setPrefWidth(340);
        name.setWrapText(true);
        name.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        name.setStyle("-fx-text-fill: #4664a8;");

        Region bar = new Region();
        bar.setMinHeight(18);
        bar.setPrefWidth(Math.max(16, 520.0 * applicationCount / Math.max(1, maxApplications)));
        bar.setStyle("-fx-background-color: #b8d5ff; -fx-background-radius: 9;");

        Label count = new Label(applicationCount + " applications");
        count.setMinWidth(110);
        count.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        count.setStyle("-fx-text-fill: #5c6481;");

        HBox row = new HBox(12, name, bar, count);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static void showUserManagementDialog(UiAppContext context) {
        AdminUserManagementPanel userManagementPanel = AdminUserManagementPanel.create(context);

        Button closeButton = UiTheme.createOutlineButton("Close", 120, 40);
        HBox footer = new HBox(closeButton);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox shell = new VBox(16, userManagementPanel.container(), footer);
        shell.setPadding(new Insets(24));
        shell.setStyle("-fx-background-color: #fffaf3;");

        ScrollPane scrollPane = new ScrollPane(shell);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setStyle(
            "-fx-background-color: transparent;" +
                "-fx-background: transparent;" +
                "-fx-border-color: transparent;"
        );

        Stage dialog = new Stage();
        dialog.setTitle("User management");
        dialog.initModality(Modality.APPLICATION_MODAL);
        closeButton.setOnAction(event -> dialog.close());
        dialog.setScene(new Scene(scrollPane, 1180, 720));
        dialog.showAndWait();
    }

    private static void showJobManagementDialog(UiAppContext context) {
        AdminJobManagementPanel jobManagementPanel = AdminJobManagementPanel.create(context);

        Button closeButton = UiTheme.createOutlineButton("Close", 120, 40);
        HBox footer = new HBox(closeButton);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox shell = new VBox(16, jobManagementPanel.container(), footer);
        shell.setPadding(new Insets(24));
        shell.setStyle("-fx-background-color: #fffaf3;");

        ScrollPane scrollPane = new ScrollPane(shell);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setStyle(
            "-fx-background-color: transparent;" +
                "-fx-background: transparent;" +
                "-fx-border-color: transparent;"
        );

        Stage dialog = new Stage();
        dialog.setTitle("Job management");
        dialog.initModality(Modality.APPLICATION_MODAL);
        closeButton.setOnAction(event -> dialog.close());
        dialog.setScene(new Scene(scrollPane, 1180, 720));
        dialog.showAndWait();
    }

    private static void showApplicationManagementDialog(UiAppContext context) {
        AdminApplicationManagementPanel applicationManagementPanel = AdminApplicationManagementPanel.create(context);

        Button closeButton = UiTheme.createOutlineButton("Close", 120, 40);
        HBox footer = new HBox(closeButton);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox shell = new VBox(16, applicationManagementPanel.container(), footer);
        shell.setPadding(new Insets(24));
        shell.setStyle("-fx-background-color: #fffaf3;");

        ScrollPane scrollPane = new ScrollPane(shell);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setStyle(
            "-fx-background-color: transparent;" +
                "-fx-background: transparent;" +
                "-fx-border-color: transparent;"
        );

        Stage dialog = new Stage();
        dialog.setTitle("Application management");
        dialog.initModality(Modality.APPLICATION_MODAL);
        closeButton.setOnAction(event -> dialog.close());
        dialog.setScene(new Scene(scrollPane, 1180, 720));
        dialog.showAndWait();
    }

    private static Button createDashboardActionButton(String text, double width) {
        Button button = UiTheme.createOutlineButton(text, width, 36);
        button.setStyle(
            "-fx-background-color: #fff0f8;" +
                "-fx-text-fill: #2f3553;" +
                "-fx-background-radius: 20;" +
                "-fx-border-color: #f0a6e9;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 20;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;"
        );
        return button;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
