package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.applicant.ApplicantCv;
import com.bupt.tarecruitment.applicant.ApplicantProfile;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 简历库页面，负责个人资料与简历信息的页面流程。
 */
public class ResumeDatabasePage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.RESUME_DATABASE, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        BorderPane centerPane = new BorderPane();
        centerPane.setPadding(new Insets(24, 32, 24, 32));

        ResumeDatabaseForm form = ResumeDatabaseForm.create();

        javafx.scene.control.Label statusLabel = new javafx.scene.control.Label();
        statusLabel.setWrapText(true);
        statusLabel.setTextFill(Color.web("#b00020"));

        AtomicReference<ApplicantCv> selectedCvRef = new AtomicReference<>();
        VBox tabsRow = new VBox(8);
        javafx.scene.control.Label selectedCvLabel = new javafx.scene.control.Label("Selected CV: none");
        selectedCvLabel.setTextFill(Color.web("#4664a8"));
        selectedCvLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Runnable refreshCvTabs = () -> rebuildCvTabs(context, tabsRow, selectedCvRef, selectedCvLabel, form);

        form.prefillProfile(context);
        refreshCvTabs.run();

        VBox content = new VBox(18);
        content.getChildren().addAll(
            tabsRow,
            selectedCvLabel,
            createFormSection(nav, context, refreshCvTabs, selectedCvRef, form, statusLabel),
            statusLabel,
            createBottomHelperRow(nav)
        );

        centerPane.setCenter(content);

        BorderPane root = UiTheme.createPage(
            "Resume Database",
            UiTheme.createApplicantSidebar(nav, PageId.RESUME_DATABASE),
            centerPane,
            nav,
            context
        );
        return UiTheme.createScene(root);
    }

    private static void rebuildCvTabs(
        UiAppContext context,
        VBox tabsRow,
        AtomicReference<ApplicantCv> selectedCvRef,
        javafx.scene.control.Label selectedCvLabel,
        ResumeDatabaseForm form
    ) {
        tabsRow.getChildren().clear();
        HBox tabRow = new HBox(12);
        tabRow.setAlignment(Pos.CENTER_LEFT);

        List<ApplicantCv> cvs = context.services().cvLibraryService()
            .listCvsByUserId(context.session().userId())
            .stream()
            .sorted(Comparator.comparing(ApplicantCv::cvId))
            .toList();

        if (cvs.isEmpty()) {
            tabRow.getChildren().add(UiTheme.createMutedText("No CVs yet. Save a new CV after filling the form below."));
            selectedCvRef.set(null);
            selectedCvLabel.setText("Selected CV: none");
        } else {
            ApplicantCv activeCv = selectedCvRef.get();
            String activeCvId = activeCv == null ? null : activeCv.cvId();
            if (activeCvId == null || cvs.stream().noneMatch(cv -> cv.cvId().equals(activeCvId))) {
                activeCv = cvs.getFirst();
                selectedCvRef.set(activeCv);
                form.loadCv(context, activeCv);
            }

            selectedCvLabel.setText("Selected CV: " + activeCv.cvId() + " | " + activeCv.title());

            for (ApplicantCv cv : cvs) {
                boolean selected = activeCv.cvId().equals(cv.cvId());
                var button = selected
                    ? UiTheme.createPrimaryButton(cv.title(), 170, 52)
                    : UiTheme.createOutlineButton(cv.title(), 170, 52);
                button.setOnAction(event -> {
                    selectedCvRef.set(cv);
                    selectedCvLabel.setText("Selected CV: " + cv.cvId() + " | " + cv.title());
                    form.loadCv(context, cv);
                    rebuildCvTabs(context, tabsRow, selectedCvRef, selectedCvLabel, form);
                });
                tabRow.getChildren().add(button);
            }
        }

        tabsRow.getChildren().add(tabRow);
    }

    private static HBox createFormSection(
        NavigationManager nav,
        UiAppContext context,
        Runnable refreshCvTabs,
        AtomicReference<ApplicantCv> selectedCvRef,
        ResumeDatabaseForm form,
        javafx.scene.control.Label statusLabel
    ) {
        HBox mainRow = new HBox(28);
        mainRow.setAlignment(Pos.TOP_LEFT);

        VBox leftForm = form.createLeftForm();

        VBox rightPanel = new VBox(18);
        rightPanel.setAlignment(Pos.TOP_CENTER);
        rightPanel.setPrefWidth(280);

        StackPane avatarPane = createAvatarPane();
        ProgressBar progressBar = new ProgressBar(0.85);
        progressBar.setPrefWidth(220);
        progressBar.setPrefHeight(18);
        progressBar.setStyle("-fx-accent: #f15bbe; -fx-control-inner-background: #f8c4ea;");

        var saveProfileButton = UiTheme.createOutlineButton("Create profile", 180, 46);
        var profileStateLabel = UiTheme.createMutedText("");
        refreshProfileActionState(context, saveProfileButton, profileStateLabel);
        saveProfileButton.setOnAction(event -> {
            boolean updatingExistingProfile = hasSavedProfile(context);
            try {
                saveProfile(context, form);
                refreshProfileActionState(context, saveProfileButton, profileStateLabel);
                statusLabel.setTextFill(Color.web("#2e7d32"));
                statusLabel.setText(updatingExistingProfile ? "Profile updated successfully." : "Profile created successfully.");
            } catch (IllegalArgumentException exception) {
                statusLabel.setTextFill(Color.web("#b00020"));
                statusLabel.setText(exception.getMessage());
            }
        });

        var saveNewCvButton = UiTheme.createOutlineButton("Save new CV", 180, 46);
        saveNewCvButton.setOnAction(event -> {
            try {
                saveProfile(context, form);
                refreshProfileActionState(context, saveProfileButton, profileStateLabel);
                context.services().cvLibraryService().createCv(
                    context.session().userId(),
                    form.cvTitle(),
                    form.resolveCvContent()
                );
                statusLabel.setTextFill(Color.web("#2e7d32"));
                statusLabel.setText("New CV saved successfully.");
                refreshCvTabs.run();
            } catch (IllegalArgumentException exception) {
                statusLabel.setTextFill(Color.web("#b00020"));
                statusLabel.setText(exception.getMessage());
            }
        });

        var updateCvButton = UiTheme.createOutlineButton("Update selected CV", 180, 46);
        updateCvButton.setOnAction(event -> {
            ApplicantCv selectedCv = selectedCvRef.get();
            if (selectedCv == null) {
                statusLabel.setTextFill(Color.web("#b00020"));
                statusLabel.setText("Please create or select a CV first.");
                return;
            }

            try {
                saveProfile(context, form);
                refreshProfileActionState(context, saveProfileButton, profileStateLabel);
                context.services().cvLibraryService().updateCvContent(
                    selectedCv.cvId(),
                    form.resolveCvContent()
                );
                statusLabel.setTextFill(Color.web("#2e7d32"));
                statusLabel.setText("CV updated successfully: " + selectedCv.cvId());
                refreshCvTabs.run();
            } catch (IllegalArgumentException exception) {
                statusLabel.setTextFill(Color.web("#b00020"));
                statusLabel.setText(exception.getMessage());
            }
        });

        var importButton = UiTheme.createOutlineButton("Import .txt CV", 180, 46);
        importButton.setOnAction(event -> form.importTxtCv(statusLabel));

        var chatButton = UiTheme.createOutlineButton("Open messages", 180, 46);
        chatButton.setOnAction(event -> nav.goTo(PageId.MESSAGES));

        rightPanel.getChildren().addAll(
            avatarPane,
            UiTheme.createMutedText("Save your profile first, then import, create, or update CVs here."),
            progressBar,
            UiTheme.createTag("Profile + CV", 220),
            profileStateLabel,
            saveProfileButton,
            importButton,
            saveNewCvButton,
            updateCvButton,
            chatButton
        );

        mainRow.getChildren().addAll(leftForm, rightPanel);
        return mainRow;
    }

    private static ApplicantProfile saveProfile(UiAppContext context, ResumeDatabaseForm form) {
        ApplicantProfile profile = form.toApplicantProfile(context);
        if (context.services().profileRepository().findByUserId(context.session().userId()).isPresent()) {
            return context.services().profileService().updateProfile(profile);
        }
        return context.services().profileService().createProfile(profile);
    }

    private static boolean hasSavedProfile(UiAppContext context) {
        return context.services().profileRepository().findByUserId(context.session().userId()).isPresent();
    }

    private static void refreshProfileActionState(
        UiAppContext context,
        javafx.scene.control.Button saveProfileButton,
        javafx.scene.control.Label profileStateLabel
    ) {
        boolean hasExistingProfile = hasSavedProfile(context);
        saveProfileButton.setText(hasExistingProfile ? "Update profile" : "Create profile");
        profileStateLabel.setText(
            hasExistingProfile
                ? "Editing existing applicant profile. Required fields are validated before saving."
                : "No saved profile yet. Fill the required fields to create your applicant profile."
        );
    }

    private static StackPane createAvatarPane() {
        StackPane avatarPane = new StackPane();
        avatarPane.setPrefSize(110, 130);

        Rectangle outer = new Rectangle(80, 95);
        outer.setFill(Color.TRANSPARENT);
        outer.setStroke(Color.web("#db4b87"));
        outer.setStrokeWidth(3);

        VBox box = new VBox(8, new StackPane(outer));
        box.setAlignment(Pos.CENTER);
        avatarPane.getChildren().add(box);
        return avatarPane;
    }

    private static HBox createBottomHelperRow(NavigationManager nav) {
        HBox helperRow = new HBox(UiTheme.createBackButton(nav));
        helperRow.setAlignment(Pos.CENTER_LEFT);
        return helperRow;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
