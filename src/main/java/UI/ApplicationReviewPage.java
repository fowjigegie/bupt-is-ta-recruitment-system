package UI;

import com.bupt.tarecruitment.applicant.ApplicantCvReview;
import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.application.ApplicationStatus;
import com.bupt.tarecruitment.application.ApplicationStatusPresenter;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.job.JobPosting;
import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApplicationReviewPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.APPLICATION_REVIEW, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        List<JobPosting> ownedJobs = loadOwnedJobs(context);

        VBox content = new VBox(18);
        content.setPadding(new Insets(35, 40, 28, 40));

        Label heading = UiTheme.createPageHeading("Application review");
        heading.setStyle("-fx-text-fill: #4664a8;");

        Label subHeading = new Label("Select a job to review applicants. CV details stay on this page.");
        subHeading.setStyle(
            "-fx-font-family: Arial;" +
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4664a8;"
        );

        content.getChildren().addAll(heading, subHeading);

        if (ownedJobs.isEmpty()) {
            content.getChildren().add(UiTheme.createWhiteCard(
                "No jobs",
                "You don't have any jobs yet. Go to Post Vacancies to create one."
            ));
            content.getChildren().add(new HBox(UiTheme.createBackButton(nav)));
        } else {
            ComboBox<JobPosting> jobBox = new ComboBox<>();
            jobBox.setPrefWidth(520);
            jobBox.setPrefHeight(42);
            jobBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(JobPosting job) {
                    if (job == null) {
                        return "";
                    }
                    // Format: job001|TA for Software Engineering|EBU6304
                    return job.jobId()
                        + "|" + job.title()
                        + "|" + job.moduleOrActivity();
                }

                @Override
                public JobPosting fromString(String string) {
                    return null;
                }
            });
            jobBox.getItems().addAll(ownedJobs);

            JobPosting initialJob = ownedJobs.stream()
                .filter(job -> job.jobId().equals(context.selectedJobId()))
                .findFirst()
                .orElse(ownedJobs.getFirst());
            jobBox.getSelectionModel().select(initialJob);

            VBox applicantListBox = new VBox(0);
            applicantListBox.setPadding(new Insets(6, 2, 6, 2));

            ScrollPane listScroll = new ScrollPane(applicantListBox);
            listScroll.setFitToWidth(true);
            listScroll.setPrefViewportHeight(560);
            listScroll.setStyle(
                "-fx-background:#faf9fc;" +
                    "-fx-background-color:#faf9fc;" +
                    "-fx-border-color:transparent;" +
                    "-fx-background-radius:14;"
            );

            Label detailTitle = new Label("Select an applicant");
            detailTitle.setStyle(
                "-fx-font-family: Arial;" +
                    "-fx-font-size: 18px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: #4664a8;" +
                    "-fx-background-color: #fff2bf;" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 4 10 4 10;"
            );

            VBox detailContent = new VBox(14);
            detailContent.setPadding(new Insets(4, 2, 4, 2));
            detailContent.getChildren().add(UiTheme.createMutedText("Click \"View Details\" to load full CV here."));

            ScrollPane detailScroll = new ScrollPane(detailContent);
            detailScroll.setFitToWidth(true);
            detailScroll.setPrefViewportHeight(560);
            detailScroll.setStyle(
                "-fx-background:#ffffff;" +
                    "-fx-background-color:#ffffff;" +
                    "-fx-border-color:#f0d9e9;" +
                    "-fx-border-radius:12;" +
                    "-fx-background-radius:12;"
            );

            Label actionStatus = UiTheme.createMutedText("");
            actionStatus.setTextFill(Color.web("#b00020"));
            ObjectProperty<JobApplication> selectedApplication = new SimpleObjectProperty<>(null);

            Runnable[] refreshListRef = new Runnable[1];
            refreshListRef[0] = () -> {
                applicantListBox.getChildren().clear();
                JobPosting selectedJob = jobBox.getValue();
                if (selectedJob == null) {
                    return;
                }

                List<JobApplication> applications = loadApplicationsForJob(context, selectedJob.jobId());
                if (applications.isEmpty()) {
                    applicantListBox.getChildren().add(UiTheme.createWhiteCard(
                        "No applications",
                        "There are no applications for this job yet."
                    ));
                    detailTitle.setText("Select an applicant");
                    detailContent.getChildren().setAll(UiTheme.createMutedText("No CV details to display."));
                    selectedApplication.set(null);
                    return;
                }

                if (selectedApplication.get() == null
                    || applications.stream().noneMatch(app -> app.applicationId().equals(selectedApplication.get().applicationId()))) {
                    selectedApplication.set(applications.getFirst());
                }

                for (JobApplication application : applications) {
                    applicantListBox.getChildren().add(createApplicantRow(
                        context,
                        application,
                        selectedApplication.get() != null
                            && application.applicationId().equals(selectedApplication.get().applicationId()),
                        selected -> {
                            selectedApplication.set(selected);
                            renderSelectedDetail(context, selected, detailTitle, detailContent, actionStatus, nav, refreshListRef[0]);
                        }
                    ));
                }

                if (selectedApplication.get() != null) {
                    renderSelectedDetail(context, selectedApplication.get(), detailTitle, detailContent, actionStatus, nav, refreshListRef[0]);
                }
            };

            jobBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshListRef[0].run());
            refreshListRef[0].run();

            VBox leftPane = new VBox(10);
            Label applicantsLabel = new Label("Applicants");
            applicantsLabel.setStyle(
                "-fx-font-family: Arial;" +
                    "-fx-font-size: 24px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: #4664a8;"
            );
            leftPane.getChildren().addAll(applicantsLabel, listScroll);
            leftPane.setPadding(new Insets(14));
            leftPane.setBackground(new Background(new BackgroundFill(Color.web("#fdfcff"), new CornerRadii(18), Insets.EMPTY)));
            leftPane.setBorder(new Border(new BorderStroke(
                Color.web("#f0d9e9"), BorderStrokeStyle.SOLID, new CornerRadii(18), new BorderWidths(1.2)
            )));
            leftPane.setPrefWidth(460);

            VBox rightPane = new VBox(12,
                detailTitle,
                detailScroll,
                actionStatus
            );
            rightPane.setPadding(new Insets(14));
            rightPane.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(18), Insets.EMPTY)));
            rightPane.setBorder(new Border(new BorderStroke(
                Color.web("#f0d9e9"), BorderStrokeStyle.SOLID, new CornerRadii(18), new BorderWidths(1.2)
            )));
            HBox.setHgrow(rightPane, Priority.ALWAYS);

            HBox contentRow = new HBox(14, leftPane, rightPane);

            VBox footer = new VBox(new HBox(UiTheme.createBackButton(nav)));
            footer.setAlignment(Pos.CENTER_LEFT);

            content.getChildren().addAll(jobBox, contentRow, footer);
        }

        ScrollPane pageScroll = new ScrollPane(content);
        pageScroll.setFitToWidth(true);
        pageScroll.setPannable(true);
        // Keep the same page background as other MO pages (UiTheme.pageBackground()).
        pageScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        BorderPane root = UiTheme.createPage(
            "Application Review",
            UiTheme.createMoSidebar(nav, PageId.APPLICATION_REVIEW),
            pageScroll,
            nav,
            context
        );
        return UiTheme.createScene(root);
    }

    private static List<JobPosting> loadOwnedJobs(UiAppContext context) {
        return context.services().jobRepository().findAll().stream()
            .filter(job -> job.organiserId().equals(context.session().userId()))
            .sorted(Comparator.comparing(JobPosting::jobId))
            .toList();
    }

    private static List<JobApplication> loadApplicationsForJob(UiAppContext context, String jobId) {
        return context.services().applicationRepository().findAll().stream()
            .filter(application -> application.jobId().equals(jobId))
            .sorted(Comparator.comparing(JobApplication::applicationId))
            .toList();
    }

    private static HBox createApplicantRow(
        UiAppContext context,
        JobApplication application,
        boolean selected,
        java.util.function.Consumer<JobApplication> onSelect
    ) {
        var profileOpt = context.services().profileRepository().findByUserId(application.applicantUserId());

        String applicantName = profileOpt.map(p -> p.fullName()).orElse(application.applicantUserId());
        String major = profileOpt.map(p -> p.programme()).orElse("-");
        String appliedDate = application.submittedAt().toLocalDate().toString();

        Label nameLabel = new Label(applicantName);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        nameLabel.setTextFill(Color.web("#4664a8"));

        Label majorLabel = new Label("Major: " + major);
        majorLabel.setFont(Font.font("Arial", 16));
        majorLabel.setTextFill(Color.web("#2f2f2f"));

        Label dateLabel = new Label("Applied: " + appliedDate);
        dateLabel.setFont(Font.font("Arial", 16));
        dateLabel.setTextFill(Color.web("#2f2f2f"));

        Label statusLabel = new Label(ApplicationStatusPresenter.toDisplayText(application.status()));
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        statusLabel.setTextFill(Color.web("#4664a8"));
        statusLabel.setPadding(new Insets(4, 10, 4, 10));
        statusLabel.setStyle("-fx-background-color: " + statusColor(application.status()) + "; -fx-background-radius: 10;");

        Button detailsButton = UiTheme.createSoftButton("View", 80, 42);
        detailsButton.setOnAction(event -> onSelect.accept(application));

        Region spacer1 = new Region();
        Region spacer2 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        HBox row = new HBox(16, nameLabel, spacer1, statusLabel, detailsButton);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 16, 14, 16));
        row.setMaxWidth(Double.MAX_VALUE);
        row.setStyle(
            (selected ? "-fx-background-color: #fdf4fb;" : "-fx-background-color: transparent;")
                + "-fx-border-color: #f4d9e6; -fx-border-width: 0 0 2 0;"
        );
        return row;
    }

    private static void renderSelectedDetail(
        UiAppContext context,
        JobApplication application,
        Label detailTitle,
        VBox detailContent,
        Label actionStatus,
        NavigationManager nav,
        Runnable refreshList
    ) {
        try {
            ApplicantCvReview review = context.services().cvReviewService().loadReviewByApplicationId(application.applicationId());
            detailTitle.setText("CV | " + review.cv().cvId() + " | " + review.profile().fullName());
            detailTitle.setTextFill(Color.web("#4664a8"));
            detailContent.getChildren().setAll(buildCvDetailPanel(context, review, actionStatus, nav, refreshList));
            actionStatus.setText("");
        } catch (Exception exception) {
            detailTitle.setText("Applicant details");
            detailContent.getChildren().setAll(UiTheme.createWhiteCard("Failed to load CV", exception.getMessage()));
            actionStatus.setText(exception.getMessage());
            actionStatus.setTextFill(Color.web("#b00020"));
        }
    }

    private static VBox buildCvDetailPanel(
        UiAppContext context,
        ApplicantCvReview review,
        Label actionStatus,
        NavigationManager nav,
        Runnable refreshList
    ) {
        ApplicantProfile profile = review.profile();
        Map<String, String> parsed = parseCvContent(review.cvContent());

        TextField nameField = createReadonlyRoundedField("Full Name", valueOrFallback(parsed.get("Name"), profile.fullName()), 280);
        TextField gradeField = createReadonlyRoundedField(
            "Grade",
            valueOrFallback(parsed.get("Grade"), "%d-year %s".formatted(profile.yearOfStudy(), profile.educationLevel().toLowerCase())),
            280
        );
        TextField programmeField = createReadonlyRoundedField("Programme", valueOrFallback(parsed.get("Programme"), profile.programme()), 280);
        TextField studentIdField = createReadonlyRoundedField("Student ID", valueOrFallback(parsed.get("Student ID"), profile.studentId()), 280);
        TextField availabilityField = createReadonlyRoundedField(
            "Availability",
            valueOrFallback(parsed.get("Availability"), String.join("; ", profile.availabilitySlots())),
            572
        );
        TextField titleField = createReadonlyRoundedField("CV Title", review.cv().title(), 572);

        TextArea cvTextArea = createReadonlyLargeArea(valueOrFallback(parsed.get("CV Text"), review.cvContent()), 170);
        TextArea skillsArea = createReadonlyLargeArea(
            valueOrFallback(parsed.get("Skills"), String.join(System.lineSeparator(), profile.skills()).replace(", ", System.lineSeparator())),
            120
        );
        TextArea positionsArea = createReadonlyLargeArea(
            valueOrFallback(
                parsed.get("Desired Positions"),
                String.join(System.lineSeparator(), profile.desiredPositions()).replace(", ", System.lineSeparator())
            ),
            120
        );

        Label statusTag = new Label("Current Status: " + ApplicationStatusPresenter.toDisplayText(review.application().status()));
        statusTag.setStyle(
            "-fx-background-color: " + statusColor(review.application().status()) + ";" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 12;" +
                "-fx-padding: 6 12 6 12;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;"
        );

        Button chatButton = UiTheme.createOutlineButton("Chat", 160, 44);
        chatButton.setOnAction(event -> {
            context.openChatContext(review.application().jobId(), review.application().applicantUserId());
            nav.goTo(PageId.MESSAGES);
        });

        Button acceptButton = UiTheme.createPrimaryButton("Accept", 160, 44);
        boolean actionable = review.application().status() == ApplicationStatus.SUBMITTED;
        acceptButton.setDisable(!actionable);
        acceptButton.setOnAction(event -> {
            try {
                context.services().applicationDecisionService().updateStatus(
                    context.session().userId(),
                    review.application().applicationId(),
                    ApplicationStatus.ACCEPTED,
                    "Accepted by MO in Application Review page."
                );
                actionStatus.setTextFill(Color.web("#2e7d32"));
                actionStatus.setText("Application accepted.");
                refreshList.run();
            } catch (IllegalArgumentException exception) {
                actionStatus.setTextFill(Color.web("#b00020"));
                actionStatus.setText(exception.getMessage());
            }
        });

        Button withdrawnButton = UiTheme.createSoftButton("Withdrawn", 160, 44);
        withdrawnButton.setDisable(!actionable);
        withdrawnButton.setOnAction(event -> {
            try {
                JobApplication current = context.services().applicationRepository()
                    .findByApplicationId(review.application().applicationId())
                    .orElseThrow(() -> new IllegalArgumentException("Application not found."));
                JobApplication updated = new JobApplication(
                    current.applicationId(),
                    current.jobId(),
                    current.applicantUserId(),
                    current.cvId(),
                    ApplicationStatus.WITHDRAWN,
                    current.submittedAt(),
                    "Marked as withdrawn by MO."
                );
                context.services().applicationRepository().save(updated);
                actionStatus.setTextFill(Color.web("#2e7d32"));
                actionStatus.setText("Application updated to withdrawn.");
                refreshList.run();
            } catch (IllegalArgumentException exception) {
                actionStatus.setTextFill(Color.web("#b00020"));
                actionStatus.setText(exception.getMessage());
            }
        });

        HBox actionRow = new HBox(12, chatButton, acceptButton, withdrawnButton);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        HBox row1 = new HBox(12, nameField, gradeField);
        HBox row2 = new HBox(12, programmeField, studentIdField);

        VBox form = new VBox(12,
            row1,
            row2,
            availabilityField,
            titleField,
            createLabeledBlock("CV Text", cvTextArea),
            createLabeledBlock("Skills", skillsArea),
            createLabeledBlock("Desired Positions", positionsArea),
            statusTag,
            actionRow
        );
        form.setPadding(new Insets(4, 6, 12, 6));
        return form;
    }

    private static String valueOrFallback(String first, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return fallback == null ? "" : fallback;
    }

    private static TextField createReadonlyRoundedField(String prompt, String value, double width) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setText(value == null ? "" : value);
        field.setEditable(false);
        field.setPrefWidth(width);
        field.setPrefHeight(44);
        field.setFont(Font.font("Arial", 15));
        field.setStyle(
            "-fx-background-color: #eba8df;" +
                "-fx-background-radius: 20;" +
                "-fx-border-radius: 20;" +
                "-fx-border-color: transparent;" +
                "-fx-prompt-text-fill: white;" +
                "-fx-text-fill: white;" +
                "-fx-padding: 0 16 0 16;"
        );
        return field;
    }

    private static VBox createLabeledBlock(String title, TextArea area) {
        Label label = new Label(title);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 17));
        label.setTextFill(Color.web("#4664a8"));
        return new VBox(6, label, area);
    }

    private static TextArea createReadonlyLargeArea(String value, double prefHeight) {
        TextArea area = new TextArea(value == null ? "" : value);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefHeight(prefHeight);
        area.setStyle(
            "-fx-control-inner-background: white;" +
                "-fx-background-color: white;" +
                "-fx-background-radius: 0;" +
                "-fx-border-color: #f3b2df;" +
                "-fx-border-width: 3;" +
                "-fx-padding: 12;"
        );
        return area;
    }

    private static String statusColor(ApplicationStatus status) {
        return switch (status) {
            case SUBMITTED -> "#5b84d6";
            case SHORTLISTED -> "#f59e0b";
            case ACCEPTED -> "#2e7d32";
            case REJECTED -> "#e53935";
            case WITHDRAWN -> "#9ca3af";
        };
    }

    private static Map<String, String> parseCvContent(String content) {
        Map<String, String> result = new LinkedHashMap<>();
        if (content == null || content.isBlank()) {
            return result;
        }

        StringBuilder cvBody = new StringBuilder();
        for (String line : content.split("\\R")) {
            int separatorIndex = line.indexOf(": ");
            if (separatorIndex > 0) {
                String key = line.substring(0, separatorIndex).trim();
                String value = line.substring(separatorIndex + 2).trim();
                if (isKnownCvKey(key)) {
                    result.put(key, value);
                    continue;
                }
            }
            cvBody.append(line).append(System.lineSeparator());
        }

        String bodyText = cvBody.toString().trim();
        if (!bodyText.isBlank()) {
            result.putIfAbsent("CV Text", bodyText);
        }
        return result;
    }

    private static boolean isKnownCvKey(String key) {
        return "Name".equals(key)
            || "Grade".equals(key)
            || "Programme".equals(key)
            || "Student ID".equals(key)
            || "Availability".equals(key)
            || "Skills".equals(key)
            || "Desired Positions".equals(key);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
