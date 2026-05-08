package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.applicant.ApplicantCv;
import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.auth.UserAccount;
import com.bupt.tarecruitment.auth.UserRole;
import com.bupt.tarecruitment.communication.InquiryMessage;
import com.bupt.tarecruitment.job.JobPosting;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Admin data-health entry point and grouped integrity report for txt-backed storage.
 */
final class AdminDataIntegrityPanel {
    private AdminDataIntegrityPanel() {
    }

    static VBox create(UiAppContext context) {
        IntegrityReport report = analyze(context);

        Label title = createTitle("Data integrity");
        Label summary = createValue(report.criticalCount() + " critical issues / " + report.warningCount() + " warnings");
        Label subtitle = createMuted("Checks txt-backed users, profiles, jobs, CVs, applications, and messages.");

        HBox healthBar = new HBox(0);
        healthBar.setMaxWidth(Double.MAX_VALUE);
        healthBar.getChildren().addAll(
            createHealthSegment("Critical", report.criticalCount(), "#fff2f2", "#b00020"),
            createHealthSegment("Warnings", report.warningCount(), "#fff6e5", "#9a6500"),
            createHealthSegment("Clear", Math.max(1, report.clearCategoryCount()), "#f2fbf3", "#2e7d32")
        );

        VBox textBox = new VBox(6, title, summary, subtitle, healthBar);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Button viewReportButton = createToolButton("View report", 150);
        viewReportButton.setOnAction(event -> showReport(context));

        HBox content = new HBox(18, textBox, viewReportButton);
        content.setAlignment(Pos.CENTER_LEFT);

        VBox card = createToolCard(content);
        return card;
    }

    static IntegrityReport analyze(UiAppContext context) {
        List<IntegrityIssue> issues = new ArrayList<>();

        List<UserAccount> users = context.services().userRepository().findAll();
        List<ApplicantProfile> profiles = context.services().profileRepository().findAll();
        List<ApplicantCv> cvs = context.services().cvRepository().findAll();
        List<JobPosting> jobs = context.services().jobRepository().findAll();
        List<JobApplication> applications = context.services().applicationRepository().findAll();
        List<InquiryMessage> messages = context.services().messageRepository().findAll();

        for (UserAccount user : users) {
            if (user.role() == UserRole.APPLICANT
                && context.services().profileRepository().findByUserId(user.userId()).isEmpty()) {
                issues.add(warning("Users / Profiles", user.displayName(), user.userId() + " has no applicant profile."));
            }
        }

        for (ApplicantProfile profile : profiles) {
            if (profile.availabilitySlots().isEmpty()) {
                issues.add(warning("Users / Profiles", profile.fullName(), profile.userId() + " has no availability slots."));
            }
        }

        for (JobPosting job : jobs) {
            if (job.scheduleSlots().isEmpty()) {
                issues.add(warning("Jobs", job.title(), job.jobId() + " has no schedule slots."));
            }
            if (job.requiredSkills().isEmpty()) {
                issues.add(warning("Jobs", job.title(), job.jobId() + " has no required skills."));
            }
        }

        for (JobApplication application : applications) {
            Optional<ApplicantCv> cv = context.services().cvRepository().findByCvId(application.cvId());
            if (cv.isEmpty()) {
                issues.add(critical(
                    "Applications / CVs",
                    application.applicationId(),
                    "References missing CV " + application.cvId() + "."
                ));
            } else if (!cv.get().ownerUserId().equals(application.applicantUserId())) {
                issues.add(critical(
                    "Applications / CVs",
                    application.applicationId(),
                    "References CV " + application.cvId() + " owned by " + cv.get().ownerUserId() + "."
                ));
            }
        }

        for (InquiryMessage message : messages) {
            if (context.services().jobRepository().findByJobId(message.jobId()).isEmpty()) {
                issues.add(critical(
                    "Messages",
                    message.messageId(),
                    "References missing job " + message.jobId() + "."
                ));
            }
        }

        return new IntegrityReport(issues);
    }

    private static void showReport(UiAppContext context) {
        IntegrityReport report = analyze(context);

        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #fffaf3;");

        Label title = UiTheme.createPageHeading("Data integrity");
        title.setStyle("-fx-text-fill: #4664a8;");
        Label subtitle = createMuted(
            report.criticalCount() + " critical issues / " + report.warningCount() + " warnings"
        );
        content.getChildren().addAll(title, subtitle);

        for (String category : List.of("Users / Profiles", "Jobs", "Applications / CVs", "Messages")) {
            List<IntegrityIssue> categoryIssues = report.issues().stream()
                .filter(issue -> issue.category().equals(category))
                .sorted(Comparator.comparing(IntegrityIssue::severity).thenComparing(IntegrityIssue::title))
                .toList();
            content.getChildren().add(createCategoryCard(category, categoryIssues));
        }

        Button closeButton = UiTheme.createOutlineButton("Close", 120, 40);
        HBox footer = new HBox(closeButton);
        footer.setAlignment(Pos.CENTER_RIGHT);
        content.getChildren().add(footer);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        Stage dialog = new Stage();
        dialog.setTitle("Data integrity report");
        dialog.initModality(Modality.APPLICATION_MODAL);
        closeButton.setOnAction(event -> dialog.close());
        dialog.setScene(new Scene(scrollPane, 900, 680));
        dialog.showAndWait();
    }

    private static VBox createCategoryCard(String category, List<IntegrityIssue> issues) {
        Color background = issues.isEmpty()
            ? Color.web("#f2fbf3")
            : issues.stream().anyMatch(issue -> issue.severity() == IntegritySeverity.CRITICAL)
                ? Color.web("#fff2f2")
                : Color.web("#fff6e5");
        Color border = issues.isEmpty()
            ? Color.web("#9fd6a6")
            : issues.stream().anyMatch(issue -> issue.severity() == IntegritySeverity.CRITICAL)
                ? Color.web("#f2a6a6")
                : Color.web("#ffd58a");

        Label heading = createTitle(category);
        VBox card = new VBox(12, heading);
        card.setPadding(new Insets(18));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setBackground(new Background(new BackgroundFill(background, new CornerRadii(22), Insets.EMPTY)));
        card.setBorder(new Border(new BorderStroke(border, BorderStrokeStyle.SOLID, new CornerRadii(22), new BorderWidths(1.6))));

        if (issues.isEmpty()) {
            card.getChildren().add(createMuted("No issues detected."));
        } else {
            for (IntegrityIssue issue : issues) {
                card.getChildren().add(createIssueRow(issue));
            }
        }
        return card;
    }

    private static HBox createIssueRow(IntegrityIssue issue) {
        Label chip = createChip(
            issue.severity() == IntegritySeverity.CRITICAL ? "Critical" : "Warning",
            issue.severity() == IntegritySeverity.CRITICAL ? "#fff2f2" : "#fff6e5",
            issue.severity() == IntegritySeverity.CRITICAL ? "#b00020" : "#9a6500",
            issue.severity() == IntegritySeverity.CRITICAL ? "#f2a6a6" : "#ffd58a"
        );
        chip.setMinWidth(92);

        VBox text = new VBox(3);
        Label title = createSmallTitle(issue.title());
        Label details = createMuted(issue.details());
        text.getChildren().addAll(title, details);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox row = new HBox(12, chip, text);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
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

    private static HBox createHealthSegment(String label, long value, String background, String color) {
        Label text = new Label(label + ": " + value);
        text.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        text.setStyle(
            "-fx-background-color: " + background + ";" +
                "-fx-text-fill: " + color + ";" +
                "-fx-padding: 5 8 5 8;" +
                "-fx-border-color: " + background + ";" +
                "-fx-border-width: 1;"
        );
        HBox box = new HBox(text);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
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

    private static IntegrityIssue critical(String category, String title, String details) {
        return new IntegrityIssue(category, IntegritySeverity.CRITICAL, title, details);
    }

    private static IntegrityIssue warning(String category, String title, String details) {
        return new IntegrityIssue(category, IntegritySeverity.WARNING, title, details);
    }

    private static Label createTitle(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        label.setStyle("-fx-text-fill: #2f3553;");
        return label;
    }

    private static Label createSmallTitle(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: #2f3553;");
        return label;
    }

    private static Label createValue(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: #4664a8;");
        return label;
    }

    private static Label createMuted(String text) {
        Label label = UiTheme.createMutedText(text);
        label.setStyle("-fx-text-fill: #5c6481;");
        return label;
    }

    private static Label createChip(String text, String background, String textColor, String borderColor) {
        Label chip = new Label(text);
        chip.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        chip.setPadding(new Insets(7, 12, 7, 12));
        chip.setStyle(
            "-fx-background-color: " + background + ";" +
                "-fx-text-fill: " + textColor + ";" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-radius: 16;" +
                "-fx-border-width: 1.2;"
        );
        return chip;
    }

    record IntegrityReport(List<IntegrityIssue> issues) {
        long criticalCount() {
            return issues.stream().filter(issue -> issue.severity() == IntegritySeverity.CRITICAL).count();
        }

        long warningCount() {
            return issues.stream().filter(issue -> issue.severity() == IntegritySeverity.WARNING).count();
        }

        long clearCategoryCount() {
            Map<String, Long> counts = issues.stream()
                .collect(java.util.stream.Collectors.groupingBy(IntegrityIssue::category, java.util.stream.Collectors.counting()));
            return List.of("Users / Profiles", "Jobs", "Applications / CVs", "Messages").stream()
                .filter(category -> !counts.containsKey(category))
                .count();
        }
    }

    private record IntegrityIssue(
        String category,
        IntegritySeverity severity,
        String title,
        String details
    ) {
    }

    private enum IntegritySeverity {
        CRITICAL,
        WARNING
    }
}
