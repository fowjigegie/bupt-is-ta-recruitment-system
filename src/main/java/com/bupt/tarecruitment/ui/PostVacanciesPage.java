package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobStatus;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * 岗位发布页，负责创建和编辑岗位的整体流程。
 */
public class PostVacanciesPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.POST_VACANCIES, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        VBox center = new VBox(22);
        center.setPadding(new Insets(20, 40, 28, 40));

        Optional<JobPosting> editJob = resolveEditJob(context);
        boolean isEditMode = editJob.isPresent();
        PostVacancyForm form = PostVacancyForm.create(context.session().userId());
        editJob.ifPresent(form::load);

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setTextFill(Color.web("#b00020"));
        statusLabel.managedProperty().bind(Bindings.createBooleanBinding(
            () -> statusLabel.getText() != null && !statusLabel.getText().isBlank(),
            statusLabel.textProperty()
        ));
        statusLabel.visibleProperty().bind(statusLabel.managedProperty());

        Label titleLabel = new Label(isEditMode ? "Edit Posting" : "New Posting");
        titleLabel.setStyle(
            "-fx-font-size: 32px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #ff66b3;"
        );

        VBox formBox = new VBox(14);
        formBox.setPadding(new Insets(28));
        formBox.setPrefWidth(930);
        formBox.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(26), Insets.EMPTY)));
        formBox.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(26),
            new BorderWidths(2)
        )));

        var publishButton = UiTheme.createPrimaryButton(isEditMode ? "Update" : "Publish", 180, 52);
        publishButton.setOnAction(event -> {
            if (isEditMode) {
                update(context, editJob.get(), form, statusLabel);
            } else {
                publish(context, form, statusLabel);
            }
        });

        var clearButton = UiTheme.createSoftButton("Clear", 110, 46);
        clearButton.setOnAction(event -> {
            if (isEditMode) {
                form.load(editJob.get());
            } else {
                form.clearForCreate();
            }
            statusLabel.setText("");
        });

        HBox actionRow = new HBox(14, clearButton, publishButton);
        actionRow.setAlignment(Pos.CENTER);
        actionRow.setPadding(new Insets(2, 0, 0, 0));

        formBox.getChildren().addAll(
            form.createFirstRow(),
            form.createMetaRow(),
            form.createScheduleSelectorBox(),
            form.createDetailRow(),
            statusLabel,
            actionRow
        );

        HBox footer = new HBox(UiTheme.createBackButton(nav));
        footer.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        center.getChildren().addAll(titleLabel, formBox, spacer, footer);

        BorderPane root = UiTheme.createPage(
            "Post Vacancies",
            UiTheme.createMoSidebar(nav, PageId.POST_VACANCIES),
            center,
            nav,
            context
        );
        return UiTheme.createScene(root);
    }

    private static Optional<JobPosting> resolveEditJob(UiAppContext context) {
        return Optional.ofNullable(context.editingJobId())
            .flatMap(jobId -> context.services().jobRepository().findByJobId(jobId))
            .filter(job -> job.organiserId().equals(context.session().userId()))
            .filter(job -> {
                if (job.status() == JobStatus.CLOSED) {
                    context.clearJobEdit();
                    return false;
                }
                return true;
            });
    }

    private static void publish(
        UiAppContext context,
        PostVacancyForm form,
        Label statusLabel
    ) {
        statusLabel.setTextFill(Color.web("#b00020"));
        statusLabel.setText("");

        final int weeklyHours;
        try {
            weeklyHours = form.parseWeeklyHours();
        } catch (NumberFormatException exception) {
            statusLabel.setText("Weekly hours must be an integer.");
            return;
        }

        try {
            JobPosting posting = context.services().jobPostingService().publish(
                form.organiserId(),
                form.title(),
                form.moduleOrActivity(),
                form.description(),
                form.requiredSkills(),
                weeklyHours,
                form.scheduleSlots()
            );
            context.selectJob(posting.jobId());
            statusLabel.setTextFill(Color.web("#2e7d32"));
            statusLabel.setText("Published successfully: " + posting.jobId());
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    private static void update(
        UiAppContext context,
        JobPosting existingJob,
        PostVacancyForm form,
        Label statusLabel
    ) {
        statusLabel.setTextFill(Color.web("#b00020"));
        statusLabel.setText("");

        final int weeklyHours;
        try {
            weeklyHours = form.parseWeeklyHours();
        } catch (NumberFormatException exception) {
            statusLabel.setText("Weekly hours must be an integer.");
            return;
        }

        try {
            JobPosting updated = new JobPosting(
                existingJob.jobId(),
                form.organiserId(),
                form.title(),
                form.moduleOrActivity(),
                form.description(),
                form.requiredSkills(),
                weeklyHours,
                form.scheduleSlots(),
                existingJob.status() == null ? JobStatus.OPEN : existingJob.status()
            );

            context.services().jobPostingService().publish(updated);
            context.selectJob(updated.jobId());
            context.clearJobEdit();
            statusLabel.setTextFill(Color.web("#2e7d32"));
            statusLabel.setText("Updated successfully: " + updated.jobId());
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
