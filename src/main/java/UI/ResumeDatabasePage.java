package UI;

import com.bupt.tarecruitment.applicant.ApplicantCv;
import com.bupt.tarecruitment.applicant.ApplicantProfile;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ResumeDatabasePage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.RESUME_DATABASE, stage);
    }

    // US02 主页面：
    // applicant 可以在这里维护 profile 的结构化字段，并创建、查看、更新自己名下的多份 CV。
    static Scene createScene(NavigationManager nav, UiAppContext context) {
        BorderPane centerPane = new BorderPane();
        centerPane.setPadding(new Insets(24, 32, 24, 32));

        TextField nameField = createRoundedField("Full Name", 260);
        ComboBox<String> gradeBox = createGradeBox();
        TextField programmeField = createRoundedField("Programme", 260);
        TextField studentIdField = createRoundedField("Student ID", 260);
        TextField availabilityField = createRoundedField("Availability slots separated by ';'", 544);
        TextField cvTitleField = createRoundedField("CV Title", 544);
        TextArea cvContentArea = createLargeTextArea("Paste CV text here or import a local .txt file");
        cvContentArea.setPrefHeight(170);
        TextArea skillsArea = createLargeTextArea("Skills (letters only, separated by commas or new lines)");
        TextArea positionsArea = createLargeTextArea("Desired positions (letters only, separated by commas or new lines)");

        javafx.scene.control.Label statusLabel = new javafx.scene.control.Label();
        statusLabel.setWrapText(true);
        statusLabel.setTextFill(Color.web("#b00020"));

        AtomicReference<ApplicantCv> selectedCvRef = new AtomicReference<>();
        VBox tabsRow = new VBox(8);
        javafx.scene.control.Label selectedCvLabel = new javafx.scene.control.Label("Selected CV: none");
        selectedCvLabel.setTextFill(Color.web("#4664a8"));
        selectedCvLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Runnable refreshCvTabs = () -> rebuildCvTabs(
            context,
            tabsRow,
            selectedCvRef,
            selectedCvLabel,
            nameField,
            gradeBox,
            programmeField,
            studentIdField,
            availabilityField,
            cvTitleField,
            cvContentArea,
            skillsArea,
            positionsArea
        );

        prefillProfile(context, nameField, gradeBox, programmeField, studentIdField, availabilityField, skillsArea, positionsArea);
        refreshCvTabs.run();

        VBox content = new VBox(18);
        content.getChildren().addAll(
            tabsRow,
            selectedCvLabel,
            createFormSection(
                nav,
                context,
                refreshCvTabs,
                selectedCvRef,
                nameField,
                gradeBox,
                programmeField,
                studentIdField,
                availabilityField,
                cvTitleField,
                cvContentArea,
                skillsArea,
                positionsArea,
                statusLabel
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

    // 根据当前 applicant 已保存的 CV 元数据，重建顶部的“CV 标签栏”。
    // 选中某一份 CV 时，会把 metadata 和正文一起加载回表单。
    private static void rebuildCvTabs(
        UiAppContext context,
        VBox tabsRow,
        AtomicReference<ApplicantCv> selectedCvRef,
        javafx.scene.control.Label selectedCvLabel,
        TextField nameField,
        ComboBox<String> gradeBox,
        TextField programmeField,
        TextField studentIdField,
        TextField availabilityField,
        TextField cvTitleField,
        TextArea cvContentArea,
        TextArea skillsArea,
        TextArea positionsArea
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
                loadCvIntoFields(
                    context,
                    activeCv,
                    nameField,
                    gradeBox,
                    programmeField,
                    studentIdField,
                    availabilityField,
                    cvTitleField,
                    cvContentArea,
                    skillsArea,
                    positionsArea
                );
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
                    loadCvIntoFields(
                        context,
                        cv,
                        nameField,
                        gradeBox,
                        programmeField,
                        studentIdField,
                        availabilityField,
                        cvTitleField,
                        cvContentArea,
                        skillsArea,
                        positionsArea
                    );
                    rebuildCvTabs(
                        context,
                        tabsRow,
                        selectedCvRef,
                        selectedCvLabel,
                        nameField,
                        gradeBox,
                        programmeField,
                        studentIdField,
                        availabilityField,
                        cvTitleField,
                        cvContentArea,
                        skillsArea,
                        positionsArea
                    );
                });
                tabRow.getChildren().add(button);
            }
        }

        tabsRow.getChildren().add(tabRow);
    }

    // 左侧是 profile/CV 编辑区，右侧是 avatar、按钮和当前状态提示。
    // 这里把“保存 profile”和“创建/更新 CV”两条动作都组织到同一个页面里。
    private static HBox createFormSection(
        NavigationManager nav,
        UiAppContext context,
        Runnable refreshCvTabs,
        AtomicReference<ApplicantCv> selectedCvRef,
        TextField nameField,
        ComboBox<String> gradeBox,
        TextField programmeField,
        TextField studentIdField,
        TextField availabilityField,
        TextField cvTitleField,
        TextArea cvContentArea,
        TextArea skillsArea,
        TextArea positionsArea,
        javafx.scene.control.Label statusLabel
    ) {
        HBox mainRow = new HBox(28);
        mainRow.setAlignment(Pos.TOP_LEFT);

        VBox leftForm = new VBox(16);
        leftForm.setPrefWidth(700);

        HBox row1 = new HBox(24, nameField, gradeBox);
        HBox row2 = new HBox(24, programmeField, studentIdField);
        leftForm.getChildren().addAll(
            row1,
            row2,
            availabilityField,
            cvTitleField,
            createLabeledTextArea("CV Text", cvContentArea),
            createLabeledTextArea("Skills", skillsArea),
            createLabeledTextArea("Desired Positions", positionsArea)
        );

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
                saveProfile(context, nameField, gradeBox, programmeField, studentIdField, availabilityField, skillsArea, positionsArea);
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
                saveProfile(context, nameField, gradeBox, programmeField, studentIdField, availabilityField, skillsArea, positionsArea);
                refreshProfileActionState(context, saveProfileButton, profileStateLabel);
                context.services().cvLibraryService().createCv(
                    context.session().userId(),
                    cvTitleField.getText().trim(),
                    resolveCvContent(
                        cvContentArea,
                        nameField,
                        gradeBox,
                        programmeField,
                        studentIdField,
                        availabilityField,
                        skillsArea,
                        positionsArea
                    )
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
                saveProfile(context, nameField, gradeBox, programmeField, studentIdField, availabilityField, skillsArea, positionsArea);
                refreshProfileActionState(context, saveProfileButton, profileStateLabel);
                context.services().cvLibraryService().updateCvContent(
                    selectedCv.cvId(),
                    resolveCvContent(
                        cvContentArea,
                        nameField,
                        gradeBox,
                        programmeField,
                        studentIdField,
                        availabilityField,
                        skillsArea,
                        positionsArea
                    )
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
        importButton.setOnAction(event -> importTxtCv(
            cvTitleField,
            cvContentArea,
            nameField,
            gradeBox,
            programmeField,
            studentIdField,
            availabilityField,
            skillsArea,
            positionsArea,
            statusLabel
        ));

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

    // 页面首次打开时，先把 applicant 已有的 profile 字段回填到表单里。
    // 这也是 US05“编辑 profile”的起点。
    private static void prefillProfile(
        UiAppContext context,
        TextField nameField,
        ComboBox<String> gradeBox,
        TextField programmeField,
        TextField studentIdField,
        TextField availabilityField,
        TextArea skillsArea,
        TextArea positionsArea
    ) {
        // US01/US05: 如果已有 profile，进入页面时先把字段回填出来
        context.services().profileRepository().findByUserId(context.session().userId()).ifPresent(profile -> {
            nameField.setText(profile.fullName());
            gradeBox.setValue(gradeLabel(profile.yearOfStudy(), profile.educationLevel()));
            programmeField.setText(profile.programme());
            studentIdField.setText(profile.studentId());
            availabilityField.setText(String.join("; ", profile.availabilitySlots()));
            skillsArea.setText(String.join(System.lineSeparator(), profile.skills()));
            positionsArea.setText(String.join(System.lineSeparator(), profile.desiredPositions()));
        });
    }

    // 用户点击顶部某个 CV 标签后，把该 CV 的 metadata 和正文加载回表单。
    private static void loadCvIntoFields(
        UiAppContext context,
        ApplicantCv cv,
        TextField nameField,
        ComboBox<String> gradeBox,
        TextField programmeField,
        TextField studentIdField,
        TextField availabilityField,
        TextField cvTitleField,
        TextArea cvContentArea,
        TextArea skillsArea,
        TextArea positionsArea
    ) {
        cvTitleField.setText(cv.title());
        prefillProfile(context, nameField, gradeBox, programmeField, studentIdField, availabilityField, skillsArea, positionsArea);

        try {
            String content = context.services().cvLibraryService().loadCvContentByCvId(cv.cvId());
            cvContentArea.setText(content);
            applyCvContent(content, nameField, gradeBox, programmeField, studentIdField, availabilityField, skillsArea, positionsArea);
        } catch (IllegalArgumentException ignored) {
            cvContentArea.clear();
        }
    }

    // 如果导入的 txt 中带有 Name: / Skills: / Student ID: 这种结构化行，
    // 就顺手把这些字段回填到表单；如果只是普通纯文本，则只填 CV Text。
    private static void applyCvContent(
        String content,
        TextField nameField,
        ComboBox<String> gradeBox,
        TextField programmeField,
        TextField studentIdField,
        TextField availabilityField,
        TextArea skillsArea,
        TextArea positionsArea
    ) {
        if (content == null || content.isBlank()) {
            return;
        }

        // 如果 CV 文本里包含 "Name: / Student ID:" 这种结构化行，会自动回填到表单字段
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
                availabilityField.setText(line.substring("Availability: ".length()).trim());
            } else if (line.startsWith("Skills: ")) {
                skillsArea.setText(line.substring("Skills: ".length()).trim().replace(", ", System.lineSeparator()));
            } else if (line.startsWith("Desired Positions: ")) {
                positionsArea.setText(line.substring("Desired Positions: ".length()).trim().replace(", ", System.lineSeparator()));
            }
        }
    }

    // 这个页面同时承载 US01 和 US05：
    // 如果当前 user 还没有保存过 profile，就走 create；
    // 如果已经有 profile，就走 update。
    // Profile 的保存入口：
    // 已有 profile 时走 update；还没有 profile 时走 create。
    // 这就是 US01（创建）和 US05（编辑）共用的一条 UI 提交路径。
    private static ApplicantProfile saveProfile(
        UiAppContext context,
        TextField nameField,
        ComboBox<String> gradeBox,
        TextField programmeField,
        TextField studentIdField,
        TextField availabilityField,
        TextArea skillsArea,
        TextArea positionsArea
    ) {
        // 这里把 UI 表单内容统一组装成 ApplicantProfile 对象。
        GradeMapping mapping = mapGrade(gradeBox.getValue());
        ApplicantProfile profile = new ApplicantProfile(
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
            splitValueList(availabilityField.getText()),
            splitValueList(positionsArea.getText())
        );

        if (context.services().profileRepository().findByUserId(context.session().userId()).isPresent()) {
            // 已有 profile => 走更新
            return context.services().profileService().updateProfile(profile);
        }
        // 没有 profile => 走新建
        return context.services().profileService().createProfile(profile);
    }

    // 用来判断按钮文案到底应该显示 Create profile 还是 Update profile。
    private static boolean hasSavedProfile(UiAppContext context) {
        return context.services().profileRepository().findByUserId(context.session().userId()).isPresent();
    }

    // 用按钮文案和提示语明确告诉用户：
    // 当前是在“创建 profile”还是“编辑已有 profile”。
    // 根据当前是否已有 profile，动态刷新按钮文案和辅助提示。
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

    // 当用户没有直接粘贴整份 CV 文本时，就把表单里的结构化字段拼成一份默认 CV 正文。
    private static String buildCvContent(
        TextField nameField,
        ComboBox<String> gradeBox,
        TextField programmeField,
        TextField studentIdField,
        TextField availabilityField,
        TextArea skillsArea,
        TextArea positionsArea
    ) {
        return String.join(
            System.lineSeparator(),
            "Name: " + nameField.getText().trim(),
            "Grade: " + gradeBox.getValue(),
            "Programme: " + programmeField.getText().trim(),
            "Student ID: " + studentIdField.getText().trim(),
            "Availability: " + availabilityField.getText().trim(),
            "Skills: " + String.join(", ", splitValueList(skillsArea.getText())),
            "Desired Positions: " + String.join(", ", splitValueList(positionsArea.getText()))
        );
    }

    // 优先使用用户真正编辑过的 CV Text；
    // 如果 CV Text 为空，再退回用结构化字段自动拼一份文本。
    private static String resolveCvContent(
        TextArea cvContentArea,
        TextField nameField,
        ComboBox<String> gradeBox,
        TextField programmeField,
        TextField studentIdField,
        TextField availabilityField,
        TextArea skillsArea,
        TextArea positionsArea
    ) {
        String rawContent = cvContentArea.getText();
        if (rawContent != null && !rawContent.isBlank()) {
            return rawContent.strip();
        }

        return buildCvContent(
            nameField,
            gradeBox,
            programmeField,
            studentIdField,
            availabilityField,
            skillsArea,
            positionsArea
        );
    }

    // 导入本地 txt：
    // 主要作用是把全文放进 CV Text，
    // 同时尝试识别其中是否有结构化字段可以回填到表单。
    private static void importTxtCv(
        TextField cvTitleField,
        TextArea cvContentArea,
        TextField nameField,
        ComboBox<String> gradeBox,
        TextField programmeField,
        TextField studentIdField,
        TextField availabilityField,
        TextArea skillsArea,
        TextArea positionsArea,
        javafx.scene.control.Label statusLabel
    ) {
        // 导入 .txt 只会填充 CV Text；如果文本里有结构化字段，会顺带回填表单
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
            applyCvContent(content, nameField, gradeBox, programmeField, studentIdField, availabilityField, skillsArea, positionsArea);
            statusLabel.setTextFill(Color.web("#2e7d32"));
            statusLabel.setText("Local .txt file loaded into the CV editor.");
        } catch (IOException exception) {
            statusLabel.setTextFill(Color.web("#b00020"));
            statusLabel.setText("Failed to read the selected .txt file: " + exception.getMessage());
        }
    }

    // Skills / Desired positions 这种列表字段统一在这里做拆分和清洗。
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
        javafx.scene.control.Label label = new javafx.scene.control.Label(labelText);
        label.setFont(Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 17));
        label.setTextFill(Color.web("#4664a8"));

        VBox box = new VBox(6, label, area);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
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
        HBox row = new HBox(UiTheme.createBackButton(nav));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private record GradeMapping(int yearOfStudy, String educationLevel) {
    }

    public static void main(String[] args) {
        launch(args);
    }
}
