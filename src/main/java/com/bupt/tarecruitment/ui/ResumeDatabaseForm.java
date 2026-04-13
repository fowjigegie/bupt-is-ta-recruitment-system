package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.applicant.ApplicantCv;
import com.bupt.tarecruitment.applicant.ApplicantProfile;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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
    private final TextArea skillsArea;
    private final TextArea positionsArea;

    private ResumeDatabaseForm(
        TextField nameField,
        ComboBox<String> gradeBox,
        TextField programmeField,
        TextField studentIdField,
        ScheduleSlotPicker availabilityPicker,
        TextField cvTitleField,
        TextArea cvContentArea,
        TextArea skillsArea,
        TextArea positionsArea
    ) {
        this.nameField = nameField;
        this.gradeBox = gradeBox;
        this.programmeField = programmeField;
        this.studentIdField = studentIdField;
        this.availabilityPicker = availabilityPicker;
        this.cvTitleField = cvTitleField;
        this.cvContentArea = cvContentArea;
        this.skillsArea = skillsArea;
        this.positionsArea = positionsArea;
    }

    static ResumeDatabaseForm create() {
        TextField nameField = createRoundedField("Full Name", 260);
        ComboBox<String> gradeBox = createGradeBox();
        TextField programmeField = createRoundedField("Programme", 260);
        TextField studentIdField = createRoundedField("Student ID", 260);
        ScheduleSlotPicker availabilityPicker = ScheduleSlotPicker.create(
            "Availability",
            "Weekdays only. Whole-hour slots only. You can add up to 5 slots."
        );
        TextField cvTitleField = createRoundedField("CV Title", 544);
        TextArea cvContentArea = createLargeTextArea("Paste CV text here or import a local .txt file");
        cvContentArea.setPrefHeight(170);
        TextArea skillsArea = createLargeTextArea("Skills (letters only, separated by commas or new lines)");
        TextArea positionsArea = createLargeTextArea("Desired positions (letters only, separated by commas or new lines)");

        return new ResumeDatabaseForm(
            nameField,
            gradeBox,
            programmeField,
            studentIdField,
            availabilityPicker,
            cvTitleField,
            cvContentArea,
            skillsArea,
            positionsArea
        );
    }

    VBox createLeftForm() {
        VBox leftForm = new VBox(16);
        leftForm.setPrefWidth(700);
        leftForm.getChildren().addAll(
            new HBox(24, nameField, gradeBox),
            new HBox(24, programmeField, studentIdField),
            availabilityPicker.container(),
            cvTitleField,
            createLabeledTextArea("CV Text", cvContentArea),
            createLabeledTextArea("Skills", skillsArea),
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
            availabilityPicker.setSlots(profile.availabilitySlots());
            skillsArea.setText(String.join(System.lineSeparator(), profile.skills()));
            positionsArea.setText(String.join(System.lineSeparator(), profile.desiredPositions()));
        });
    }

    void loadCv(UiAppContext context, ApplicantCv cv) {
        cvTitleField.setText(cv.title());
        prefillProfile(context);

        try {
            String content = context.services().cvLibraryService().loadCvContentByCvId(cv.cvId());
            cvContentArea.setText(content);
            applyCvContent(content);
        } catch (IllegalArgumentException ignored) {
            cvContentArea.clear();
        }
    }

    ApplicantProfile toApplicantProfile(UiAppContext context) {
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
            splitValueList(skillsArea.getText()),
            availabilityPicker.slots(),
            splitValueList(positionsArea.getText())
        );
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
            applyCvContent(content);
            statusLabel.setTextFill(Color.web("#2e7d32"));
            statusLabel.setText("Local .txt file loaded into the CV editor.");
        } catch (IOException exception) {
            statusLabel.setTextFill(Color.web("#b00020"));
            statusLabel.setText("Failed to read the selected .txt file: " + exception.getMessage());
        }
    }

    private String buildCvContent() {
        return String.join(
            System.lineSeparator(),
            "Name: " + nameField.getText().trim(),
            "Grade: " + gradeBox.getValue(),
            "Programme: " + programmeField.getText().trim(),
            "Student ID: " + studentIdField.getText().trim(),
            "Availability: " + availabilityPicker.formattedSlots(),
            "Skills: " + String.join(", ", splitValueList(skillsArea.getText())),
            "Desired Positions: " + String.join(", ", splitValueList(positionsArea.getText()))
        );
    }

    private void applyCvContent(String content) {
        if (content == null || content.isBlank()) {
            return;
        }

        for (String line : content.split("\\R")) {
            if (line.startsWith("Name: ")) {
                nameField.setText(line.substring("Name: ".length()).trim());
            } else if (line.startsWith("Grade: ")) {
                gradeBox.setValue(line.substring("Grade: ".length()).trim());
            } else if (line.startsWith("Programme: ")) {
                programmeField.setText(line.substring("Programme: ".length()).trim());
            } else if (line.startsWith("Student ID: ")) {
                studentIdField.setText(line.substring("Student ID: ".length()).trim());
            } else if (line.startsWith("Availability: ")) {
                availabilityPicker.setSlots(splitValueList(line.substring("Availability: ".length()).trim()));
            } else if (line.startsWith("Skills: ")) {
                skillsArea.setText(line.substring("Skills: ".length()).trim().replace(", ", System.lineSeparator()));
            } else if (line.startsWith("Desired Positions: ")) {
                positionsArea.setText(line.substring("Desired Positions: ".length()).trim().replace(", ", System.lineSeparator()));
            }
        }
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

    private static TextField createRoundedField(String prompt, double width) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefWidth(width);
        field.setPrefHeight(58);
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
        gradeBox.setPrefWidth(260);
        gradeBox.setPrefHeight(58);
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
        area.setPrefWidth(544);
        area.setPrefHeight(130);
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
        label.setFont(Font.font("Arial", FontWeight.BOLD, 17));
        label.setTextFill(Color.web("#4664a8"));

        VBox box = new VBox(6, label, area);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private record GradeMapping(int yearOfStudy, String educationLevel) {
    }
}
