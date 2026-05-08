package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.applicant.ApplicantCv;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.auth.UserAccount;
import com.bupt.tarecruitment.auth.UserRole;
import com.bupt.tarecruitment.job.JobPosting;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
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
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Admin global search entry point for users, jobs, applications, and CV metadata.
 */
final class AdminGlobalSearchPanel {
    private AdminGlobalSearchPanel() {
    }

    static VBox create(UiAppContext context) {
        Label title = createTitle("Global search");
        Label subtitle = createMuted("Search users, jobs, applications, and CV titles across txt-backed data.");

        TextField searchField = new TextField();
        searchField.setPromptText("Search users, jobs, applications, CVs...");
        searchField.setPrefHeight(38);
        searchField.setStyle(
            "-fx-background-color: white;" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: #f0a6e9;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 18;" +
                "-fx-font-size: 14px;"
        );
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchButton = createToolButton("Search", 120);
        searchButton.setOnAction(event -> showResults(context, searchField.getText()));
        searchField.setOnAction(event -> showResults(context, searchField.getText()));

        HBox actionRow = new HBox(10, searchField, searchButton);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setMaxWidth(Double.MAX_VALUE);

        VBox content = new VBox(8, title, subtitle, actionRow);
        return createToolCard(content);
    }

    private static void showResults(UiAppContext context, String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        SearchResults results = search(context, query);

        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #fffaf3;");

        Label title = UiTheme.createPageHeading("Global search");
        title.setStyle("-fx-text-fill: #4664a8;");
        Label subtitle = createMuted(query.isBlank()
            ? "Showing all records because the search box is empty."
            : "Results for: " + query);
        content.getChildren().addAll(title, subtitle);

        content.getChildren().add(createResultGroup("Users", results.users().stream()
            .map(user -> createResultRow(
                context.displayNameForUser(user.userId()),
                user.userId() + " | " + user.role().name()
            ))
            .toList()));
        content.getChildren().add(createResultGroup("Jobs", results.jobs().stream()
            .map(job -> createResultRow(
                job.title(),
                job.jobId() + " | " + job.moduleOrActivity() + " | " + job.status().name()
            ))
            .toList()));
        content.getChildren().add(createResultGroup("Applications", results.applications().stream()
            .map(application -> createResultRow(
                application.applicationId(),
                context.formatUserLabel(application.applicantUserId()) + " -> " + resolveJobTitle(context, application.jobId())
            ))
            .toList()));
        content.getChildren().add(createResultGroup("CVs", results.cvs().stream()
            .map(cv -> createResultRow(
                cv.title(),
                cv.cvId() + " | owner " + cv.ownerUserId()
            ))
            .toList()));

        Button closeButton = UiTheme.createOutlineButton("Close", 120, 40);
        HBox footer = new HBox(closeButton);
        footer.setAlignment(Pos.CENTER_RIGHT);
        content.getChildren().add(footer);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        Stage dialog = new Stage();
        dialog.setTitle("Global search");
        dialog.initModality(Modality.APPLICATION_MODAL);
        closeButton.setOnAction(event -> dialog.close());
        dialog.setScene(new Scene(scrollPane, 900, 680));
        dialog.showAndWait();
    }

    private static SearchResults search(UiAppContext context, String query) {
        String normalized = query.toLowerCase(Locale.ROOT);
        boolean empty = normalized.isBlank();

        List<UserAccount> users = context.services().userRepository().findAll().stream()
            .filter(user -> user.role() != UserRole.ADMIN)
            .filter(user -> empty
                || contains(user.userId(), normalized)
                || contains(user.displayName(), normalized)
                || contains(context.displayNameForUser(user.userId()), normalized)
                || contains(user.role().name(), normalized))
            .sorted(Comparator.comparing(UserAccount::role).thenComparing(UserAccount::userId))
            .toList();

        List<JobPosting> jobs = context.services().jobRepository().findAll().stream()
            .filter(job -> empty
                || contains(job.jobId(), normalized)
                || contains(job.title(), normalized)
                || contains(job.moduleOrActivity(), normalized)
                || contains(job.activityType(), normalized))
            .sorted(Comparator.comparing(JobPosting::jobId))
            .toList();

        List<JobApplication> applications = context.services().applicationRepository().findAll().stream()
            .filter(application -> empty
                || contains(application.applicationId(), normalized)
                || contains(application.applicantUserId(), normalized)
                || contains(application.jobId(), normalized)
                || contains(application.cvId(), normalized)
                || contains(application.status().name(), normalized)
                || contains(resolveJobTitle(context, application.jobId()), normalized)
                || contains(context.displayNameForUser(application.applicantUserId()), normalized))
            .sorted(Comparator.comparing(JobApplication::submittedAt).reversed())
            .toList();

        List<ApplicantCv> cvs = context.services().cvRepository().findAll().stream()
            .filter(cv -> empty
                || contains(cv.cvId(), normalized)
                || contains(cv.title(), normalized)
                || contains(cv.ownerUserId(), normalized)
                || contains(cv.fileName(), normalized)
                || contains(context.displayNameForUser(cv.ownerUserId()), normalized))
            .sorted(Comparator.comparing(ApplicantCv::cvId))
            .toList();

        return new SearchResults(users, jobs, applications, cvs);
    }

    private static VBox createResultGroup(String title, List<? extends Node> rows) {
        VBox group = new VBox(10);
        group.setPadding(new Insets(18));
        group.setMaxWidth(Double.MAX_VALUE);
        group.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(22), Insets.EMPTY)));
        group.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(22),
            new BorderWidths(1.5)
        )));

        Label heading = createTitle(title + " (" + rows.size() + ")");
        group.getChildren().add(heading);
        if (rows.isEmpty()) {
            group.getChildren().add(createMuted("No matching records."));
        } else {
            group.getChildren().addAll(rows);
        }
        return group;
    }

    private static HBox createResultRow(String title, String subtitle) {
        VBox text = new VBox(3);
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-text-fill: #4664a8;");
        Label subtitleLabel = createMuted(subtitle);
        text.getChildren().addAll(titleLabel, subtitleLabel);

        HBox row = new HBox(text);
        row.setPadding(new Insets(12));
        row.setMaxWidth(Double.MAX_VALUE);
        row.setBackground(new Background(new BackgroundFill(Color.web("#fffafd"), new CornerRadii(16), Insets.EMPTY)));
        row.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(16),
            new BorderWidths(1.1)
        )));
        return row;
    }

    private static String resolveJobTitle(UiAppContext context, String jobId) {
        Optional<JobPosting> job = context.services().jobRepository().findByJobId(jobId);
        return job.map(JobPosting::title).orElse("Unknown job");
    }

    private static boolean contains(String text, String query) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(query);
    }

    private static VBox createToolCard(Node child) {
        VBox card = new VBox(child);
        card.setPadding(new Insets(20, 24, 20, 24));
        card.setMinHeight(150);
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

    private static Button createToolButton(String text, double width) {
        Button button = UiTheme.createOutlineButton(text, width, 38);
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

    private static Label createTitle(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        label.setStyle("-fx-text-fill: #2f3553;");
        return label;
    }

    private static Label createMuted(String text) {
        Label label = UiTheme.createMutedText(text);
        label.setStyle("-fx-text-fill: #5c6481;");
        return label;
    }

    private record SearchResults(
        List<UserAccount> users,
        List<JobPosting> jobs,
        List<JobApplication> applications,
        List<ApplicantCv> cvs
    ) {
    }
}
