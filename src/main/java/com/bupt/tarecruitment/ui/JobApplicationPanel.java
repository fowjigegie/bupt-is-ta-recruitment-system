package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.applicant.ApplicantCv;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobStatus;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;

import java.util.Comparator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 封装岗位详情页中的简历选择、投递和撤回操作区。
 */
final class JobApplicationPanel {
    private final ComboBox<ApplicantCv> cvBox;
    private final Label statusLabel;
    private final VBox container;

    private JobApplicationPanel(ComboBox<ApplicantCv> cvBox, Label statusLabel, VBox container) {
        this.cvBox = cvBox;
        this.statusLabel = statusLabel;
        this.container = container;
    }

    static JobApplicationPanel create(
        NavigationManager nav,
        UiAppContext context,
        JobPosting job,
        JobApplication currentApplication,
        String applyBlockedReason,
        BiConsumer<ApplicantCv, Label> applyAction,
        Consumer<Label> withdrawAction,
        Runnable chatAction
    ) {
        Label applyTitle = new Label("Apply with one of your CVs");
        applyTitle.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        applyTitle.setTextFill(Color.web("#4664a8"));

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setTextFill(Color.web("#b00020"));

        ComboBox<ApplicantCv> cvBox = createCvBox();
        loadCvs(context, cvBox, statusLabel);

        var applyButton = UiTheme.createPrimaryButton("Apply now", 190, 56);
        boolean blockedByAvailability = applyBlockedReason != null && !applyBlockedReason.isBlank();
        applyButton.setDisable(job.status() != JobStatus.OPEN || currentApplication != null || blockedByAvailability);
        applyButton.setOnAction(event -> applyAction.accept(cvBox.getValue(), statusLabel));

        var withdrawButton = UiTheme.createOutlineButton("Withdraw application", 220, 56);
        withdrawButton.setDisable(currentApplication == null);
        withdrawButton.setOnAction(event -> {
            if (currentApplication != null) {
                withdrawAction.accept(statusLabel);
            }
        });

        var chatButton = UiTheme.createSoftButton("Chat with MO", 170, 56);
        chatButton.setOnAction(event -> chatAction.run());

        HBox actions = new HBox(16, cvBox, applyButton, withdrawButton, chatButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        if (currentApplication != null) {
            statusLabel.setTextFill(Color.web("#4664a8"));
            statusLabel.setText("Current application: " + currentApplication.applicationId()
                + " (" + currentApplication.status().name() + ")");
        } else if (blockedByAvailability) {
            statusLabel.setTextFill(Color.web("#b00020"));
            statusLabel.setText(applyBlockedReason);
        }

        return new JobApplicationPanel(
            cvBox,
            statusLabel,
            new VBox(14, applyTitle, actions, statusLabel)
        );
    }

    ApplicantCv selectedCv() {
        return cvBox.getValue();
    }

    Label statusLabel() {
        return statusLabel;
    }

    VBox container() {
        return container;
    }

    private static ComboBox<ApplicantCv> createCvBox() {
        ComboBox<ApplicantCv> cvBox = new ComboBox<>();
        cvBox.setPrefWidth(380);
        cvBox.setPrefHeight(42);
        cvBox.setStyle(
            "-fx-background-color: white;" +
                "-fx-background-radius: 22;" +
                "-fx-border-color: #f1c7da;" +
                "-fx-border-radius: 22;" +
                "-fx-border-width: 1.5;" +
                "-fx-padding: 2 10 2 10;" +
                "-fx-font-size: 13.5px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #566589;"
        );
        cvBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(ApplicantCv cv, boolean empty) {
                super.updateItem(cv, empty);
                setText(empty || cv == null ? "" : cv.title());
                setStyle("-fx-text-fill: #566589; -fx-font-weight: bold;");
            }
        });
        cvBox.setCellFactory(listView -> new ListCell<>() {
            {
                selectedProperty().addListener((obs, oldVal, newVal) -> updateCellStyle());
                hoverProperty().addListener((obs, oldVal, newVal) -> updateCellStyle());
            }

            @Override
            protected void updateItem(ApplicantCv cv, boolean empty) {
                super.updateItem(cv, empty);
                setText(empty || cv == null ? "" : cv.title());
                updateCellStyle();
            }

            private void updateCellStyle() {
                if (isEmpty()) {
                    setStyle("");
                    return;
                }
                if (isSelected()) {
                    setStyle("-fx-background-color: #7fdae9; -fx-text-fill: #243b6b; -fx-font-weight: bold; -fx-padding: 10 12 10 12;");
                } else if (isHover()) {
                    setStyle("-fx-background-color: #d9f4f9; -fx-text-fill: #2f3553; -fx-font-weight: bold; -fx-padding: 10 12 10 12;");
                } else {
                    setStyle("-fx-background-color: white; -fx-text-fill: #2f3553; -fx-padding: 10 12 10 12;");
                }
            }
        });
        cvBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ApplicantCv applicantCv) {
                if (applicantCv == null) {
                    return "";
                }
                return applicantCv.title();
            }

            @Override
            public ApplicantCv fromString(String string) {
                return null;
            }
        });
        return cvBox;
    }

    private static void loadCvs(UiAppContext context, ComboBox<ApplicantCv> cvBox, Label statusLabel) {
        if (!context.session().isAuthenticated()) {
            return;
        }

        try {
            var cvs = context.services().cvLibraryService()
                .listCvsByUserId(context.session().userId())
                .stream()
                .sorted(Comparator.comparing(ApplicantCv::cvId))
                .toList();
            cvBox.getItems().addAll(cvs);
            if (!cvs.isEmpty()) {
                cvBox.getSelectionModel().selectFirst();
            }
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }
}
