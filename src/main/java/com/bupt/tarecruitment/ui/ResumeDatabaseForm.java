package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.applicant.ApplicantCv;
import com.bupt.tarecruitment.applicant.ApplicantProfile;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 封装简历库页面的表单输入、导入和内容组装逻辑。
 */
final class ResumeDatabaseForm {
    private final TextField nameField;
    private final ComboBox<String> gradeBox;
    private final TextField programmeField;
    private final TextField studentIdField;
    private final ScheduleSlotPicker availabilityPicker;
    private final TextField cvTitleField;
    private final TextArea cvContentArea;
    private final TextArea positionsArea;
    private final LinkedHashSet<String> selectedSkills;
    private boolean storedAvatarPresent;

    private ResumeDatabaseForm(
        TextField nameField,
        ComboBox<String> gradeBox,
        TextField programmeField,
        TextField studentIdField,
        ScheduleSlotPicker availabilityPicker,
        TextField cvTitleField,
        TextArea cvContentArea,
        TextArea positionsArea
    ) {
        this.nameField = nameField;
        this.gradeBox = gradeBox;
        this.programmeField = programmeField;
        this.studentIdField = studentIdField;
        this.availabilityPicker = availabilityPicker;
        this.cvTitleField = cvTitleField;
        this.cvContentArea = cvContentArea;
        this.positionsArea = positionsArea;
        this.selectedSkills = new LinkedHashSet<>();
        this.storedAvatarPresent = false;
    }

    static ResumeDatabaseForm create() {
        TextField nameField = createRoundedField("Full Name", 280);
        ComboBox<String> gradeBox = createGradeBox();
        TextField programmeField = createRoundedField("Programme", 280);
        TextField studentIdField = createRoundedField("Student ID", 280);
        ScheduleSlotPicker availabilityPicker = ScheduleSlotPicker.create(
            "Availability",
            "Weekdays only. Whole-hour slots only. You can add up to 5 slots."
        );
        TextField cvTitleField = createRoundedField("CV Title", 600);
        TextArea cvContentArea = createLargeTextArea("Paste CV text here or import a local .txt file");
        cvContentArea.setPrefHeight(190);
        TextArea positionsArea = createLargeTextArea("Desired positions (letters only, separated by commas or new lines)");
        positionsArea.setPrefHeight(145);

        return new ResumeDatabaseForm(
            nameField,
            gradeBox,
            programmeField,
            studentIdField,
            availabilityPicker,
            cvTitleField,
            cvContentArea,
            positionsArea
        );
    }

    VBox createLeftForm(Runnable openSkillSelectionAction) {
        VBox leftForm = new VBox(16);
        leftForm.setPrefWidth(780);
        leftForm.setMaxWidth(Double.MAX_VALUE);
        leftForm.getChildren().addAll(
            new HBox(24, nameField, gradeBox),
            new HBox(24, programmeField, studentIdField),
            availabilityPicker.container(),
            createSkillsSummarySection(openSkillSelectionAction),
            cvTitleField,
            createLabeledTextArea("CV Text", cvContentArea),
            createLabeledTextArea("Desired Positions", positionsArea)
        );
        return leftForm;
    }

    String cvTitle() {
        return cvTitleField.getText().trim();
    }

    void prefillProfile(UiAppContext context) {
        context.services().profileRepository().findByUserId(context.session().userId()).ifPresent(profile -> {
            nameField.setText(profile.fullName());
            gradeBox.setValue(gradeLabel(profile.yearOfStudy(), profile.educationLevel()));
            programmeField.setText(profile.programme());
            studentIdField.setText(profile.studentId());
            syncStoredAvatarState(
                context.services().applicantAvatarStorageService().hasAvatarForUser(profile.userId())
                    || !profile.avatarPath().isBlank()
            );
            availabilityPicker.setSlots(profile.availabilitySlots());
            setSkills(profile.skills());
            positionsArea.setText(String.join(System.lineSeparator(), profile.desiredPositions()));
        });
    }

    void loadCv(UiAppContext context, ApplicantCv cv) {
        cvTitleField.setText(cv.title());
        try {
            String content = context.services().cvLibraryService().loadCvContentByCvId(cv.cvId());
            cvContentArea.setText(content);
        } catch (IllegalArgumentException ignored) {
            cvContentArea.clear();
        }
    }

    ApplicantProfile toApplicantProfile(UiAppContext context, String avatarPath) {
        GradeMapping mapping = mapGrade(gradeBox.getValue());
        return new ApplicantProfile(
            context.services().profileRepository().findByUserId(context.session().userId())
                .map(ApplicantProfile::profileId)
                .orElseGet(() -> context.services().profileIdGenerator().nextProfileId()),
            context.session().userId(),
            studentIdField.getText().trim(),
            nameField.getText().trim(),
            programmeField.getText().trim(),
            mapping.yearOfStudy(),
            mapping.educationLevel(),
            skills(),
            availabilityPicker.slots(),
            splitValueList(positionsArea.getText()),
            avatarPath
        );
    }

    ResumeDraftState toDraft(String selectedCvId) {
        return new ResumeDraftState(
            selectedCvId,
            nameField.getText(),
            gradeBox.getValue(),
            programmeField.getText(),
            studentIdField.getText(),
            availabilityPicker.slots(),
            cvTitleField.getText(),
            cvContentArea.getText(),
            skills(),
            positionsArea.getText()
        );
    }

    void applyDraft(ResumeDraftState draftState) {
        nameField.setText(draftState.fullName());
        gradeBox.setValue(draftState.gradeLabel());
        programmeField.setText(draftState.programme());
        studentIdField.setText(draftState.studentId());
        availabilityPicker.setSlots(draftState.availabilitySlots());
        cvTitleField.setText(draftState.cvTitle());
        cvContentArea.setText(draftState.cvContent());
        setSkills(draftState.skills());
        positionsArea.setText(draftState.desiredPositionsText());
    }

    List<String> skills() {
        return List.copyOf(selectedSkills);
    }

    void setSkills(List<String> skills) {
        selectedSkills.clear();
        for (String skill : skills) {
            String normalized = normalizeDisplaySkill(skill);
            if (!normalized.isBlank()) {
                selectedSkills.add(normalized);
            }
        }
    }

    void syncStoredAvatarState(boolean storedAvatarPresent) {
        this.storedAvatarPresent = storedAvatarPresent;
    }

    boolean hasAvatarPreview() {
        return storedAvatarPresent;
    }

    String resolveCvContent() {
        String rawContent = cvContentArea.getText();
        if (rawContent != null && !rawContent.isBlank()) {
            return rawContent.strip();
        }
        return buildCvContent();
    }

    void importTxtCv(Label statusLabel) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a .txt CV file");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt"));

        Path selectedPath = null;
        if (cvTitleField.getScene() != null && cvTitleField.getScene().getWindow() != null) {
            var selectedFile = chooser.showOpenDialog(cvTitleField.getScene().getWindow());
            if (selectedFile != null) {
                selectedPath = selectedFile.toPath();
            }
        }

        if (selectedPath == null) {
            return;
        }

        String fileName = selectedPath.getFileName().toString();
        if (!fileName.toLowerCase().endsWith(".txt")) {
            statusLabel.setTextFill(Color.web("#b00020"));
            statusLabel.setText("Only .txt CV files are supported.");
            return;
        }

        try {
            String content = Files.readString(selectedPath, StandardCharsets.UTF_8);
            cvContentArea.setText(content);
            if (cvTitleField.getText().isBlank()) {
                cvTitleField.setText(fileName.substring(0, fileName.length() - 4));
            }
            statusLabel.setTextFill(Color.web("#2e7d32"));
            statusLabel.setText("Local .txt file loaded into the CV editor.");
        } catch (IOException exception) {
            statusLabel.setTextFill(Color.web("#b00020"));
            statusLabel.setText("Failed to read the selected .txt file: " + exception.getMessage());
        }
    }

    private VBox createSkillsSummarySection(Runnable openSkillSelectionAction) {
        Label label = new Label("Skills");
        label.setStyle(
            "-fx-font-family: Arial;" +
                "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4664a8;"
        );

        Label helper = UiTheme.createMutedText(
            selectedSkills.isEmpty()
                ? "Choose skills on the dedicated skill page."
                : "Edit your profile skills on the dedicated skill page."
        );
        helper.setStyle(
            "-fx-font-family: Arial;" +
                "-fx-font-size: 14px;" +
                "-fx-text-fill: #8b7fa0;"
        );

        FlowPane skillsPane = new FlowPane();
        skillsPane.setHgap(8);
        skillsPane.setVgap(8);
        skillsPane.setPrefWrapLength(620);

        if (selectedSkills.isEmpty()) {
            skillsPane.getChildren().add(UiTheme.createMutedText("No skills selected yet."));
        } else {
            for (String skill : selectedSkills) {
                Label tag = new Label(skill);
                tag.setPadding(new Insets(6, 10, 6, 10));
                tag.setStyle(
                    "-fx-background-color: #ffe6f2;" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: #f3b2df;" +
                        "-fx-border-radius: 18;" +
                        "-fx-border-width: 1.2;" +
                        "-fx-font-family: Arial;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #5c3f6b;"
                );
                skillsPane.getChildren().add(tag);
            }
        }

        Button openButton = UiTheme.createOutlineButton("Choose skills", 180, 46);
        openButton.setOnAction(event -> openSkillSelectionAction.run());

        VBox box = new VBox(5, label, helper, skillsPane, openButton);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(12));
        box.setStyle(
            "-fx-background-color: white;" +
                "-fx-background-radius: 20;" +
                "-fx-border-color: #f3b2df;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 20;"
        );
        return box;
    }

    private String buildCvContent() {
        return String.join(
            System.lineSeparator(),
            "Name: " + nameField.getText().trim(),
            "Grade: " + gradeBox.getValue(),
            "Programme: " + programmeField.getText().trim(),
            "Student ID: " + studentIdField.getText().trim(),
            "Availability: " + availabilityPicker.formattedSlots(),
            "Skills: " + String.join(", ", skills()),
            "Desired Positions: " + String.join(", ", splitValueList(positionsArea.getText()))
        );
    }

    private static List<String> splitValueList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        return List.of(raw.split("(?:,|;|\\R)+")).stream()
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList();
    }

    private static String gradeLabel(int year, String educationLevel) {
        boolean graduated = "graduated".equalsIgnoreCase(educationLevel);
        if (!graduated && year == 4) {
            return "Senior undergraduate";
        }
        return switch (year) {
            case 1 -> "First-year graduate";
            case 2 -> "Second-year graduate";
            case 3 -> "Third-year graduate";
            default -> "Senior undergraduate";
        };
    }

    private static GradeMapping mapGrade(String label) {
        if (label == null) {
            return new GradeMapping(4, "Not Graduated");
        }

        return switch (label) {
            case "First-year graduate" -> new GradeMapping(1, "Graduated");
            case "Second-year graduate" -> new GradeMapping(2, "Graduated");
            case "Third-year graduate" -> new GradeMapping(3, "Graduated");
            default -> new GradeMapping(4, "Not Graduated");
        };
    }

    private static String normalizeDisplaySkill(String rawSkill) {
        if (rawSkill == null || rawSkill.isBlank()) {
            return "";
        }
        return List.of(rawSkill.trim().split("\\s+")).stream()
            .filter(part -> !part.isBlank())
            .map(part -> part.length() <= 2
                ? part.toUpperCase()
                : Character.toUpperCase(part.charAt(0)) + part.substring(1).toLowerCase())
            .reduce((left, right) -> left + " " + right)
            .orElse("");
    }

    private static TextField createRoundedField(String prompt, double width) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefWidth(width);
        field.setPrefHeight(54);
        field.setFont(Font.font("Arial", 16));
        field.setStyle(
            "-fx-background-color: #eba8df;" +
                "-fx-background-radius: 25;" +
                "-fx-border-radius: 25;" +
                "-fx-border-color: transparent;" +
                "-fx-prompt-text-fill: white;" +
                "-fx-text-fill: white;" +
                "-fx-padding: 0 18 0 18;"
        );
        return field;
    }

    private static ComboBox<String> createGradeBox() {
        ComboBox<String> gradeBox = new ComboBox<>();
        gradeBox.getItems().addAll(
            "Senior undergraduate",
            "First-year graduate",
            "Second-year graduate",
            "Third-year graduate"
        );
        gradeBox.setValue("Senior undergraduate");
        gradeBox.setPrefWidth(280);
        gradeBox.setPrefHeight(54);
        gradeBox.setStyle(
            "-fx-background-color: #eba8df;" +
                "-fx-background-radius: 25;" +
                "-fx-border-radius: 25;" +
                "-fx-font-size: 16px;" +
                "-fx-padding: 0 12 0 12;"
        );
        return gradeBox;
    }

    private static TextArea createLargeTextArea(String prompt) {
        TextArea area = new TextArea();
        area.setPromptText(prompt);
        area.setPrefWidth(620);
        area.setPrefHeight(145);
        area.setWrapText(true);
        area.setFont(Font.font("Arial", 16));
        area.setStyle(
            "-fx-control-inner-background: white;" +
                "-fx-background-color: white;" +
                "-fx-background-radius: 0;" +
                "-fx-border-color: #f3b2df;" +
                "-fx-border-width: 3;" +
                "-fx-prompt-text-fill: black;" +
                "-fx-padding: 12;"
        );
        return area;
    }

    private static VBox createLabeledTextArea(String labelText, TextArea area) {
        Label label = new Label(labelText);
        label.setStyle(
            "-fx-font-family: Arial;" +
                "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4664a8;"
        );

        VBox box = new VBox(6, label, area);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private record GradeMapping(int yearOfStudy, String educationLevel) {
    }
}
