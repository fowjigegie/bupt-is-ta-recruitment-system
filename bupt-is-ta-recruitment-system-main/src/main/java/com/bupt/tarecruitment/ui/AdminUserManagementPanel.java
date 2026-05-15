package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.auth.AccountStatus;
import com.bupt.tarecruitment.auth.UserAccount;
import com.bupt.tarecruitment.auth.UserRole;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.job.JobPosting;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.Node;
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
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Admin-facing user list with role filtering, account status control, and related data summaries.
 */
final class AdminUserManagementPanel {
    private static final String ALL_ROLES = "All roles";
    private static final int MAX_VISIBLE_USER_ROWS = 8;
    private static final double USER_ROW_HEIGHT = 76;
    private static final double USER_ROW_GAP = 10;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UiAppContext context;
    private final VBox container;
    private final ComboBox<String> roleFilter;
    private final VBox userList;
    private final ScrollPane userScroll;
    private final Label summaryLabel;

    private AdminUserManagementPanel(
        UiAppContext context,
        VBox container,
        ComboBox<String> roleFilter,
        VBox userList,
        ScrollPane userScroll,
        Label summaryLabel
    ) {
        this.context = context;
        this.container = container;
        this.roleFilter = roleFilter;
        this.userList = userList;
        this.userScroll = userScroll;
        this.summaryLabel = summaryLabel;
    }

    static AdminUserManagementPanel create(UiAppContext context) {
        ComboBox<String> roleFilter = createRoleFilter();
        Label summaryLabel = UiTheme.createMutedText("");
        summaryLabel.setStyle("-fx-text-fill: #2f3553;");

        VBox userList = new VBox(USER_ROW_GAP);
        userList.setFillWidth(true);
        ScrollPane userScroll = createUserScrollPane(userList);

        VBox panel = new VBox(14);
        panel.setPadding(new Insets(22));
        panel.setMaxWidth(Double.MAX_VALUE);
        panel.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(24), Insets.EMPTY)));
        panel.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(24),
            new BorderWidths(1.6)
        )));

        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = UiTheme.createSectionTitle("User management");
        title.setStyle("-fx-text-fill: #2f3553;");
        Label subtitle = UiTheme.createMutedText("View users, filter by role, check profile readiness, and enable or disable non-admin accounts.");
        subtitle.setStyle("-fx-text-fill: #2f3553;");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(titleBox, spacer, roleFilter);
        panel.getChildren().addAll(header, summaryLabel, userScroll);

        AdminUserManagementPanel userManagementPanel = new AdminUserManagementPanel(
            context,
            panel,
            roleFilter,
            userList,
            userScroll,
            summaryLabel
        );
        roleFilter.valueProperty().addListener((obs, oldValue, newValue) -> userManagementPanel.refresh());
        userManagementPanel.refresh();
        return userManagementPanel;
    }

    VBox container() {
        return container;
    }

    private void refresh() {
        List<UserAccount> users = filteredUsers();
        userList.getChildren().clear();

        long activeCount = users.stream().filter(user -> user.status() == AccountStatus.ACTIVE).count();
        int visibleRows = Math.max(1, Math.min(MAX_VISIBLE_USER_ROWS, users.size()));
        userScroll.setPrefViewportHeight(visibleRows * USER_ROW_HEIGHT + Math.max(0, visibleRows - 1) * USER_ROW_GAP);
        summaryLabel.setText(
            "Showing "
                + users.size()
                + " users. Active: "
                + activeCount
                + ", Disabled: "
                + (users.size() - activeCount)
                + "."
        );

        if (users.isEmpty()) {
            userList.getChildren().add(UiTheme.createWhiteCard("No users", "No users match the selected role filter."));
            return;
        }

        for (UserAccount user : users) {
            userList.getChildren().add(createUserRow(user));
        }
    }

    private List<UserAccount> filteredUsers() {
        String selectedRole = roleFilter.getValue();
        return context.services().userRepository().findAll().stream()
            .filter(user -> user.role() != UserRole.ADMIN)
            .filter(user -> selectedRole == null
                || ALL_ROLES.equals(selectedRole)
                || user.role().name().equals(selectedRole))
            .sorted(Comparator.comparing(UserAccount::role).thenComparing(UserAccount::userId))
            .toList();
    }

    private HBox createUserRow(UserAccount user) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMinHeight(USER_ROW_HEIGHT);
        row.setPadding(new Insets(10, 18, 10, 18));
        row.setMaxWidth(Double.MAX_VALUE);
        row.setBackground(new Background(new BackgroundFill(Color.web("#fffafd"), new CornerRadii(18), Insets.EMPTY)));
        row.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(18),
            new BorderWidths(1.2)
        )));

        VBox identity = new VBox(4);
        identity.setMinWidth(230);
        identity.setPrefWidth(280);
        Label nameLabel = new Label(user.displayName());
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        nameLabel.setTextFill(Color.web("#4664a8"));
        nameLabel.setStyle("-fx-text-fill: #4664a8;");
        Label idLabel = UiTheme.createMutedText(user.userId() + " | " + user.role().name());
        idLabel.setStyle("-fx-text-fill: #5c6481;");
        identity.getChildren().addAll(nameLabel, idLabel);

        HBox detailChips = new HBox(10);
        detailChips.setAlignment(Pos.CENTER_LEFT);
        detailChips.getChildren().addAll(
            createStatusChip(user.status()),
            createProfileChip(user),
            createActivityChip(user)
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button detailsButton = UiTheme.createOutlineButton("View details", 130, 42);
        detailsButton.setOnAction(event -> showDetails(user));
        Button statusButton = createStatusButton(user);
        actions.getChildren().addAll(detailsButton, statusButton);

        row.getChildren().addAll(identity, detailChips, spacer, actions);
        return row;
    }

    private Label createStatusChip(AccountStatus status) {
        boolean active = status == AccountStatus.ACTIVE;
        return createChip(
            active ? "Active" : "Disabled",
            active ? "#e8f5e9" : "#fff2f2",
            active ? "#2e7d32" : "#b00020",
            active ? "#9fd6a6" : "#f2a6a6"
        );
    }

    private Label createProfileChip(UserAccount user) {
        if (user.role() != UserRole.APPLICANT) {
            return createChip("Profile: N/A", "#f7f3ff", "#5d4c86", "#d7c5ff");
        }

        Optional<ApplicantProfile> profile = context.services().profileRepository().findByUserId(user.userId());
        boolean complete = profile.map(this::isProfileComplete).orElse(false);
        return createChip(
            complete ? "Profile complete" : "Profile missing",
            complete ? "#edf7ff" : "#fff6e5",
            complete ? "#2f5c9f" : "#9a6500",
            complete ? "#b8d5ff" : "#ffd58a"
        );
    }

    private Label createActivityChip(UserAccount user) {
        if (user.role() == UserRole.MO) {
            long postedJobs = context.services().jobRepository().findAll().stream()
                .filter(job -> job.organiserId().equals(user.userId()))
                .count();
            return createChip(
                "Posted jobs: " + postedJobs,
                "#f7f3ff",
                "#5d4c86",
                "#d7c5ff"
            );
        }

        if (user.role() == UserRole.APPLICANT) {
            long submittedApplications = context.services().applicationRepository().findAll().stream()
                .filter(application -> application.applicantUserId().equals(user.userId()))
                .count();
            return createChip(
                "Applications: " + submittedApplications,
                "#f7f3ff",
                "#5d4c86",
                "#d7c5ff"
            );
        }

        return createChip("System account", "#f7f3ff", "#5d4c86", "#d7c5ff");
    }

    private Button createStatusButton(UserAccount user) {
        boolean active = user.status() == AccountStatus.ACTIVE;
        Button button = UiTheme.createOutlineButton(active ? "Disable" : "Enable", 130, 42);
        button.setStyle(
            "-fx-background-color: " + (active ? "#fff2f2" : "#edf9ee") + ";" +
                "-fx-text-fill: " + (active ? "#b00020" : "#2e7d32") + ";" +
                "-fx-background-radius: 22;" +
                "-fx-border-color: " + (active ? "#f2a6a6" : "#9fd6a6") + ";" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 22;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;"
        );
        button.setOnAction(event -> {
            AccountStatus nextStatus = active ? AccountStatus.DISABLED : AccountStatus.ACTIVE;
            context.services().userRepository().save(new UserAccount(
                user.userId(),
                user.passwordHash(),
                user.role(),
                user.displayName(),
                nextStatus
            ));
            refresh();
        });
        return button;
    }

    private void showDetails(UserAccount user) {
        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #fffaf3;");

        content.getChildren().add(createDetailsHeader(user));

        if (user.role() == UserRole.APPLICANT) {
            addApplicantDetails(content, user);
        } else if (user.role() == UserRole.MO) {
            addMoDetails(content, user);
        }

        Button closeButton = UiTheme.createOutlineButton("Close", 120, 40);
        HBox footer = new HBox(closeButton);
        footer.setAlignment(Pos.CENTER_RIGHT);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        Stage dialog = new Stage();
        dialog.setTitle("User details - " + user.displayName());
        dialog.initModality(Modality.APPLICATION_MODAL);
        closeButton.setOnAction(event -> dialog.close());
        content.getChildren().add(footer);
        dialog.setScene(new Scene(scrollPane, 820, 620));
        dialog.showAndWait();
    }

    private void addApplicantDetails(VBox content, UserAccount user) {
        Optional<ApplicantProfile> profile = context.services().profileRepository().findByUserId(user.userId());
        List<JobApplication> applications = context.services().applicationRepository().findAll().stream()
            .filter(application -> application.applicantUserId().equals(user.userId()))
            .sorted(Comparator.comparing(JobApplication::submittedAt).reversed())
            .toList();

        int skillCount = profile.map(applicantProfile -> applicantProfile.skills().size()).orElse(0);
        int slotCount = profile.map(applicantProfile -> applicantProfile.availabilitySlots().size()).orElse(0);
        HBox metrics = createMetricRow(
            createMetric("Profile", profile.filter(this::isProfileComplete).isPresent() ? "Complete" : "Incomplete"),
            createMetric("Applications", Integer.toString(applications.size())),
            createMetric("Skills", Integer.toString(skillCount)),
            createMetric("Availability", slotCount + " slots")
        );
        content.getChildren().add(metrics);

        if (profile.isEmpty()) {
            content.getChildren().add(createDetailCard(
                "Profile summary",
                createDetailLine("No applicant profile has been created yet.")
            ));
        } else {
            ApplicantProfile applicantProfile = profile.get();
            content.getChildren().add(createDetailCard(
                "Profile summary",
                createKeyValue("Full name", applicantProfile.fullName()),
                createKeyValue("Student ID", applicantProfile.studentId()),
                createKeyValue("Programme", applicantProfile.programme()),
                createKeyValue("Year", Integer.toString(applicantProfile.yearOfStudy())),
                createKeyValue("Education level", applicantProfile.educationLevel()),
                createKeyValue("Skills", joinOrEmpty(applicantProfile.skills())),
                createKeyValue("Desired positions", joinOrEmpty(applicantProfile.desiredPositions())),
                createKeyValue("Availability", applicantProfile.availabilitySlots().size() + " selected teaching slots")
            ));
        }

        VBox applicationList = new VBox(10);
        if (applications.isEmpty()) {
            applicationList.getChildren().add(createDetailLine("No applications submitted."));
        } else {
            for (JobApplication application : applications) {
                applicationList.getChildren().add(createApplicationCard(application));
            }
        }
        content.getChildren().add(createDetailCard("Application history", applicationList));
    }

    private void addMoDetails(VBox content, UserAccount user) {
        List<JobPosting> jobs = context.services().jobRepository().findAll().stream()
            .filter(job -> job.organiserId().equals(user.userId()))
            .sorted(Comparator.comparing(JobPosting::jobId))
            .toList();
        long openJobs = jobs.stream().filter(job -> "OPEN".equals(job.status().name())).count();
        long totalApplications = context.services().applicationRepository().findAll().stream()
            .filter(application -> jobs.stream().anyMatch(job -> job.jobId().equals(application.jobId())))
            .count();

        content.getChildren().add(createMetricRow(
            createMetric("Posted jobs", Integer.toString(jobs.size())),
            createMetric("Open jobs", Long.toString(openJobs)),
            createMetric("Applications", Long.toString(totalApplications))
        ));

        VBox jobList = new VBox(10);
        if (jobs.isEmpty()) {
            jobList.getChildren().add(createDetailLine("No jobs posted."));
        } else {
            for (JobPosting job : jobs) {
                jobList.getChildren().add(createJobCard(job));
            }
        }
        content.getChildren().add(createDetailCard("Posted jobs", jobList));
    }

    private boolean isProfileComplete(ApplicantProfile profile) {
        return !profile.fullName().isBlank()
            && !profile.studentId().isBlank()
            && !profile.programme().isBlank()
            && !profile.skills().isEmpty()
            && !profile.availabilitySlots().isEmpty()
            && !profile.desiredPositions().isEmpty();
    }

    private static ComboBox<String> createRoleFilter() {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(ALL_ROLES, UserRole.MO.name(), UserRole.APPLICANT.name());
        comboBox.getSelectionModel().selectFirst();
        comboBox.setPrefWidth(180);
        comboBox.setPrefHeight(42);
        comboBox.setStyle(
            "-fx-background-color: white;" +
                "-fx-background-radius: 22;" +
                "-fx-border-color: #f1c7da;" +
                "-fx-border-radius: 22;" +
                "-fx-border-width: 1.5;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 14px;"
        );
        return comboBox;
    }

    private static ScrollPane createUserScrollPane(VBox userList) {
        ScrollPane scrollPane = new ScrollPane(userList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle(
            "-fx-background-color: transparent;" +
                "-fx-background: transparent;" +
                "-fx-border-color: transparent;"
        );
        return scrollPane;
    }

    private HBox createDetailsHeader(UserAccount user) {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20));
        header.setMaxWidth(Double.MAX_VALUE);
        header.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(24), Insets.EMPTY)));
        header.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(24),
            new BorderWidths(1.5)
        )));

        VBox identity = new VBox(6);
        Label title = new Label(user.displayName());
        title.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        title.setStyle("-fx-text-fill: #4664a8;");
        Label subtitle = createDetailLine(user.userId() + " | " + user.role().name());
        identity.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox chips = new HBox(8, createStatusChip(user.status()), createChip(
            user.role() == UserRole.APPLICANT ? "Applicant account" : "MO account",
            "#f7f3ff",
            "#5d4c86",
            "#d7c5ff"
        ));
        chips.setAlignment(Pos.CENTER_RIGHT);

        header.getChildren().addAll(identity, spacer, chips);
        return header;
    }

    private HBox createMetricRow(VBox... metrics) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        for (VBox metric : metrics) {
            HBox.setHgrow(metric, Priority.ALWAYS);
            row.getChildren().add(metric);
        }
        return row;
    }

    private VBox createMetric(String title, String value) {
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        titleLabel.setStyle("-fx-text-fill: #5c6481;");

        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 21));
        valueLabel.setStyle("-fx-text-fill: #2f3553;");

        VBox metric = new VBox(4, titleLabel, valueLabel);
        metric.setPadding(new Insets(14, 16, 14, 16));
        metric.setMaxWidth(Double.MAX_VALUE);
        metric.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(18), Insets.EMPTY)));
        metric.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(18),
            new BorderWidths(1.3)
        )));
        return metric;
    }

    private VBox createDetailCard(String title, Node... children) {
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        titleLabel.setStyle("-fx-text-fill: #4664a8;");

        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(22), Insets.EMPTY)));
        card.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(22),
            new BorderWidths(1.5)
        )));
        card.getChildren().add(titleLabel);
        card.getChildren().addAll(children);
        return card;
    }

    private HBox createKeyValue(String key, String value) {
        Label keyLabel = new Label(key);
        keyLabel.setMinWidth(130);
        keyLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        keyLabel.setStyle("-fx-text-fill: #5c6481;");

        Label valueLabel = createDetailLine(value);
        HBox.setHgrow(valueLabel, Priority.ALWAYS);

        HBox row = new HBox(10, keyLabel, valueLabel);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private VBox createApplicationCard(JobApplication application) {
        JobPosting job = findJobById(application.jobId()).orElse(null);
        String jobTitle = job == null ? "Unknown job" : job.title();
        String jobMeta = job == null ? application.jobId() : job.moduleOrActivity() + " | " + application.jobId();

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(jobTitle);
        title.setFont(Font.font("Arial", FontWeight.BOLD, 17));
        title.setStyle("-fx-text-fill: #4664a8;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        top.getChildren().addAll(title, spacer, createApplicationStatusChip(application.status().name()));

        VBox card = createListItemCard();
        card.getChildren().addAll(
            top,
            createDetailLine(jobMeta),
            createDetailLine("CV: " + application.cvId() + " | Submitted: " + DATE_TIME_FORMATTER.format(application.submittedAt()))
        );
        if (!application.reviewerNote().isBlank()) {
            card.getChildren().add(createDetailLine("Reviewer note: " + application.reviewerNote()));
        }
        return card;
    }

    private VBox createJobCard(JobPosting job) {
        long applicationCount = context.services().applicationRepository().findAll().stream()
            .filter(application -> application.jobId().equals(job.jobId()))
            .count();

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(job.title());
        title.setFont(Font.font("Arial", FontWeight.BOLD, 17));
        title.setStyle("-fx-text-fill: #4664a8;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        top.getChildren().addAll(title, spacer, createJobStatusChip(job.status().name()));

        VBox card = createListItemCard();
        card.getChildren().addAll(
            top,
            createDetailLine(job.jobId() + " | " + job.moduleOrActivity() + " | " + job.activityType()),
            createDetailLine("Applications: " + applicationCount + " | Weekly hours: " + job.weeklyHours()),
            createDetailLine("Required skills: " + joinOrEmpty(job.requiredSkills())),
            createDetailLine("Schedule slots: " + job.scheduleSlots().size())
        );
        return card;
    }

    private VBox createListItemCard() {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setBackground(new Background(new BackgroundFill(Color.web("#fffafd"), new CornerRadii(16), Insets.EMPTY)));
        card.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(16),
            new BorderWidths(1.1)
        )));
        return card;
    }

    private Optional<JobPosting> findJobById(String jobId) {
        return context.services().jobRepository().findAll().stream()
            .filter(job -> job.jobId().equals(jobId))
            .findFirst();
    }

    private static Label createApplicationStatusChip(String status) {
        return switch (status) {
            case "ACCEPTED" -> createChip("Accepted", "#e8f5e9", "#2e7d32", "#9fd6a6");
            case "REJECTED" -> createChip("Rejected", "#fff2f2", "#b00020", "#f2a6a6");
            case "SHORTLISTED" -> createChip("Shortlisted", "#edf7ff", "#2f5c9f", "#b8d5ff");
            case "WITHDRAWN" -> createChip("Withdrawn", "#f3f3f3", "#5c6481", "#d8d8d8");
            default -> createChip("Submitted", "#fff6e5", "#9a6500", "#ffd58a");
        };
    }

    private static Label createJobStatusChip(String status) {
        return switch (status) {
            case "OPEN" -> createChip("Open", "#e8f5e9", "#2e7d32", "#9fd6a6");
            case "CLOSED" -> createChip("Closed", "#f3f3f3", "#5c6481", "#d8d8d8");
            default -> createChip(status, "#fff6e5", "#9a6500", "#ffd58a");
        };
    }

    private static Label createDetailLine(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setFont(Font.font("Arial", 15));
        label.setTextFill(Color.web("#2f3553"));
        label.setStyle("-fx-text-fill: #2f3553;");
        return label;
    }

    private static String joinOrEmpty(List<String> values) {
        return values.isEmpty() ? "(none)" : String.join(", ", values);
    }

    private static Label createChip(String text, String background, String textColor, String borderColor) {
        Label chip = new Label(text);
        chip.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        chip.setTextFill(Color.web(textColor));
        chip.setPadding(new Insets(7, 12, 7, 12));
        chip.setStyle(
            "-fx-background-color: " + background + ";" +
                "-fx-text-fill: " + textColor + ";" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-radius: 16;" +
                "-fx-border-width: 1.2;"
        );
        return chip;
    }
}
