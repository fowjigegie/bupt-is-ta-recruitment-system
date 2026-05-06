package com.bupt.tarecruitment.ui;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
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
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * 管理员首页，负责系统概览和页面装配。
 */
public class AdminDashboardPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.ADMIN_DASHBOARD, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        long userCount = context.services().userRepository().findAll().size();
        long jobCount = context.services().jobRepository().findAll().size();
        long applicationCount = context.services().applicationRepository().findAll().size();

        VBox center = new VBox(18);
        center.setPadding(new Insets(32, 42, 30, 42));
        center.setStyle("-fx-text-fill: #2f3553;");

        HBox stats = new HBox(20,
            createAdminStatCard("Users", Long.toString(userCount), "User records"),
            createAdminStatCard("Jobs", Long.toString(jobCount), "Current postings"),
            createAdminStatCard("Applications", Long.toString(applicationCount), "Submitted records")
        );
        stats.setMaxWidth(Double.MAX_VALUE);
        stats.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));

        AdminWorkloadPanel workloadPanel = AdminWorkloadPanel.create(context);

        center.getChildren().addAll(
            UiTheme.createPageHeading("Admin dashboard"),
            UiTheme.createMutedText("Use this page to monitor accepted TA allocations and spot overload, schedule conflict, or schedule-data risks."),
            stats,
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
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        titleLabel.setStyle("-fx-text-fill: #5c6481;");

        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        valueLabel.setStyle("-fx-text-fill: #2f3553;");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setFont(Font.font("Arial", 14));
        subtitleLabel.setStyle("-fx-text-fill: #8b7fa0;");

        VBox card = new VBox(4, titleLabel, valueLabel, subtitleLabel);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16, 24, 16, 24));
        card.setMinHeight(116);
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

    public static void main(String[] args) {
        launch(args);
    }
}
