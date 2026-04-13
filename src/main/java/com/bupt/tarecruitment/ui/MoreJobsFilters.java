package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.job.JobBrowseFilter;
import com.bupt.tarecruitment.job.JobPosting;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * 封装岗位浏览页的筛选、排序和重置控件。
 */
final class MoreJobsFilters {
    private boolean sortNewestFirst = true;

    private final HBox searchBar;
    private final VBox filterPanel;
    private final Label resultLabel;
    private final Button sortButton;
    private final Button resetButton;
    private final TextField keywordField;
    private final ComboBox<String> moduleFilter;
    private final ComboBox<String> activityFilter;
    private final ComboBox<String> skillFilter;
    private final ComboBox<String> organiserFilter;
    private final ComboBox<String> timeSlotFilter;

    private MoreJobsFilters(
        HBox searchBar,
        VBox filterPanel,
        Label resultLabel,
        Button sortButton,
        Button resetButton,
        TextField keywordField,
        ComboBox<String> moduleFilter,
        ComboBox<String> activityFilter,
        ComboBox<String> skillFilter,
        ComboBox<String> organiserFilter,
        ComboBox<String> timeSlotFilter
    ) {
        this.searchBar = searchBar;
        this.filterPanel = filterPanel;
        this.resultLabel = resultLabel;
        this.sortButton = sortButton;
        this.resetButton = resetButton;
        this.keywordField = keywordField;
        this.moduleFilter = moduleFilter;
        this.activityFilter = activityFilter;
        this.skillFilter = skillFilter;
        this.organiserFilter = organiserFilter;
        this.timeSlotFilter = timeSlotFilter;
    }

    static MoreJobsFilters create(List<JobPosting> jobs) {
        HBox searchBar = new HBox(16);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setPadding(new Insets(0, 22, 0, 22));
        searchBar.setPrefHeight(56);
        searchBar.setBackground(new Background(new BackgroundFill(
            Color.web("#ffcce6"),
            new CornerRadii(28),
            Insets.EMPTY
        )));

        Label searchIcon = new Label("Jobs");
        searchIcon.setTextFill(Color.WHITE);
        searchIcon.setFont(Font.font("Arial", FontWeight.BOLD, 22));

        Region divider = new Region();
        divider.setPrefWidth(3);
        divider.setPrefHeight(34);
        divider.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

        Label resultLabel = new Label();
        resultLabel.setTextFill(Color.WHITE);
        resultLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Region searchSpacer = new Region();
        HBox.setHgrow(searchSpacer, Priority.ALWAYS);

        Button sortButton = UiTheme.createSoftButton("Sort by job ID: newest", 230, 44);
        sortButton.setStyle(baseSortButtonStyle(true));
        searchBar.getChildren().addAll(searchIcon, divider, resultLabel, searchSpacer, sortButton);

        TextField keywordField = createFilterField("Search title, module, skill, organiser, MO name, or job ID");
        HBox.setHgrow(keywordField, Priority.ALWAYS);

        ComboBox<String> moduleFilter = createFilterComboBox(190, "All modules");
        moduleFilter.getItems().addAll(JobBrowseFilter.collectUniqueModules(jobs));
        moduleFilter.getSelectionModel().selectFirst();

        ComboBox<String> activityFilter = createFilterComboBox(190, "All activity types");
        activityFilter.getItems().addAll(
            "Lab session",
            "Tutorial",
            "Assignment / marking",
            "Project / development"
        );
        activityFilter.getSelectionModel().selectFirst();

        ComboBox<String> skillFilter = createFilterComboBox(210, "All skills");
        skillFilter.getItems().addAll(JobBrowseFilter.collectUniqueSkills(jobs));
        skillFilter.getSelectionModel().selectFirst();

        ComboBox<String> organiserFilter = createFilterComboBox(200, "All organisers");
        organiserFilter.getItems().addAll(JobBrowseFilter.collectUniqueOrganisers(jobs));
        organiserFilter.getSelectionModel().selectFirst();

        ComboBox<String> timeSlotFilter = createFilterComboBox(170, "Any time");
        timeSlotFilter.getItems().addAll(
            "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN",
            "Morning", "Afternoon", "Evening"
        );
        timeSlotFilter.getSelectionModel().selectFirst();

        Button resetButton = UiTheme.createOutlineButton("Reset filters", 150, 44);

        HBox searchRow = new HBox(12, keywordField, resetButton);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        HBox filterRow = new HBox(12, moduleFilter, activityFilter, skillFilter, organiserFilter, timeSlotFilter);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        VBox filterPanel = new VBox(12, searchRow, filterRow);
        filterPanel.setPadding(new Insets(14, 16, 14, 16));
        filterPanel.setBackground(new Background(new BackgroundFill(
            Color.web("#fffafd"),
            new CornerRadii(18),
            Insets.EMPTY
        )));
        filterPanel.setBorder(new Border(new BorderStroke(
            Color.web("#f0d9e9"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(18),
            new BorderWidths(1.2)
        )));

        return new MoreJobsFilters(
            searchBar,
            filterPanel,
            resultLabel,
            sortButton,
            resetButton,
            keywordField,
            moduleFilter,
            activityFilter,
            skillFilter,
            organiserFilter,
            timeSlotFilter
        );
    }

    void attachRefresh(Runnable refresh) {
        resetButton.setOnAction(event -> {
            keywordField.clear();
            moduleFilter.getSelectionModel().selectFirst();
            activityFilter.getSelectionModel().selectFirst();
            skillFilter.getSelectionModel().selectFirst();
            organiserFilter.getSelectionModel().selectFirst();
            timeSlotFilter.getSelectionModel().selectFirst();
            refresh.run();
        });

        sortButton.setOnAction(event -> {
            sortNewestFirst = !sortNewestFirst;
            sortButton.setText(sortNewestFirst ? "Sort by job ID: newest" : "Sort by job ID: oldest");
            sortButton.setStyle(baseSortButtonStyle(sortNewestFirst));
            refresh.run();
        });

        keywordField.textProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        moduleFilter.valueProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        activityFilter.valueProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        skillFilter.valueProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        organiserFilter.valueProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        timeSlotFilter.valueProperty().addListener((obs, oldValue, newValue) -> refresh.run());
    }

    boolean sortNewestFirst() {
        return sortNewestFirst;
    }

    String keyword() {
        return keywordField.getText();
    }

    String module() {
        return moduleFilter.getValue();
    }

    String activity() {
        return activityFilter.getValue();
    }

    String skill() {
        return skillFilter.getValue();
    }

    String organiser() {
        return organiserFilter.getValue();
    }

    String timeSlot() {
        return timeSlotFilter.getValue();
    }

    void updateResults(int shownCount, long openJobs) {
        resultLabel.setText("Showing " + shownCount + " of " + openJobs + " OPEN jobs");
    }

    HBox searchBar() {
        return searchBar;
    }

    VBox filterPanel() {
        return filterPanel;
    }

    private static String baseSortButtonStyle(boolean newestFirst) {
        return newestFirst
            ? "-fx-background-color: #f8d7e9; -fx-background-radius: 22; -fx-text-fill: #2f3553; -fx-font-weight: bold; -fx-font-size: 14px;"
            : "-fx-background-color: #f3c7dd; -fx-background-radius: 22; -fx-text-fill: #2a2f4e; -fx-font-weight: bold; -fx-font-size: 14px;";
    }

    private static TextField createFilterField(String promptText) {
        TextField field = new TextField();
        field.setPromptText(promptText);
        field.setPrefHeight(44);
        field.setFont(Font.font("Arial", 15));
        field.setStyle(
            "-fx-background-color: white;" +
                "-fx-background-radius: 22;" +
                "-fx-border-color: #f1c7da;" +
                "-fx-border-radius: 22;" +
                "-fx-border-width: 1.5;" +
                "-fx-padding: 0 16 0 16;"
        );
        return field;
    }

    private static ComboBox<String> createFilterComboBox(double width, String firstOption) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setPrefWidth(width);
        comboBox.setPrefHeight(44);
        comboBox.getItems().add(firstOption);
        comboBox.setValue(firstOption);
        comboBox.setStyle(
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
        return comboBox;
    }
}
