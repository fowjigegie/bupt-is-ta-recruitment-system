package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.common.skill.SkillCatalog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 提供技能搜索、分类浏览、推荐选择和自定义添加的输入组件。
 */
final class SkillPicker {
    private final Map<String, List<String>> categorizedSuggestions;
    private final LinkedHashSet<String> selectedSkills;
    private final VBox container;
    private final TextField searchField;
    private final VBox categoryBox;
    private final FlowPane selectedPane;
    private final Label helperText;
    private final LinkedHashMap<String, Boolean> expandedStates;

    private SkillPicker(
        Map<String, List<String>> categorizedSuggestions,
        LinkedHashSet<String> selectedSkills,
        VBox container,
        TextField searchField,
        VBox categoryBox,
        FlowPane selectedPane,
        Label helperText,
        LinkedHashMap<String, Boolean> expandedStates
    ) {
        this.categorizedSuggestions = categorizedSuggestions;
        this.selectedSkills = selectedSkills;
        this.container = container;
        this.searchField = searchField;
        this.categoryBox = categoryBox;
        this.selectedPane = selectedPane;
        this.helperText = helperText;
        this.expandedStates = expandedStates;
    }

    static SkillPicker create(String labelText, String helper, Map<String, List<String>> categorizedSuggestions) {
        Label title = new Label(labelText);
        title.setFont(Font.font("Arial", FontWeight.BOLD, 17));
        title.setTextFill(Color.web("#4664a8"));

        TextField searchField = new TextField();
        searchField.setPromptText("Search skills or type your own");
        searchField.setPrefWidth(420);
        searchField.setPrefHeight(46);
        searchField.setFont(Font.font("Arial", 15));
        searchField.setStyle(
            "-fx-background-color: white;" +
                "-fx-background-radius: 22;" +
                "-fx-border-color: #f3b2df;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 22;" +
                "-fx-padding: 0 14 0 14;"
        );

        Button addButton = UiTheme.createOutlineButton("Add custom skill", 170, 46);

        FlowPane selectedPane = new FlowPane();
        selectedPane.setHgap(8);
        selectedPane.setVgap(8);
        selectedPane.setPrefWrapLength(720);

        VBox categoryBox = new VBox(10);
        categoryBox.setPadding(new Insets(4, 0, 4, 0));

        ScrollPane categoryScroll = new ScrollPane(categoryBox);
        categoryScroll.setFitToWidth(true);
        categoryScroll.setPrefViewportHeight(360);
        categoryScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        categoryScroll.setStyle(
            "-fx-background-color: transparent;" +
                "-fx-background: transparent;" +
                "-fx-border-color: transparent;"
        );

        Label helperText = UiTheme.createMutedText(helper);
        LinkedHashSet<String> selectedSkills = new LinkedHashSet<>();
        LinkedHashMap<String, Boolean> expandedStates = new LinkedHashMap<>();

        SkillPicker picker = new SkillPicker(
            Collections.unmodifiableMap(new LinkedHashMap<>(categorizedSuggestions)),
            selectedSkills,
            new VBox(
                10,
                title,
                new HBox(12, searchField, addButton),
                createSectionLabel("Selected Skills"),
                selectedPane,
                createSectionLabel("Browse by Category"),
                categoryScroll,
                helperText
            ),
            searchField,
            categoryBox,
            selectedPane,
            helperText,
            expandedStates
        );

        picker.container.setAlignment(Pos.CENTER_LEFT);
        addButton.setOnAction(event -> picker.tryAddCurrentInput());
        searchField.textProperty().addListener((obs, oldValue, newValue) -> picker.refreshCategories());
        searchField.setOnAction(event -> picker.tryAddCurrentInput());
        picker.refreshSelectedTags();
        picker.refreshCategories();
        return picker;
    }

    VBox container() {
        return container;
    }

    List<String> skills() {
        return List.copyOf(selectedSkills);
    }

    void setSkills(List<String> skills) {
        selectedSkills.clear();
        for (String skill : skills) {
            String normalized = normalizeSkill(skill);
            if (!normalized.isBlank()) {
                selectedSkills.add(normalized);
            }
        }
        searchField.clear();
        refreshSelectedTags();
        refreshCategories();
    }

    private void tryAddCurrentInput() {
        String value = normalizeSkill(searchField.getText());
        if (value.isBlank()) {
            helperText.setText("Type a skill to search or add.");
            helperText.setTextFill(Color.web("#8b7fa0"));
            return;
        }
        if (!SkillCatalog.isValidSkillValue(value)) {
            helperText.setText("Skills can use letters, numbers, spaces, +, #, . and -.");
            helperText.setTextFill(Color.web("#b00020"));
            return;
        }
        if (selectedSkills.add(value)) {
            helperText.setText("Skill added to your current selection. Click Use selected skills to bring it back to the Resume page.");
            helperText.setTextFill(Color.web("#2e7d32"));
        } else {
            helperText.setText("That skill is already in your current selection.");
            helperText.setTextFill(Color.web("#8b7fa0"));
        }
        searchField.clear();
        refreshSelectedTags();
        refreshCategories();
    }

    private void refreshSelectedTags() {
        selectedPane.getChildren().clear();
        if (selectedSkills.isEmpty()) {
            selectedPane.getChildren().add(UiTheme.createMutedText("No skills selected yet."));
            return;
        }

        for (String skill : selectedSkills) {
            Label tagLabel = new Label(skill);
            tagLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            tagLabel.setTextFill(Color.web("#5c3f6b"));

            Button removeButton = new Button("x");
            removeButton.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            removeButton.setStyle(
                "-fx-background-color: transparent;" +
                    "-fx-text-fill: #8a4f7a;" +
                    "-fx-cursor: hand;"
            );
            removeButton.setOnAction(event -> {
                selectedSkills.remove(skill);
                helperText.setText("Skill removed from your current selection.");
                helperText.setTextFill(Color.web("#4664a8"));
                refreshSelectedTags();
                refreshCategories();
            });

            HBox tag = new HBox(6, tagLabel, removeButton);
            tag.setAlignment(Pos.CENTER_LEFT);
            tag.setPadding(new Insets(6, 10, 6, 10));
            tag.setStyle(
                "-fx-background-color: #ffe6f2;" +
                    "-fx-background-radius: 18;" +
                    "-fx-border-color: #f3b2df;" +
                    "-fx-border-radius: 18;" +
                    "-fx-border-width: 1.2;"
            );
            selectedPane.getChildren().add(tag);
        }
    }

    private void refreshCategories() {
        categoryBox.getChildren().clear();
        String query = SkillCatalog.normalize(searchField.getText());
        boolean hasQuery = !query.isBlank();
        boolean hasVisibleCategory = false;

        for (Map.Entry<String, List<String>> entry : categorizedSuggestions.entrySet()) {
            List<String> visibleSkills = entry.getValue().stream()
                .filter(skill -> !selectedSkills.contains(skill))
                .filter(skill -> query.isBlank() || SkillCatalog.normalize(skill).contains(query))
                .toList();

            if (visibleSkills.isEmpty()) {
                continue;
            }

            FlowPane skillButtons = new FlowPane();
            skillButtons.setHgap(10);
            skillButtons.setVgap(10);
            skillButtons.setPadding(new Insets(12, 10, 12, 10));
            skillButtons.setPrefWrapLength(720);

            for (String skill : visibleSkills) {
                Button skillButton = createSkillButton(skill);
                skillButton.setOnAction(event -> {
                    selectedSkills.add(skill);
                    searchField.clear();
                    helperText.setText("Skill added to your current selection. Click Use selected skills to bring it back to the Resume page.");
                    helperText.setTextFill(Color.web("#2e7d32"));
                    refreshSelectedTags();
                    refreshCategories();
                });
                skillButtons.getChildren().add(skillButton);
            }

            TitledPane titledPane = new TitledPane(entry.getKey() + " (" + visibleSkills.size() + ")", skillButtons);
            titledPane.setAnimated(false);
            titledPane.setCollapsible(true);
            titledPane.setExpanded(hasQuery || expandedStates.getOrDefault(entry.getKey(), !hasVisibleCategory));
            titledPane.expandedProperty().addListener((obs, oldValue, newValue) -> expandedStates.put(entry.getKey(), newValue));
            titledPane.setStyle(
                "-fx-text-fill: #4664a8;" +
                    "-fx-font-size: 15px;" +
                    "-fx-background-color: white;"
            );
            categoryBox.getChildren().add(titledPane);
            hasVisibleCategory = true;
        }

        if (!hasVisibleCategory) {
            categoryBox.getChildren().add(
                UiTheme.createMutedText("No suggested skills match your search. You can still type one and add it as a custom skill.")
            );
        }
    }

    private static Button createSkillButton(String text) {
        Button button = new Button(text);
        button.setWrapText(true);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        button.setPadding(new Insets(8, 12, 8, 12));
        button.setStyle(
            "-fx-background-color: #fff1f8;" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: #f3b2df;" +
                "-fx-border-width: 1.2;" +
                "-fx-border-radius: 18;" +
                "-fx-text-fill: #5c3f6b;" +
                "-fx-cursor: hand;"
        );
        return button;
    }

    private static Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        label.setTextFill(Color.web("#8a4f7a"));
        return label;
    }

    private static String normalizeSkill(String rawSkill) {
        if (rawSkill == null) {
            return "";
        }
        String normalized = SkillCatalog.normalize(rawSkill);
        if (normalized.isBlank()) {
            return "";
        }
        return List.of(normalized.split(" ")).stream()
            .filter(part -> !part.isBlank())
            .map(part -> part.length() <= 2
                ? part.toUpperCase(Locale.ROOT)
                : Character.toUpperCase(part.charAt(0)) + part.substring(1))
            .reduce((left, right) -> left + " " + right)
            .orElse("");
    }
}
