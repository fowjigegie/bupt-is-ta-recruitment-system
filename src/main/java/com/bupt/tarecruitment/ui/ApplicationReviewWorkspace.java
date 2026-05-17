package com.bupt.tarecruitment.ui;

import java.util.List;

import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.job.JobPosting;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

final class ApplicationReviewWorkspace {
    private final ComboBox<JobPosting> jobBox;
    private final VBox applicantListBox;
    private final Label detailTitle;
    private final VBox detailContent;
    private final Label actionStatus;
    private final ObjectProperty<JobApplication> selectedApplication;
    private final HBox contentRow;

    private ApplicationReviewWorkspace(
        ComboBox<JobPosting> jobBox,
        VBox applicantListBox,
        Label detailTitle,
        VBox detailContent,
        Label actionStatus,
        ObjectProperty<JobApplication> selectedApplication,
        HBox contentRow
    ) {
        this.jobBox = jobBox;
        this.applicantListBox = applicantListBox;
        this.detailTitle = detailTitle;
        this.detailContent = detailContent;
        this.actionStatus = actionStatus;
        this.selectedApplication = selectedApplication;
        this.contentRow = contentRow;
    }

    static ApplicationReviewWorkspace create(List<JobPosting> ownedJobs, JobPosting initialJob) {
        ComboBox<JobPosting> jobBox = new ComboBox<>();
        jobBox.setPrefWidth(520);
        jobBox.setPrefHeight(42);
        jobBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(JobPosting job) {
                if (job == null) {
                    return "";
                }
                return job.jobId() + "|" + job.title() + "|" + job.moduleOrActivity();
            }

            @Override
            public JobPosting fromString(String string) {
                return null;
            }
        });
        jobBox.getItems().addAll(ownedJobs);
        jobBox.getSelectionModel().select(initialJob);

        VBox applicantListBox = new VBox(0);
        applicantListBox.setPadding(new Insets(6, 2, 6, 2));

        ScrollPane listScroll = new ScrollPane(applicantListBox);
        listScroll.setFitToWidth(true);
        listScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        listScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        listScroll.setPrefViewportHeight(560);
        listScroll.setStyle(
            "-fx-background:#faf9fc;" +
                "-fx-background-color:#faf9fc;" +
                "-fx-border-color:transparent;" +
                "-fx-background-radius:14;"
        );

        Label detailTitle = new Label("Select an applicant");
        detailTitle.setStyle(
            "-fx-font-family: Arial;" +
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4664a8;" +
                "-fx-background-color: #fff2bf;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 4 10 4 10;"
        );

        VBox detailContent = new VBox(14);
        detailContent.setPadding(new Insets(4, 2, 4, 2));
        detailContent.getChildren().add(UiTheme.createMutedText("Click \"View\" to load full CV here."));

        ScrollPane detailScroll = new ScrollPane(detailContent);
        detailScroll.setFitToWidth(true);
        detailScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        detailScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        detailScroll.setPrefViewportHeight(560);
        detailScroll.setStyle(
            "-fx-background:#ffffff;" +
                "-fx-background-color:#ffffff;" +
                "-fx-border-color:#f0d9e9;" +
                "-fx-border-radius:12;" +
                "-fx-background-radius:12;"
        );

        Label actionStatus = UiTheme.createMutedText("");
        actionStatus.setTextFill(Color.web("#b00020"));

        ObjectProperty<JobApplication> selectedApplication = new SimpleObjectProperty<>(null);

        VBox leftPane = new VBox(10);
        Label applicantsLabel = new Label("Applicants");
        applicantsLabel.setStyle(
            "-fx-font-family: Arial;" +
                "-fx-font-size: 24px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4664a8;"
        );
        leftPane.getChildren().addAll(applicantsLabel, listScroll);
        leftPane.setPadding(new Insets(14));
        leftPane.setBackground(new Background(new BackgroundFill(Color.web("#fdfcff"), new CornerRadii(18), Insets.EMPTY)));
        leftPane.setBorder(new Border(new BorderStroke(
            Color.web("#f0d9e9"), BorderStrokeStyle.SOLID, new CornerRadii(18), new BorderWidths(1.2)
        )));
        leftPane.setPrefWidth(520);
        leftPane.setMinWidth(500);

        VBox rightPane = new VBox(12, detailTitle, detailScroll, actionStatus);
        rightPane.setPadding(new Insets(14));
        rightPane.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(18), Insets.EMPTY)));
        rightPane.setBorder(new Border(new BorderStroke(
            Color.web("#f0d9e9"), BorderStrokeStyle.SOLID, new CornerRadii(18), new BorderWidths(1.2)
        )));
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        HBox contentRow = new HBox(14, leftPane, rightPane);
        contentRow.setFillHeight(true);

        return new ApplicationReviewWorkspace(
            jobBox,
            applicantListBox,
            detailTitle,
            detailContent,
            actionStatus,
            selectedApplication,
            contentRow
        );
    }

    ComboBox<JobPosting> jobBox() {
        return jobBox;
    }

    VBox applicantListBox() {
        return applicantListBox;
    }

    Label detailTitle() {
        return detailTitle;
    }

    VBox detailContent() {
        return detailContent;
    }

    Label actionStatus() {
        return actionStatus;
    }

    JobApplication selectedApplication() {
        return selectedApplication.get();
    }

    void setSelectedApplication(JobApplication application) {
        selectedApplication.set(application);
    }

    HBox contentRow() {
        return contentRow;
    }
}
