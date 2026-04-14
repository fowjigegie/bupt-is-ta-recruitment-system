package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.applicant.ApplicantCv;
import com.bupt.tarecruitment.applicant.ApplicantProfile;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
        ResumeDraftState draftState = context.resumeDraft();
        AtomicReference<Boolean> keepDraftUnselectedRef = new AtomicReference<>(
            draftState != null && draftState.selectedCvId() == null
        );

        javafx.scene.control.Label statusLabel = new javafx.scene.control.Label();
        statusLabel.setWrapText(true);
        statusLabel.setTextFill(Color.web("#b00020"));

        AtomicReference<ApplicantCv> selectedCvRef = new AtomicReference<>(
            findCvById(context, draftState == null ? null : draftState.selectedCvId()).orElse(null)
        );
        VBox tabsRow = new VBox(8);
        javafx.scene.control.Label selectedCvLabel = new javafx.scene.control.Label("Selected CV: none");
        selectedCvLabel.setTextFill(Color.web("#4664a8"));
        selectedCvLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        AtomicReference<Runnable> refreshAvatarPreviewRef = new AtomicReference<>(() -> {
        });

        Runnable refreshCvTabs = () -> rebuildCvTabs(
            context,
            tabsRow,
            selectedCvRef,
            selectedCvLabel,
            form,
            refreshAvatarPreviewRef.get(),
            keepDraftUnselectedRef
        );

        form.prefillProfile(context);
        if (selectedCvRef.get() != null) {
            form.loadCv(context, selectedCvRef.get());
        }
        refreshCvTabs.run();
        if (draftState != null) {
            form.applyDraft(draftState);
        }

        VBox content = new VBox(18);
        content.setFillWidth(true);
        content.getChildren().addAll(
            tabsRow,
            selectedCvLabel,
            createFormSection(
                nav,
                context,
                refreshCvTabs,
                selectedCvRef,
                form,
                statusLabel,
                refreshAvatarPreviewRef,
                keepDraftUnselectedRef
            ),
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
        ResumeDatabaseForm form,
        Runnable refreshAvatarPreview,
        AtomicReference<Boolean> keepDraftUnselectedRef
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
            keepDraftUnselectedRef.set(false);
        } else {
            ApplicantCv activeCv = selectedCvRef.get();
            String activeCvId = activeCv == null ? null : activeCv.cvId();
            boolean activeCvExists = activeCvId != null && cvs.stream().anyMatch(cv -> cv.cvId().equals(activeCvId));

            if (!activeCvExists && activeCvId == null && keepDraftUnselectedRef.get()) {
                selectedCvRef.set(null);
                selectedCvLabel.setText("Selected CV: none");
            } else if (!activeCvExists) {
                activeCv = cvs.getFirst();
                selectedCvRef.set(activeCv);
                form.loadCv(context, activeCv);
                refreshAvatarPreview.run();
                keepDraftUnselectedRef.set(false);
            }

            if (activeCv != null) {
                selectedCvLabel.setText("Selected CV: " + activeCv.cvId() + " | " + activeCv.title());
            }

            for (ApplicantCv cv : cvs) {
                boolean selected = activeCv != null && activeCv.cvId().equals(cv.cvId());
                var button = selected
                    ? UiTheme.createPrimaryButton(cv.title(), 170, 52)
                    : UiTheme.createOutlineButton(cv.title(), 170, 52);
                button.setOnAction(event -> {
                    selectedCvRef.set(cv);
                    keepDraftUnselectedRef.set(false);
                    selectedCvLabel.setText("Selected CV: " + cv.cvId() + " | " + cv.title());
                    form.loadCv(context, cv);
                    refreshAvatarPreview.run();
                    rebuildCvTabs(
                        context,
                        tabsRow,
                        selectedCvRef,
                        selectedCvLabel,
                        form,
                        refreshAvatarPreview,
                        keepDraftUnselectedRef
                    );
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
        javafx.scene.control.Label statusLabel,
        AtomicReference<Runnable> refreshAvatarPreviewRef,
        AtomicReference<Boolean> keepDraftUnselectedRef
    ) {
        HBox mainRow = new HBox(28);
        mainRow.setAlignment(Pos.TOP_LEFT);
        mainRow.setFillHeight(true);
        mainRow.setMaxWidth(Double.MAX_VALUE);

        VBox leftForm = form.createLeftForm(() -> {
            ApplicantCv selectedCv = selectedCvRef.get();
            context.saveResumeDraft(form.toDraft(selectedCv == null ? null : selectedCv.cvId()));
            nav.goTo(PageId.SKILL_SELECTOR);
        });
        ScrollPane leftScroll = new ScrollPane(leftForm);
        leftScroll.setFitToWidth(true);
        leftScroll.setPannable(true);
        leftScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        leftScroll.setPrefViewportWidth(780);
        leftScroll.setPrefViewportHeight(640);
        leftScroll.setStyle(
            "-fx-background-color: transparent;" +
                "-fx-background: transparent;" +
                "-fx-border-color: transparent;"
        );
        HBox.setHgrow(leftScroll, Priority.ALWAYS);

        VBox rightPanel = new VBox(18);
        rightPanel.setAlignment(Pos.TOP_CENTER);
        rightPanel.setPrefWidth(300);
        rightPanel.setMinWidth(300);

        StackPane avatarPane = new StackPane();
        Button uploadAvatarButton = UiTheme.createOutlineButton("Upload avatar", 180, 46);
        Button removeAvatarButton = UiTheme.createOutlineButton("Remove avatar", 180, 46);
        Runnable refreshAvatarPreview = () -> {
            form.syncStoredAvatarState(
                context.services().applicantAvatarStorageService().hasAvatarForUser(context.session().userId())
            );
            rebuildAvatarPane(context, form, avatarPane);
            removeAvatarButton.setDisable(!form.hasAvatarPreview());
        };
        refreshAvatarPreviewRef.set(refreshAvatarPreview);
        refreshAvatarPreview.run();

        uploadAvatarButton.setOnAction(event -> chooseAvatar(context, form, statusLabel, refreshAvatarPreview));
        removeAvatarButton.setOnAction(event -> {
            context.services().applicantAvatarStorageService().deleteAvatarForUser(context.session().userId());
            form.syncStoredAvatarState(false);
            refreshAvatarPreview.run();
            statusLabel.setTextFill(Color.web("#4664a8"));
            statusLabel.setText("Avatar removed.");
        });

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
                context.clearResumeDraft();
                refreshAvatarPreview.run();
                refreshProfileActionState(context, saveProfileButton, profileStateLabel);
                statusLabel.setTextFill(Color.web("#2e7d32"));
                statusLabel.setText(updatingExistingProfile ? "Profile updated successfully." : "Profile created successfully.");
            } catch (RuntimeException exception) {
                statusLabel.setTextFill(Color.web("#b00020"));
                statusLabel.setText(exception.getMessage());
            }
        });

        var saveNewCvButton = UiTheme.createOutlineButton("Save new CV", 180, 46);
        saveNewCvButton.setOnAction(event -> {
            try {
                saveProfile(context, form);
                context.clearResumeDraft();
                keepDraftUnselectedRef.set(false);
                refreshAvatarPreview.run();
                refreshProfileActionState(context, saveProfileButton, profileStateLabel);
                ApplicantCv createdCv = context.services().cvLibraryService().createCv(
                    context.session().userId(),
                    form.cvTitle(),
                    form.resolveCvContent()
                );
                selectedCvRef.set(createdCv);
                statusLabel.setTextFill(Color.web("#2e7d32"));
                statusLabel.setText("New CV saved successfully.");
                refreshCvTabs.run();
            } catch (RuntimeException exception) {
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
                context.clearResumeDraft();
                keepDraftUnselectedRef.set(false);
                refreshAvatarPreview.run();
                refreshProfileActionState(context, saveProfileButton, profileStateLabel);
                context.services().cvLibraryService().updateCvContent(
                    selectedCv.cvId(),
                    form.resolveCvContent()
                );
                statusLabel.setTextFill(Color.web("#2e7d32"));
                statusLabel.setText("CV updated successfully: " + selectedCv.cvId());
                refreshCvTabs.run();
            } catch (RuntimeException exception) {
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
            UiTheme.createMutedText("Upload a JPG or PNG avatar to copy it into the profile avatar folder."),
            uploadAvatarButton,
            removeAvatarButton,
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

        mainRow.getChildren().addAll(leftScroll, rightPanel);
        return mainRow;
    }

    private static ApplicantProfile saveProfile(UiAppContext context, ResumeDatabaseForm form) {
        ApplicantProfile profile = form.toApplicantProfile(context, "");
        if (context.services().profileRepository().findByUserId(context.session().userId()).isPresent()) {
            ApplicantProfile updated = context.services().profileService().updateProfile(profile);
            form.syncStoredAvatarState(
                context.services().applicantAvatarStorageService().hasAvatarForUser(context.session().userId())
            );
            return updated;
        }
        ApplicantProfile created = context.services().profileService().createProfile(profile);
        form.syncStoredAvatarState(
            context.services().applicantAvatarStorageService().hasAvatarForUser(context.session().userId())
        );
        return created;
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

    private static void chooseAvatar(
        UiAppContext context,
        ResumeDatabaseForm form,
        Label statusLabel,
        Runnable refreshAvatarPreview
    ) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose an avatar image");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image files (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg")
        );

        if (statusLabel.getScene() == null || statusLabel.getScene().getWindow() == null) {
            return;
        }

        var selectedFile = chooser.showOpenDialog(statusLabel.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        try {
            context.services().applicantAvatarStorageService().storeAvatar(context.session().userId(), selectedFile.toPath());
            form.syncStoredAvatarState(true);
            refreshAvatarPreview.run();
            statusLabel.setTextFill(Color.web("#2e7d32"));
            statusLabel.setText("Avatar uploaded and copied into the project avatar folder.");
        } catch (RuntimeException exception) {
            statusLabel.setTextFill(Color.web("#b00020"));
            statusLabel.setText(exception.getMessage());
        }
    }

    private static void rebuildAvatarPane(UiAppContext context, ResumeDatabaseForm form, StackPane avatarPane) {
        avatarPane.getChildren().clear();
        avatarPane.setPrefSize(160, 170);
        avatarPane.setStyle(
            "-fx-background-color: white;" +
                "-fx-background-radius: 24;" +
                "-fx-border-color: #f3b2df;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 24;" +
                "-fx-padding: 16;"
        );

        Optional<Path> previewPath = resolveAvatarPreviewPath(context, form);
        if (previewPath.isPresent()) {
            try {
                Image image = new Image(previewPath.get().toUri().toString(), 112, 112, true, true, true);
                if (!image.isError()) {
                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(112);
                    imageView.setFitHeight(112);
                    imageView.setPreserveRatio(true);
                    VBox box = new VBox(8, imageView, UiTheme.createMutedText("Profile avatar"));
                    box.setAlignment(Pos.CENTER);
                    avatarPane.getChildren().add(box);
                    return;
                }
            } catch (Exception ignored) {
            }
        }

        Rectangle placeholder = new Rectangle(96, 112);
        placeholder.setArcWidth(28);
        placeholder.setArcHeight(28);
        placeholder.setFill(Color.TRANSPARENT);
        placeholder.setStroke(Color.web("#db4b87"));
        placeholder.setStrokeWidth(3);

        VBox box = new VBox(8, new StackPane(placeholder), UiTheme.createMutedText("No avatar selected"));
        box.setAlignment(Pos.CENTER);
        avatarPane.getChildren().add(box);
    }

    private static Optional<Path> resolveAvatarPreviewPath(UiAppContext context, ResumeDatabaseForm form) {
        if (form.hasAvatarPreview()) {
            Optional<Path> avatarForUser = context.services().applicantAvatarStorageService()
                .resolveAvatarForUser(context.session().userId());
            if (avatarForUser.isPresent()) {
                return avatarForUser;
            }
        }
        return Optional.empty();
    }

    private static HBox createBottomHelperRow(NavigationManager nav) {
        HBox helperRow = new HBox(UiTheme.createBackButton(nav));
        helperRow.setAlignment(Pos.CENTER_LEFT);
        return helperRow;
    }

    private static Optional<ApplicantCv> findCvById(UiAppContext context, String cvId) {
        if (cvId == null || cvId.isBlank()) {
            return Optional.empty();
        }
        return context.services().cvLibraryService().listCvsByUserId(context.session().userId()).stream()
            .filter(cv -> cv.cvId().equals(cvId))
            .findFirst();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
