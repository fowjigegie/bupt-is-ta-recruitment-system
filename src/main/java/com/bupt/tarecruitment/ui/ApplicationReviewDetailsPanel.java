package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.applicant.ApplicantCvReview;
import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.application.ApplicationStatus;
import com.bupt.tarecruitment.application.ApplicationStatusPresenter;
import com.bupt.tarecruitment.application.JobApplication;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 封装申请审核页右侧的简历详情展示与状态更新操作。
 */
final class ApplicationReviewDetailsPanel {
    private ApplicationReviewDetailsPanel() {
    }

    static void renderSelectedDetail(
        UiAppContext context,
        JobApplication application,
        Label detailTitle,
        VBox detailContent,
        Label actionStatus,
        NavigationManager nav,
        Runnable refreshList
    ) {
        try {
            ApplicantCvReview review = context.services().cvReviewService().loadReviewByApplicationId(application.applicationId());
            detailTitle.setText("CV | " + review.cv().cvId() + " | " + review.profile().fullName());
            detailTitle.setTextFill(Color.web("#4664a8"));
            detailContent.getChildren().setAll(build(context, review, actionStatus, nav, refreshList));
            actionStatus.setText("");
        } catch (Exception exception) {
            detailTitle.setText("Applicant details");
            detailContent.getChildren().setAll(UiTheme.createWhiteCard("Failed to load CV", exception.getMessage()));
            actionStatus.setText(exception.getMessage());
            actionStatus.setTextFill(Color.web("#b00020"));
        }
    }

    private static VBox build(
        UiAppContext context,
        ApplicantCvReview review,
        Label actionStatus,
        NavigationManager nav,
        Runnable refreshList
    ) {
        ApplicantProfile profile = review.profile();
        Map<String, String> parsed = parseCvContent(review.cvContent());

        TextField nameField = createReadonlyRoundedField("Full Name", valueOrFallback(parsed.get("Name"), profile.fullName()), 280);
        TextField gradeField = createReadonlyRoundedField(
            "Grade",
            valueOrFallback(parsed.get("Grade"), "%d-year %s".formatted(profile.yearOfStudy(), profile.educationLevel().toLowerCase())),
            280
        );
        TextField programmeField = createReadonlyRoundedField("Programme", valueOrFallback(parsed.get("Programme"), profile.programme()), 280);
        TextField studentIdField = createReadonlyRoundedField("Student ID", valueOrFallback(parsed.get("Student ID"), profile.studentId()), 280);
        TextField availabilityField = createReadonlyRoundedField(
            "Availability",
            valueOrFallback(parsed.get("Availability"), String.join("; ", profile.availabilitySlots())),
            572
        );
        TextField titleField = createReadonlyRoundedField("CV Title", review.cv().title(), 572);

        TextArea cvTextArea = createReadonlyLargeArea(valueOrFallback(parsed.get("CV Text"), review.cvContent()), 170);
        TextArea skillsArea = createReadonlyLargeArea(
            valueOrFallback(parsed.get("Skills"), String.join(System.lineSeparator(), profile.skills()).replace(", ", System.lineSeparator())),
            120
        );
        TextArea positionsArea = createReadonlyLargeArea(
            valueOrFallback(
                parsed.get("Desired Positions"),
                String.join(System.lineSeparator(), profile.desiredPositions()).replace(", ", System.lineSeparator())
            ),
            120
        );

        Label statusTag = new Label("Current Status: " + ApplicationStatusPresenter.toDisplayText(review.application().status()));
        statusTag.setStyle(
            "-fx-background-color: " + statusColor(review.application().status()) + ";" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 12;" +
                "-fx-padding: 6 12 6 12;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;"
        );

        Button chatButton = UiTheme.createOutlineButton("Chat", 160, 44);
        chatButton.setOnAction(event -> {
            context.openChatContext(review.application().jobId(), review.application().applicantUserId());
            nav.goTo(PageId.MESSAGES);
        });

        ApplicationStatus currentStatus = review.application().status();
        boolean actionable = currentStatus != ApplicationStatus.WITHDRAWN;

        Button shortlistedButton = UiTheme.createSoftButton("Shortlisted", 160, 44);
        shortlistedButton.setDisable(!actionable || currentStatus == ApplicationStatus.SHORTLISTED);
        shortlistedButton.setOnAction(event -> updateReviewStatus(
            context,
            review.application().applicationId(),
            ApplicationStatus.SHORTLISTED,
            "Shortlisted by MO in Application Review page.",
            actionStatus,
            refreshList
        ));

        Button acceptedButton = UiTheme.createPrimaryButton("Accepted", 160, 44);
        acceptedButton.setDisable(!actionable || currentStatus == ApplicationStatus.ACCEPTED);
        acceptedButton.setOnAction(event -> updateReviewStatus(
            context,
            review.application().applicationId(),
            ApplicationStatus.ACCEPTED,
            "Accepted by MO in Application Review page.",
            actionStatus,
            refreshList
        ));

        Button rejectedButton = UiTheme.createSoftButton("Rejected", 160, 44);
        rejectedButton.setDisable(!actionable || currentStatus == ApplicationStatus.REJECTED);
        rejectedButton.setOnAction(event -> updateReviewStatus(
            context,
            review.application().applicationId(),
            ApplicationStatus.REJECTED,
            "Rejected by MO in Application Review page.",
            actionStatus,
            refreshList
        ));

        HBox actionRow = new HBox(12, chatButton, shortlistedButton, acceptedButton, rejectedButton);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        HBox row1 = new HBox(12, nameField, gradeField);
        HBox row2 = new HBox(12, programmeField, studentIdField);

        VBox form = new VBox(12,
            row1,
            row2,
            availabilityField,
            titleField,
            createLabeledBlock("CV Text", cvTextArea),
            createLabeledBlock("Skills", skillsArea),
            createLabeledBlock("Desired Positions", positionsArea),
            statusTag,
            actionRow
        );
        form.setPadding(new Insets(4, 6, 12, 6));
        return form;
    }

    private static void updateReviewStatus(
        UiAppContext context,
        String applicationId,
        ApplicationStatus nextStatus,
        String reviewerNote,
        Label actionStatus,
        Runnable refreshList
    ) {
        try {
            JobApplication current = context.services().applicationRepository()
                .findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));

            ApplicationStatus fromStatus = current.status();
            if (fromStatus == nextStatus) {
                return;
            }
            if (fromStatus == ApplicationStatus.WITHDRAWN) {
                throw new IllegalArgumentException("Withdrawn applications cannot be reviewed.");
            }

            javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm status change");
            confirm.setHeaderText(null);
            confirm.setContentText(
                "Change application status from "
                    + ApplicationStatusPresenter.toDisplayText(fromStatus)
                    + " to "
                    + ApplicationStatusPresenter.toDisplayText(nextStatus)
                    + "..."
            );

            var result = confirm.showAndWait();
            if (result.isEmpty() || result.get() != javafx.scene.control.ButtonType.OK) {
                return;
            }

            context.services().applicationDecisionService().updateStatus(
                context.session().userId(),
                applicationId,
                nextStatus,
                reviewerNote
            );
            actionStatus.setTextFill(Color.web("#2e7d32"));
            actionStatus.setText("Application updated to " + ApplicationStatusPresenter.toDisplayText(nextStatus) + ".");
            refreshList.run();
        } catch (IllegalArgumentException exception) {
            actionStatus.setTextFill(Color.web("#b00020"));
            actionStatus.setText(exception.getMessage());
        }
    }

    private static String valueOrFallback(String first, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return fallback == null ? "" : fallback;
    }

    private static TextField createReadonlyRoundedField(String prompt, String value, double width) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setText(value == null ? "" : value);
        field.setEditable(false);
        field.setPrefWidth(width);
        field.setPrefHeight(44);
        field.setFont(Font.font("Arial", 15));
        field.setStyle(
            "-fx-background-color: #eba8df;" +
                "-fx-background-radius: 20;" +
                "-fx-border-radius: 20;" +
                "-fx-border-color: transparent;" +
                "-fx-prompt-text-fill: white;" +
                "-fx-text-fill: white;" +
                "-fx-padding: 0 16 0 16;"
        );
        return field;
    }

    private static VBox createLabeledBlock(String title, TextArea area) {
        Label label = new Label(title);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 17));
        label.setTextFill(Color.web("#4664a8"));
        return new VBox(6, label, area);
    }

    private static TextArea createReadonlyLargeArea(String value, double prefHeight) {
        TextArea area = new TextArea(value == null ? "" : value);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefHeight(prefHeight);
        area.setStyle(
            "-fx-control-inner-background: white;" +
                "-fx-background-color: white;" +
                "-fx-background-radius: 0;" +
                "-fx-border-color: #f3b2df;" +
                "-fx-border-width: 3;" +
                "-fx-padding: 12;"
        );
        return area;
    }

    static String statusColor(ApplicationStatus status) {
        return switch (status) {
            case SUBMITTED -> "#5b84d6";
            case SHORTLISTED -> "#f59e0b";
            case ACCEPTED -> "#2e7d32";
            case REJECTED -> "#e53935";
            case WITHDRAWN -> "#9ca3af";
        };
    }

    private static Map<String, String> parseCvContent(String content) {
        Map<String, String> result = new LinkedHashMap<>();
        if (content == null || content.isBlank()) {
            return result;
        }

        StringBuilder cvBody = new StringBuilder();
        for (String line : content.split("\\R")) {
            int separatorIndex = line.indexOf(": ");
            if (separatorIndex > 0) {
                String key = line.substring(0, separatorIndex).trim();
                String value = line.substring(separatorIndex + 2).trim();
                if (isKnownCvKey(key)) {
                    result.put(key, value);
                    continue;
                }
            }
            cvBody.append(line).append(System.lineSeparator());
        }

        String bodyText = cvBody.toString().trim();
        if (!bodyText.isBlank()) {
            result.putIfAbsent("CV Text", bodyText);
        }
        return result;
    }

    private static boolean isKnownCvKey(String key) {
        return "Name".equals(key)
            || "Grade".equals(key)
            || "Programme".equals(key)
            || "Student ID".equals(key)
            || "Availability".equals(key)
            || "Skills".equals(key)
            || "Desired Positions".equals(key);
    }
}
