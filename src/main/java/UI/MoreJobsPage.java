package UI;

import com.bupt.tarecruitment.auth.UserAccount;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobStatus;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Applicant-facing "More jobs" page: browse OPEN postings with keyword and combo filters,
 * pagination, sort by inferred job sequence, and shortcuts to job detail / MO chat / AI advisor.
 */
public class MoreJobsPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.MORE_JOBS, stage);
    }

    /**
     * Builds the full page scene; job list is derived once from the repository (OPEN only) and
     * re-filtered in memory when inputs change.
     */
    static Scene createScene(NavigationManager nav, UiAppContext context) {
        final int pageSize = 3;
        // Mutable state held in one-element arrays so nested lambdas can update them.
        final int[] currentPage = {0};
        final boolean[] sortNewestFirst = {true};

        VBox center = new VBox(18);
        center.setPadding(new Insets(28, 40, 28, 40));

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

        // Fixed snapshot for this scene; filters only narrow this list (no live reload).
        List<JobPosting> jobs = context.services().jobRepository().findAll().stream()
            .filter(job -> job.status() == JobStatus.OPEN)
            .toList();

        Label placeholder = new Label();
        placeholder.setTextFill(Color.WHITE);
        placeholder.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Region searchSpacer = new Region();
        HBox.setHgrow(searchSpacer, Priority.ALWAYS);

        // Toggles sort direction for numeric suffix in job id (see extractJobSequenceNumber).
        Button sortButton = UiTheme.createSoftButton("Sort by publish time: newest", 260, 44);
        sortButton.setStyle("-fx-background-color: #f8d7e9; -fx-background-radius: 22; -fx-text-fill: #2f3553; -fx-font-weight: bold; -fx-font-size: 14px;");
        searchBar.getChildren().addAll(searchIcon, divider, placeholder, searchSpacer, sortButton);

        TextField keywordField = new TextField();
        keywordField.setPromptText("Search by keyword (course ID, MO id/name, title, skills, schedule...)");
        keywordField.setPrefHeight(40);
        HBox.setHgrow(keywordField, Priority.ALWAYS);
        keywordField.setStyle(
            "-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #e8c4d8; "
                + "-fx-border-radius: 10; -fx-padding: 0 12 0 12;"
        );

        ComboBox<String> moduleFilter = new ComboBox<>();
        moduleFilter.getItems().add("All modules");
        // Distinct module/activity labels from data, sorted for stable dropdown order.
        moduleFilter.getItems().addAll(jobs.stream()
            .map(JobPosting::moduleOrActivity)
            .filter(m -> m != null && !m.isBlank())
            .collect(Collectors.toCollection(TreeSet::new)));
        moduleFilter.getSelectionModel().selectFirst();

        ComboBox<String> activityFilter = new ComboBox<>();
        activityFilter.getItems().addAll(
            "All activity types",
            "Lab session",
            "Tutorial",
            "Assignment / marking",
            "Project / development"
        );
        activityFilter.getSelectionModel().selectFirst();

        ComboBox<String> skillFilter = new ComboBox<>();
        skillFilter.getItems().add("All skills");
        // Union of required skills across OPEN jobs, sorted.
        skillFilter.getItems().addAll(jobs.stream()
            .flatMap(j -> j.requiredSkills().stream())
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(Collectors.toCollection(TreeSet::new)));
        skillFilter.getSelectionModel().selectFirst();

        ComboBox<String> timeSlotFilter = new ComboBox<>();
        timeSlotFilter.getItems().addAll(
            "Any time",
            "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN",
            "Morning", "Afternoon", "Evening"
        );
        timeSlotFilter.getSelectionModel().selectFirst();

        double comboWidth = 160;
        moduleFilter.setPrefWidth(comboWidth);
        activityFilter.setPrefWidth(comboWidth + 20);
        skillFilter.setPrefWidth(comboWidth);
        timeSlotFilter.setPrefWidth(comboWidth);

        for (ComboBox<String> combo : List.of(moduleFilter, activityFilter, skillFilter, timeSlotFilter)) {
            styleFilterCombo(combo);
        }

        Button clearFiltersButton = UiTheme.createSoftButton("Clear filters", 130, 40);
        clearFiltersButton.setStyle(
            "-fx-background-color: #f0e6f5; -fx-background-radius: 10; -fx-text-fill: #2f3553; "
                + "-fx-font-weight: bold; -fx-font-size: 12px;"
        );

        HBox searchRow = new HBox(12, keywordField, clearFiltersButton);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        Label moduleLbl = new Label("Module");
        moduleLbl.setTextFill(Color.web("#6b5b7a"));
        moduleLbl.setFont(Font.font("Arial", 11));
        Label activityLbl = new Label("Activity");
        activityLbl.setTextFill(Color.web("#6b5b7a"));
        activityLbl.setFont(Font.font("Arial", 11));
        Label skillLbl = new Label("Skill");
        skillLbl.setTextFill(Color.web("#6b5b7a"));
        skillLbl.setFont(Font.font("Arial", 11));
        Label timeLbl = new Label("Time slot");
        timeLbl.setTextFill(Color.web("#6b5b7a"));
        timeLbl.setFont(Font.font("Arial", 11));

        VBox modCol = new VBox(4, moduleLbl, moduleFilter);
        VBox actCol = new VBox(4, activityLbl, activityFilter);
        VBox skCol = new VBox(4, skillLbl, skillFilter);
        VBox timeCol = new VBox(4, timeLbl, timeSlotFilter);

        Region filterAiSpacer = new Region();
        HBox.setHgrow(filterAiSpacer, Priority.ALWAYS);

        Label aiColLabel = new Label("AI Assistant");
        aiColLabel.setTextFill(Color.web("#6b5b7a"));
        aiColLabel.setFont(Font.font("Arial", 11));

        Button aiJobAdvisorButton = UiTheme.createSoftButton("AI Job Advisor", 168, 44);
        aiJobAdvisorButton.setStyle(
            "-fx-background-color: linear-gradient(to right, #ffe6f2, #ffd6e8);"
                + "-fx-text-fill: #b0306e;"
                + "-fx-font-weight: bold;"
                + "-fx-font-size: 13px;"
                + "-fx-background-radius: 22;"
        );
        aiJobAdvisorButton.setMinWidth(140);
        aiJobAdvisorButton.setOnAction(event -> JobAiChatWindow.open(nav, context, jobs));

        VBox aiCol = new VBox(4, aiColLabel, aiJobAdvisorButton);

        HBox filterRow = new HBox(14, modCol, actCol, skCol, timeCol, filterAiSpacer, aiCol);
        filterRow.setAlignment(Pos.BOTTOM_LEFT);

        VBox filterPanel = new VBox(12, searchRow, filterRow);
        filterPanel.setPadding(new Insets(16, 20, 16, 20));
        filterPanel.setBackground(new Background(new BackgroundFill(Color.web("#fffafd"), new CornerRadii(16), Insets.EMPTY)));
        filterPanel.setBorder(new Border(new BorderStroke(
            Color.web("#f0d9e9"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(16),
            new BorderWidths(1.2)
        )));

        VBox jobsList = new VBox(16);
        HBox pageSelector = new HBox(6);
        pageSelector.setAlignment(Pos.CENTER);
        pageSelector.setPadding(new Insets(4, 6, 4, 6));
        pageSelector.setBackground(new Background(new BackgroundFill(
            Color.web("#e7c4e3"),
            new CornerRadii(24),
            Insets.EMPTY
        )));

        // Forward declaration so pagination buttons can call back into the same refresh logic.
        Runnable[] refreshJobsView = new Runnable[1];
        refreshJobsView[0] = () -> {
            String kw = keywordField.getText() == null ? "" : keywordField.getText().trim();
            String mod = moduleFilter.getValue() == null ? "All modules" : moduleFilter.getValue();
            String act = activityFilter.getValue() == null ? "All activity types" : activityFilter.getValue();
            String sk = skillFilter.getValue() == null ? "All skills" : skillFilter.getValue();
            String time = timeSlotFilter.getValue() == null ? "Any time" : timeSlotFilter.getValue();

            // Keyword + combo filters (all must pass).
            List<JobPosting> filtered = jobs.stream()
                .filter(job -> matchesKeyword(job, context, kw))
                .filter(job -> matchesModule(job, mod))
                .filter(job -> matchesActivityType(job, act))
                .filter(job -> matchesSkill(job, sk))
                .filter(job -> matchesTimeSlot(job, time))
                .toList();

            if (jobs.isEmpty()) {
                placeholder.setText("Open jobs: 0");
            } else if (filtered.size() == jobs.size() && kw.isEmpty()
                && "All modules".equals(mod) && "All activity types".equals(act)
                && "All skills".equals(sk) && "Any time".equals(time)) {
                placeholder.setText("Open jobs: " + jobs.size());
            } else {
                placeholder.setText("Open: " + jobs.size() + " | Showing: " + filtered.size());
            }

            // Newest = highest numeric id chunk first (toggle reverses comparator only).
            List<JobPosting> sortedJobs = filtered.stream()
                .sorted(
                    Comparator.comparingInt(MoreJobsPage::extractJobSequenceNumber)
                        .thenComparing(JobPosting::jobId)
                        .reversed()
                )
                .toList();
            if (!sortNewestFirst[0]) {
                List<JobPosting> ascending = filtered.stream()
                    .sorted(Comparator.comparingInt(MoreJobsPage::extractJobSequenceNumber).thenComparing(JobPosting::jobId))
                    .toList();
                sortedJobs = ascending;
            }

            jobsList.getChildren().clear();
            if (sortedJobs.isEmpty()) {
                if (jobs.isEmpty()) {
                    jobsList.getChildren().add(UiTheme.createWhiteCard(
                        "No jobs",
                        "There are currently no OPEN jobs in the repository."
                    ));
                } else {
                    jobsList.getChildren().add(UiTheme.createWhiteCard(
                        "No matching jobs",
                        "Try different keywords or clear filters to see all open positions."
                    ));
                }
                pageSelector.getChildren().clear();
                return;
            }

            int totalPages = (sortedJobs.size() + pageSize - 1) / pageSize;
            currentPage[0] = Math.max(0, Math.min(currentPage[0], totalPages - 1));
            int fromIndex = currentPage[0] * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, sortedJobs.size());

            // One row card per job on this page.
            for (JobPosting job : sortedJobs.subList(fromIndex, toIndex)) {
                jobsList.getChildren().add(createJobCard(nav, context, job));
            }

            pageSelector.getChildren().clear();
            for (int pageNumber = 1; pageNumber <= totalPages; pageNumber++) {
                final int targetPage = pageNumber - 1;
                Button pageButton = new Button(String.valueOf(pageNumber));
                pageButton.setPrefWidth(54);
                pageButton.setPrefHeight(48);
                pageButton.setFont(Font.font("Arial", FontWeight.BOLD, 20));
                pageButton.setStyle(targetPage == currentPage[0]
                    ? "-fx-background-color: #c45786; -fx-background-radius: 22; -fx-text-fill: white;"
                    : "-fx-background-color: transparent; -fx-text-fill: #566589; -fx-background-radius: 22;");
                pageButton.setOnAction(event -> {
                    currentPage[0] = targetPage;
                    refreshJobsView[0].run();
                });
                pageSelector.getChildren().add(pageButton);
            }
        };

        // Any filter change jumps back to page 1.
        Runnable resetPageAndRefresh = () -> {
            currentPage[0] = 0;
            refreshJobsView[0].run();
        };

        keywordField.textProperty().addListener((obs, o, n) -> resetPageAndRefresh.run());
        moduleFilter.valueProperty().addListener((obs, o, n) -> resetPageAndRefresh.run());
        activityFilter.valueProperty().addListener((obs, o, n) -> resetPageAndRefresh.run());
        skillFilter.valueProperty().addListener((obs, o, n) -> resetPageAndRefresh.run());
        timeSlotFilter.valueProperty().addListener((obs, o, n) -> resetPageAndRefresh.run());

        clearFiltersButton.setOnAction(event -> {
            keywordField.clear();
            moduleFilter.getSelectionModel().selectFirst();
            activityFilter.getSelectionModel().selectFirst();
            skillFilter.getSelectionModel().selectFirst();
            timeSlotFilter.getSelectionModel().selectFirst();
            resetPageAndRefresh.run();
        });

        sortButton.setOnAction(event -> {
            sortNewestFirst[0] = !sortNewestFirst[0];
            sortButton.setText(sortNewestFirst[0] ? "Sort by publish time: newest" : "Sort by publish time: oldest");
            sortButton.setStyle(sortNewestFirst[0]
                ? "-fx-background-color: #f8d7e9; -fx-background-radius: 22; -fx-text-fill: #2f3553; -fx-font-weight: bold; -fx-font-size: 14px;"
                : "-fx-background-color: #f3c7dd; -fx-background-radius: 22; -fx-text-fill: #2a2f4e; -fx-font-weight: bold; -fx-font-size: 14px;");
            currentPage[0] = 0;
            refreshJobsView[0].run();
        });

        refreshJobsView[0].run();

        // Footer: back navigation + page strip (page buttons wired to refreshJobsView).
        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        HBox footer = new HBox(12, UiTheme.createBackButton(nav), footerSpacer, pageSelector);
        footer.setAlignment(Pos.CENTER_LEFT);

        center.getChildren().addAll(
            UiTheme.createPageHeading("More jobs"),
            filterPanel,
            searchBar,
            jobsList,
            footer
        );
        center.setFillWidth(true);
        center.setMaxWidth(Double.MAX_VALUE);

        // Main column scrolls vertically; horizontal overflow hidden.
        ScrollPane centerScroll = new ScrollPane(center);
        centerScroll.setFitToWidth(true);
        centerScroll.setPannable(false);
        centerScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        centerScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        centerScroll.setStyle(
            "-fx-background: transparent;"
                + "-fx-background-color: transparent;"
                + "-fx-padding: 0;"
        );
        URL scrollCss = MoreJobsPage.class.getResource("more-jobs-scroll.css");
        if (scrollCss != null) {
            centerScroll.getStylesheets().add(scrollCss.toExternalForm());
        }

        BorderPane root = UiTheme.createPage(
            "More Jobs",
            UiTheme.createApplicantSidebar(nav, PageId.MORE_JOBS),
            centerScroll,
            nav,
            context
        );
        return UiTheme.createScene(root);
    }

    /** Single job row: summary text plus View details and Chat with MO. */
    private static VBox createJobCard(NavigationManager nav, UiAppContext context, JobPosting job) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(20, 24, 20, 24));
        row.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(24), Insets.EMPTY)));
        row.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(24),
            new BorderWidths(1.5)
        )));

        VBox textBox = new VBox(6);
        Label courseLabel = new Label(job.title());
        courseLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        courseLabel.setTextFill(Color.web("#4664a8"));

        Label idLabel = new Label("Job ID : " + job.jobId());
        idLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        idLabel.setTextFill(Color.web("#ff66b3"));

        Label moLabel = new Label("MO : " + job.organiserId() + " | " + job.moduleOrActivity());
        moLabel.setFont(Font.font("Arial", 17));
        moLabel.setTextFill(Color.web("#8b7fa0"));

        Label hoursAndScheduleLabel = UiTheme.createMutedText(
            "Weekly Hours: " + job.weeklyHours() + "h/week | Schedule: " +
                (job.scheduleSlots().isEmpty() ? "(none listed)" : String.join(", ", job.scheduleSlots()))
        );

        Label skillsLabel = UiTheme.createMutedText(
            "Skills: " + (job.requiredSkills().isEmpty() ? "(none listed)" : String.join(", ", job.requiredSkills()))
        );
        textBox.getChildren().addAll(courseLabel, idLabel, moLabel, hoursAndScheduleLabel, skillsLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        var viewDetails = UiTheme.createSoftButton("View Details", 150, 44);
        viewDetails.setOnAction(event -> {
            context.selectJob(job.jobId());
            nav.goTo(PageId.JOB_DETAIL);
        });

        var chatButton = UiTheme.createSoftButton("Chat with MO", 180, 44);
        chatButton.setOnAction(event -> {
            context.openChatContext(job.jobId(), job.organiserId());
            nav.goTo(PageId.MESSAGES);
        });

        row.getChildren().addAll(textBox, spacer, viewDetails, chatButton);
        return new VBox(row);
    }

    public static void main(String[] args) {
        launch(args);
    }

    /** List cell used for filter {@link ComboBox} dropdown and button display. */
    private static ListCell<String> newFilterComboListCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle(
                    empty
                        ? ""
                        : "-fx-text-fill: #2f3553; -fx-font-size: 13px; -fx-padding: 8 12 8 12;"
                );
            }
        };
    }

    /** Shared pink/white styling for the four filter combo boxes. */
    private static void styleFilterCombo(ComboBox<String> combo) {
        combo.setStyle(
            "-fx-background-color: #ffffff;"
                + "-fx-background-radius: 12;"
                + "-fx-border-color: #e8c4d8;"
                + "-fx-border-radius: 12;"
                + "-fx-border-width: 1.2;"
                + "-fx-padding: 2 10 2 10;"
                + "-fx-font-size: 13px;"
                + "-fx-font-weight: 600;"
                + "-fx-text-fill: #2f3553;"
        );
        combo.setButtonCell(newFilterComboListCell());
        combo.setCellFactory(list -> newFilterComboListCell());
    }

    /**
     * Parses digits from {@link JobPosting#jobId()} for sort order (e.g. {@code TA-2025-00042} → 202500042).
     * Non-numeric ids sort last via {@link Integer#MIN_VALUE}.
     */
    private static int extractJobSequenceNumber(JobPosting job) {
        String jobId = job.jobId();
        String numeric = jobId.replaceAll("\\D+", "");
        if (numeric.isEmpty()) {
            return Integer.MIN_VALUE;
        }
        try {
            return Integer.parseInt(numeric);
        } catch (NumberFormatException ignored) {
            return Integer.MIN_VALUE;
        }
    }

    /** MO display name for keyword search, if the account exists. */
    private static String moDisplayName(UiAppContext context, String organiserId) {
        return context.services().userRepository().findByUserId(organiserId)
            .map(UserAccount::displayName)
            .filter(name -> name != null && !name.isBlank())
            .orElse("");
    }

    /** Lowercased concatenation of fields searched by the keyword box. */
    private static String searchHaystack(JobPosting job, UiAppContext context) {
        String moName = moDisplayName(context, job.organiserId());
        return String.join(
            " ",
            job.jobId(),
            job.organiserId(),
            moName,
            job.title(),
            job.moduleOrActivity(),
            job.description(),
            String.join(" ", job.requiredSkills()),
            String.join(" ", job.scheduleSlots())
        ).toLowerCase(Locale.ROOT);
    }

    /** Substring match over {@link #searchHaystack(JobPosting, UiAppContext)}; blank keyword matches all. */
    private static boolean matchesKeyword(JobPosting job, UiAppContext context, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return searchHaystack(job, context).contains(keyword.trim().toLowerCase(Locale.ROOT));
    }

    /** Exact match on {@link JobPosting#moduleOrActivity()} (case-insensitive), or pass-through for "All modules". */
    private static boolean matchesModule(JobPosting job, String moduleChoice) {
        if (moduleChoice == null || "All modules".equals(moduleChoice)) {
            return true;
        }
        return job.moduleOrActivity() != null && job.moduleOrActivity().equalsIgnoreCase(moduleChoice.trim());
    }

    /** Heuristic: scan title + description for keywords per selected activity bucket. */
    private static boolean matchesActivityType(JobPosting job, String activityChoice) {
        if (activityChoice == null || "All activity types".equals(activityChoice)) {
            return true;
        }
        String blob = (job.title() + " " + job.description()).toLowerCase(Locale.ROOT);
        return switch (activityChoice) {
            case "Lab session" -> blob.contains("lab");
            case "Tutorial" -> blob.contains("tutorial");
            case "Assignment / marking" ->
                blob.contains("assignment") || blob.contains("marking") || blob.contains("grading");
            case "Project / development" ->
                blob.contains("project") || blob.contains("development") || blob.contains("studio");
            default -> true;
        };
    }

    /** Case-insensitive equality against one entry in {@link JobPosting#requiredSkills()}. */
    private static boolean matchesSkill(JobPosting job, String skillChoice) {
        if (skillChoice == null || "All skills".equals(skillChoice)) {
            return true;
        }
        String target = skillChoice.trim().toLowerCase(Locale.ROOT);
        return job.requiredSkills().stream()
            .anyMatch(s -> s != null && s.trim().toLowerCase(Locale.ROOT).equals(target));
    }

    /**
     * Day codes match {@code MON-}… prefix in schedule strings; morning/afternoon/evening use
     * {@link #slotStartHour(String)}. Jobs with no schedule never match a specific slot filter.
     */
    private static boolean matchesTimeSlot(JobPosting job, String timeChoice) {
        if (timeChoice == null || "Any time".equals(timeChoice)) {
            return true;
        }
        List<String> slots = job.scheduleSlots();
        if (slots.isEmpty()) {
            return false;
        }
        if (List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").contains(timeChoice)) {
            String prefix = timeChoice + "-";
            return slots.stream().anyMatch(s -> s != null && s.toUpperCase(Locale.ROOT).startsWith(prefix));
        }
        if ("Morning".equals(timeChoice)) {
            return slots.stream().anyMatch(MoreJobsPage::slotStartsInMorning);
        }
        if ("Afternoon".equals(timeChoice)) {
            return slots.stream().anyMatch(MoreJobsPage::slotStartsInAfternoon);
        }
        if ("Evening".equals(timeChoice)) {
            return slots.stream().anyMatch(MoreJobsPage::slotStartsInEvening);
        }
        return true;
    }

    /**
     * Expects slots like {@code MON-09:00-11:00}; returns start hour (0–23) or {@code -1} if unparsable.
     */
    private static int slotStartHour(String slot) {
        if (slot == null || slot.isBlank()) {
            return -1;
        }
        int first = slot.indexOf('-');
        if (first < 0) {
            return -1;
        }
        String rest = slot.substring(first + 1);
        int second = rest.indexOf('-');
        if (second < 0) {
            return -1;
        }
        String start = rest.substring(0, second);
        String[] parts = start.split(":");
        if (parts.length < 1) {
            return -1;
        }
        try {
            return Integer.parseInt(parts[0].trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    /**
     * Time-of-day filters for {@link #matchesTimeSlot(JobPosting, String)}: start hour from
     * {@link #slotStartHour(String)} in morning {@code [5, 12)}, afternoon {@code [12, 17)}, evening {@code [17, 24]}.
     */
    private static boolean slotStartsInMorning(String slot) {
        int h = slotStartHour(slot);
        return h >= 5 && h < 12;
    }

    private static boolean slotStartsInAfternoon(String slot) {
        int h = slotStartHour(slot);
        return h >= 12 && h < 17;
    }

    private static boolean slotStartsInEvening(String slot) {
        int h = slotStartHour(slot);
        return h >= 17 && h <= 23;
    }
}
