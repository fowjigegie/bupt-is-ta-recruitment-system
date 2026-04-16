package com.bupt.tarecruitment.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 以弹窗形式复用技能选择器，适合需要局部编辑技能列表的页面。
 */
final class SkillPickerDialog {
    private SkillPickerDialog() {
    }

    static List<String> chooseSkills(
        Window owner,
        String dialogTitle,
        String pickerTitle,
        String helperText,
        String applyButtonText,
        Map<String, List<String>> categorizedSuggestions,
        List<String> initialSkills
    ) {
        SkillPicker picker = SkillPicker.create(pickerTitle, helperText, categorizedSuggestions);
        picker.setSkills(initialSkills);

        Stage dialog = new Stage();
        dialog.setTitle(dialogTitle);
        dialog.initModality(Modality.WINDOW_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        AtomicReference<List<String>> resultRef = new AtomicReference<>(List.copyOf(initialSkills));

        var applyButton = UiTheme.createPrimaryButton(applyButtonText, 220, 48);
        applyButton.setOnAction(event -> {
            resultRef.set(picker.skills());
            dialog.close();
        });

        var cancelButton = UiTheme.createOutlineButton("Cancel", 140, 48);
        cancelButton.setOnAction(event -> dialog.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actionRow = new HBox(12, spacer, cancelButton, applyButton);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(
            18,
            UiTheme.createPageHeading(dialogTitle),
            picker.container(),
            actionRow
        );
        content.setPadding(new Insets(24, 28, 24, 28));
        content.setBackground(UiTheme.pageBackground());

        Scene scene = new Scene(content, 980, 720);
        dialog.setScene(scene);
        dialog.showAndWait();
        return resultRef.get();
    }
}
