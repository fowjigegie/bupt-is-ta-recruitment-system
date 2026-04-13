package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobStatus;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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

import java.util.List;

/**
 * 封装 MO 首页的统计卡片、快捷入口和岗位预览区。
 */
final class ModuleOrganizerDashboardSections {
    private static final Color SIDEBAR_BLUE = Color.web("#4565a8");

    private ModuleOrganizerDashboardSections() {
    }

    static HBox createStats(long ownedJobsCount, long ownedApplications, long pendingReviews) {
        HBox stats = new HBox(16,
            createCompactMoStatCard("Owned postings", Long.toString(ownedJobsCount), "Jobs for this organiser."),
            createCompactMoStatCard("Applications", Long.toString(ownedApplications), "Across your postings."),
            createCompactMoStatCard("Pending reviews", Long.toString(pendingReviews), "Submitted, not yet reviewed.")
        );
        stats.setAlignment(Pos.CENTER);
        stats.setMaxWidth(Double.MAX_VALUE);
        return stats;
    }

    static HBox createQuickActions(
        NavigationManager nav,
        UiAppContext context,
        String userId,
        long pendingReviews,
        long unreadMessages
    ) {
        Button postButton = UiTheme.createPrimaryButton("Post a new vacancy", 300, 72);
        applyGradientButtonStyle(postButton);
        postButton.setOnAction(event -> {
            context.clearJobEdit();
            nav.goTo(PageId.POST_VACANCIES);
        });

        Button pendingButton = UiTheme.createPrimaryButton("Pending applications", 300, 72);
        applyGradientButtonStyle(pendingButton);
        pendingButton.setOnAction(event -> {
            context.selectJob(null);
            context.selectApplication(null);
            nav.goTo(PageId.APPLICATION_REVIEW);
        });

        Button chatButton = UiTheme.createPrimaryButton("Chat", 300, 72);
        applyGradientButtonStyle(chatButton);
        chatButton.setOnAction(event -> {
            context.services().messageService()
                .findMostRecentConversationForUser(userId)
                .ifPresentOrElse(
                    conversation -> context.openChatContext(conversation.jobId(), conversation.peerUserId()),
                    context::clearSelections
                );
            nav.goTo(PageId.MESSAGES);
        });

        HBox quickActions = new HBox(22,
            wrapPrimaryButton(postButton),
            wrapPrimaryButtonWithBadge(pendingButton, String.valueOf(Math.min(pendingReviews, 99))),
            wrapPrimaryButtonWithBadge(chatButton, Long.toString(Math.min(unreadMessages, 99)))
        );
        quickActions.setAlignment(Pos.CENTER);
        quickActions.setMaxWidth(Double.MAX_VALUE);
        quickActions.setPadding(new Insets(4, 0, 8, 0));
        return quickActions;
    }

    static VBox createJobManagementSection(NavigationManager nav, UiAppContext context, List<JobPosting> ownedJobs) {
        Label sectionTitle = new Label("Jobs I have posted");
        sectionTitle.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        sectionTitle.setTextFill(SIDEBAR_BLUE);

        StackPane titleChip = new StackPane(sectionTitle);
        titleChip.setPadding(new Insets(8, 16, 8, 16));
        titleChip.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(8), Insets.EMPTY)));
        titleChip.setBorder(new Border(new BorderStroke(
            Color.web("#f0a6c9"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(8),
            new BorderWidths(2)
        )));

        HBox titleBar = new HBox(titleChip);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(12, 16, 10, 16));
        titleBar.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

        VBox listContainer = new VBox(0);
        listContainer.setFillWidth(true);
        listContainer.setMaxWidth(Double.MAX_VALUE);
        listContainer.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

        if (ownedJobs.isEmpty()) {
            Label empty = new Label("You have not posted any jobs yet. Use \"Post a new vacancy\" above.");
            empty.setFont(Font.font("Arial", 14));
            empty.setTextFill(SIDEBAR_BLUE);
            empty.setWrapText(true);
            empty.setPadding(new Insets(16, 16, 20, 16));
            listContainer.getChildren().add(empty);
        } else {
            for (JobPosting job : ownedJobs) {
                listContainer.getChildren().add(createPostedJobRow(nav, context, job));
            }
        }

        ScrollPane scroll = new ScrollPane(listContainer);
        scroll.setFitToWidth(true);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        int visibleRows = ownedJobs.isEmpty() ? 1 : Math.min(ownedJobs.size(), 4);
        double rowHeight = 72;
        double viewportHeight = visibleRows * rowHeight + 18;
        scroll.setMinViewportHeight(viewportHeight);
        scroll.setPrefViewportHeight(viewportHeight);
        scroll.setMaxHeight(viewportHeight + 6);
        scroll.setStyle("-fx-background: #ffffff; -fx-background-color: #ffffff;");
        scroll.setPadding(Insets.EMPTY);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox sectionContent = new VBox(0, titleBar, scroll);
        sectionContent.setFillWidth(true);
        sectionContent.setMaxWidth(Double.MAX_VALUE);
        sectionContent.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox sectionWrapper = new VBox(sectionContent);
        sectionWrapper.setFillWidth(true);
        sectionWrapper.setMaxWidth(Double.MAX_VALUE);
        sectionWrapper.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(8), Insets.EMPTY)));
        sectionWrapper.setBorder(new Border(new BorderStroke(
            Color.web("#f0a6c9"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(8),
            new BorderWidths(2)
        )));
        return sectionWrapper;
    }

    private static void applyGradientButtonStyle(Button button) {
        button.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        button.setStyle(
            "-fx-background-color: linear-gradient(to right, #ffd699, #ffb3d9);" +
                "-fx-text-fill: #4565a8;" +
                "-fx-background-radius: 24;" +
                "-fx-cursor: hand;"
        );
    }

    private static VBox createCompactMoStatCard(String title, String value, String subtitle) {
        VBox card = new VBox(6);
        card.setPrefSize(320, 74);
        card.setMinSize(260, 70);
        card.setMaxHeight(78);
        card.setPadding(new Insets(10, 16, 10, 16));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(18), Insets.EMPTY)));
        card.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(18),
            new BorderWidths(1.5)
        )));

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        titleLabel.setTextFill(SIDEBAR_BLUE);

        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        valueLabel.setTextFill(SIDEBAR_BLUE);

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setFont(Font.font("Arial", 12));
        subtitleLabel.setTextFill(SIDEBAR_BLUE);
        subtitleLabel.setWrapText(true);

        HBox valueRow = new HBox(10, valueLabel, subtitleLabel);
        valueRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(subtitleLabel, Priority.ALWAYS);

        card.getChildren().addAll(titleLabel, valueRow);
        return card;
    }

    private static StackPane wrapPrimaryButton(Button button) {
        StackPane stack = new StackPane(button);
        stack.setMinWidth(300);
        stack.setMaxWidth(340);
        return stack;
    }

    private static StackPane wrapPrimaryButtonWithBadge(Button button, String badgeText) {
        if ("0".equals(badgeText)) {
            return wrapPrimaryButton(button);
        }

        Label badge = new Label(badgeText);
        badge.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        badge.setTextFill(Color.WHITE);
        badge.setStyle(
            "-fx-background-color: #e53935; -fx-background-radius: 14; -fx-padding: 3 9 3 9; -fx-min-width: 22px; -fx-alignment: center;"
        );
        StackPane stack = new StackPane(button, badge);
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(6, 10, 0, 0));
        stack.setMinWidth(300);
        stack.setMaxWidth(340);
        return stack;
    }

    private static HBox createPostedJobRow(NavigationManager nav, UiAppContext context, JobPosting job) {
        long applicationCount = context.services().applicationRepository().findAll().stream()
            .filter(application -> application.jobId().equals(job.jobId()))
            .count();

        String statusLabel = job.status() == JobStatus.OPEN ? "Open" : "Closed";

        Label headline = new Label(job.moduleOrActivity() + " - " + job.title());
        headline.setFont(Font.font("Arial", FontWeight.BOLD, 17));
        headline.setTextFill(SIDEBAR_BLUE);
        headline.setWrapText(true);

        Label meta = new Label("Status: " + statusLabel + " | Applications: " + applicationCount);
        meta.setFont(Font.font("Arial", 13));
        meta.setTextFill(SIDEBAR_BLUE);

        VBox textColumn = new VBox(6, headline, meta);
        textColumn.setAlignment(Pos.CENTER_LEFT);
        textColumn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textColumn, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button manageButton = new Button("Managing This Job");
        manageButton.setPrefSize(200, 44);
        manageButton.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        manageButton.setStyle(
            "-fx-background-color: #ffd6e8;" +
                "-fx-text-fill: #4565a8;" +
                "-fx-background-radius: 22;" +
                "-fx-border-color: #c5d4e8;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 22;" +
                "-fx-cursor: hand;"
        );
        manageButton.setOnAction(event -> {
            context.selectJob(job.jobId());
            nav.goTo(PageId.JOB_MANAGEMENT);
        });

        HBox row = new HBox(20, textColumn, spacer, manageButton);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 16, 14, 16));
        row.setMaxWidth(Double.MAX_VALUE);
        row.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        row.setBorder(new Border(new BorderStroke(
            Color.web("#f5b4d4"),
            BorderStrokeStyle.SOLID,
            CornerRadii.EMPTY,
            new BorderWidths(0, 0, 1, 0)
        )));

        return row;
    }
}
