package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.auth.UserRole;
import com.bupt.tarecruitment.common.schedule.ScheduleSlot;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.mo.MoInterviewScheduleService;
import com.bupt.tarecruitment.mo.MoInterviewScheduleService.InterviewConflictReport;
import com.bupt.tarecruitment.mo.MoInterviewScheduleService.InterviewMessageTemplate;
import com.bupt.tarecruitment.mo.MoInterviewScheduleService.InterviewTemplateVariables;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.Node;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * MO interview scheduling with templates; label colours use inline CSS so theme cannot wash them out.
 */
public class MoInterviewSchedulePage extends Application {
    private static final String DARK_TEXT = "-fx-text-fill: #111111 !important;";
    private static final DateTimeFormatter DATE_TEXT = DateTimeFormatter.ofPattern("yyyy-MM-dd (EEE)", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_TEXT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.MO_INTERVIEW_SCHEDULE, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        VBox pageBody = new VBox(16);
        pageBody.setPadding(new Insets(20, 40, 28, 40));
        pageBody.setMaxWidth(Double.MAX_VALUE);
        pageBody.setStyle(DARK_TEXT);

        Label heading = UiTheme.createPageHeading("Interview schedule");
        heading.setStyle("-fx-text-fill: #4664a8 !important;");

        String jobId = context.selectedJobId();
        String peerId = context.selectedChatPeerUserId();
        String organiserId = context.session().userId();

        if (!context.session().isAuthenticated() || context.session().role() != UserRole.MO) {
            pageBody.getChildren().addAll(
                heading,
                UiTheme.createWhiteCard("Not available", "Please sign in as a module organiser."),
                new HBox(UiTheme.createBackButton(nav))
            );
            return wrapInScrollAndScene(nav, context, pageBody);
        }

        if (jobId == null || peerId == null) {
            Label hint = new Label(
                "Open Messages, choose the applicant conversation for a job, then use \"Interview schedule\" again."
            );
            hint.setWrapText(true);
            hint.setFont(Font.font("Arial", 14));
            hint.setStyle(DARK_TEXT);
            pageBody.getChildren().addAll(
                heading,
                UiTheme.createWhiteCard("Select a conversation first", ""),
                hint,
                new HBox(UiTheme.createBackButton(nav))
            );
            return wrapInScrollAndScene(nav, context, pageBody);
        }

        JobPosting job = context.services().jobRepository().findByJobId(jobId).orElse(null);
        if (job == null || !job.organiserId().equals(organiserId)) {
            pageBody.getChildren().addAll(
                heading,
                UiTheme.createWhiteCard("Invalid job", "You can only schedule interviews for your own job posting."),
                new HBox(UiTheme.createBackButton(nav))
            );
            return wrapInScrollAndScene(nav, context, pageBody);
        }

        Label contextLine = new Label(
            "Conversation: "
                + context.formatUserLabel(peerId)
                + "  |  Job: "
                + job.title()
                + "  ("
                + job.jobId()
                + ")"
        );
        contextLine.setWrapText(true);
        contextLine.setFont(Font.font("Arial", 14));
        contextLine.setStyle(DARK_TEXT);

        Label helpLabel = new Label(
            "Times use 24-hour clock (HH:mm). Blocking checks: overlap with this job's TA hours, "
                + "and with this applicant's accepted TA assignments elsewhere. "
                + "Profile availability is advisory (warning only)."
        );
        helpLabel.setWrapText(true);
        helpLabel.setFont(Font.font("Arial", 13));
        helpLabel.setStyle(DARK_TEXT);

        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setPrefWidth(220);

        TextField startTimeField = new TextField("09:30");
        startTimeField.setPromptText("Start e.g. 09:30");
        startTimeField.setPrefWidth(120);

        TextField endTimeField = new TextField("10:15");
        endTimeField.setPromptText("End e.g. 10:15");
        endTimeField.setPrefWidth(120);

        TextField locationField = new TextField();
        locationField.setPromptText("Room, building, or online meeting link");
        locationField.setPrefWidth(480);

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Optional notes for the applicant (parking, what to bring, …)");
        notesArea.setPrefRowCount(3);
        notesArea.setWrapText(true);
        notesArea.setStyle(
            "-fx-control-inner-background: #ffffff;"
                + "-fx-text-fill: #111111 !important;"
                + "-fx-prompt-text-fill: #666666;"
        );

        ComboBox<InterviewMessageTemplate> templateBox = new ComboBox<>();
        templateBox.getItems().addAll(MoInterviewScheduleService.defaultTemplates());
        templateBox.getSelectionModel().selectFirst();
        templateBox.setPrefWidth(520);
        templateBox.setStyle(DARK_TEXT);
        templateBox.setButtonCell(interviewTemplateListCell());
        templateBox.setCellFactory(lv -> interviewTemplateListCell());

        TextArea previewArea = new TextArea();
        previewArea.setEditable(false);
        previewArea.setWrapText(true);
        previewArea.setPrefRowCount(12);
        previewArea.setStyle(
            "-fx-control-inner-background: #ffffff;"
                + "-fx-text-fill: #111111 !important;"
                + "-fx-prompt-text-fill: #666666;"
        );

        Label conflictLabel = new Label();
        conflictLabel.setWrapText(true);
        conflictLabel.setFont(Font.font("Arial", 13));
        conflictLabel.setStyle(DARK_TEXT);

        Runnable refreshPreview = () -> {
            try {
                LocalDate date = datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now();
                LocalTime start = parseTimeFlexible(startTimeField.getText());
                LocalTime end = parseTimeFlexible(endTimeField.getText());
                InterviewTemplateVariables vars = buildVariables(
                    context,
                    organiserId,
                    peerId,
                    job,
                    date,
                    start,
                    end,
                    locationField.getText(),
                    notesArea.getText()
                );
                InterviewMessageTemplate tpl = templateBox.getSelectionModel().getSelectedItem();
                String body = tpl == null ? "" : context.services().moInterviewScheduleService()
                    .renderMessage(tpl.body(), vars);
                previewArea.setText(body);
            } catch (RuntimeException exception) {
                previewArea.setText("(Fix date/time to update preview.)\n" + exception.getMessage());
            }
        };

        datePicker.valueProperty().addListener((o, a, b) -> refreshPreview.run());
        startTimeField.textProperty().addListener((o, a, b) -> refreshPreview.run());
        endTimeField.textProperty().addListener((o, a, b) -> refreshPreview.run());
        locationField.textProperty().addListener((o, a, b) -> refreshPreview.run());
        notesArea.textProperty().addListener((o, a, b) -> refreshPreview.run());
        templateBox.valueProperty().addListener((o, a, b) -> refreshPreview.run());

        Button checkButton = UiTheme.createSoftButton("Run conflict check", 200, 44);
        Button confirmButton = new Button("Confirm & send to TA");
        confirmButton.setWrapText(true);
        confirmButton.setTextAlignment(TextAlignment.CENTER);
        confirmButton.setMinHeight(48);
        confirmButton.setPrefHeight(48);
        confirmButton.setMaxHeight(48);
        confirmButton.setMaxWidth(280);
        confirmButton.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        String confirmStyle =
            "-fx-background-color: linear-gradient(to right, #ffd699, #ffb3d9);"
                + "-fx-text-fill: #1e3a5f !important;"
                + "-fx-background-radius: 24;"
                + "-fx-border-radius: 24;"
                + "-fx-border-color: transparent;"
                + "-fx-background-insets: 0;"
                + "-fx-padding: 12 20 12 20;"
                + "-fx-cursor: hand;";
        confirmButton.setStyle(confirmStyle);
        confirmButton.setOnMouseEntered(e -> confirmButton.setStyle(confirmStyle));
        confirmButton.setOnMouseExited(e -> confirmButton.setStyle(confirmStyle));

        checkButton.setOnAction(event -> {
            conflictLabel.setText("");
            try {
                LocalDate date = requireDate(datePicker);
                LocalTime start = parseTimeFlexible(startTimeField.getText());
                LocalTime end = parseTimeFlexible(endTimeField.getText());
                ScheduleSlot slot = context.services().moInterviewScheduleService()
                    .toInterviewSlot(date, start, end);
                InterviewConflictReport report = context.services().moInterviewScheduleService()
                    .checkConflicts(organiserId, jobId, peerId, slot);
                StringBuilder text = new StringBuilder();
                if (report.blockingErrors().isEmpty()) {
                    text.append("No blocking schedule conflicts.");
                } else {
                    text.append("Blocking issues:\n");
                    for (String line : report.blockingErrors()) {
                        text.append("• ").append(line).append('\n');
                    }
                }
                if (!report.warnings().isEmpty()) {
                    text.append("\nWarnings:\n");
                    for (String line : report.warnings()) {
                        text.append("• ").append(line).append('\n');
                    }
                }
                if (report.hasBlockingIssues()) {
                    conflictLabel.setStyle("-fx-text-fill: #b00020 !important;");
                } else {
                    conflictLabel.setStyle("-fx-text-fill: #1b5e20 !important;");
                }
                conflictLabel.setText(text.toString().trim());
            } catch (IllegalArgumentException exception) {
                conflictLabel.setStyle("-fx-text-fill: #b00020 !important;");
                conflictLabel.setText(exception.getMessage());
            }
        });

        confirmButton.setOnAction(event -> {
            try {
                LocalDate date = requireDate(datePicker);
                LocalTime start = parseTimeFlexible(startTimeField.getText());
                LocalTime end = parseTimeFlexible(endTimeField.getText());
                InterviewMessageTemplate tpl = templateBox.getSelectionModel().getSelectedItem();
                if (tpl == null) {
                    throw new IllegalArgumentException("Please select a message template.");
                }
                if (locationField.getText() == null || locationField.getText().isBlank()) {
                    throw new IllegalArgumentException("Location (or online link) must not be blank.");
                }
                InterviewConflictReport pre = context.services().moInterviewScheduleService()
                    .checkConflicts(
                        organiserId,
                        jobId,
                        peerId,
                        context.services().moInterviewScheduleService().toInterviewSlot(date, start, end)
                    );
                if (pre.hasBlockingIssues()) {
                    throw new IllegalArgumentException(
                        "Resolve blocking conflicts before sending:\n" + String.join("\n", pre.blockingErrors())
                    );
                }

                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Send interview invitation");
                confirm.setHeaderText(null);
                confirm.setContentText("Send this message to the applicant in the job chat thread?");
                var choice = confirm.showAndWait();
                if (choice.isEmpty() || choice.get() != javafx.scene.control.ButtonType.OK) {
                    return;
                }

                InterviewTemplateVariables vars = buildVariables(
                    context,
                    organiserId,
                    peerId,
                    job,
                    date,
                    start,
                    end,
                    locationField.getText(),
                    notesArea.getText()
                );
                context.services().moInterviewScheduleService().sendInterviewInvitation(
                    organiserId,
                    jobId,
                    peerId,
                    date,
                    start,
                    end,
                    locationField.getText().trim(),
                    notesArea.getText(),
                    tpl.body(),
                    vars
                );

                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("Sent");
                ok.setHeaderText(null);
                ok.setContentText("Interview invitation was sent as a chat message.");
                ok.showAndWait();
                context.openChatContext(jobId, peerId);
                nav.resetTo(PageId.MESSAGES);
            } catch (IllegalArgumentException exception) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Cannot send");
                err.setHeaderText(null);
                err.setContentText(exception.getMessage());
                err.showAndWait();
            }
        });

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.setStyle(DARK_TEXT);
        int row = 0;
        form.add(sectionLabel("Date"), 0, row);
        form.add(datePicker, 1, row++);
        form.add(sectionLabel("Start time"), 0, row);
        form.add(startTimeField, 1, row++);
        form.add(sectionLabel("End time"), 0, row);
        form.add(endTimeField, 1, row++);
        form.add(sectionLabel("Location / link"), 0, row);
        form.add(locationField, 1, row++);
        form.add(sectionLabel("Notes"), 0, row);
        form.add(notesArea, 1, row++);
        form.add(sectionLabel("Template"), 0, row);
        form.add(templateBox, 1, row++);

        VBox formCard = cardShell(
            new VBox(14,
                styledMuted("Fill in the interview details. Preview updates as you type."),
                form
            )
        );

        VBox previewCard = cardShell(
            new VBox(10,
                sectionLabel("Message preview"),
                previewArea
            )
        );
        VBox.setVgrow(previewArea, Priority.ALWAYS);

        HBox actions = new HBox(14, checkButton, confirmButton, UiTheme.createBackButton(nav));
        actions.setAlignment(Pos.CENTER_LEFT);

        pageBody.getChildren().addAll(
            heading,
            contextLine,
            helpLabel,
            formCard,
            actions,
            conflictLabel,
            previewCard
        );
        VBox.setVgrow(previewCard, Priority.ALWAYS);

        refreshPreview.run();

        return wrapInScrollAndScene(nav, context, pageBody);
    }

    private static Label styledMuted(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setFont(Font.font("Arial", 13));
        l.setStyle(DARK_TEXT);
        return l;
    }

    private static Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        l.setStyle("-fx-text-fill: #4664a8 !important;");
        return l;
    }

    private static VBox cardShell(Node content) {
        VBox box = new VBox(content);
        box.setPadding(new Insets(24));
        box.setBackground(new Background(new BackgroundFill(
            Color.WHITE,
            new CornerRadii(24),
            Insets.EMPTY
        )));
        box.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(24),
            new BorderWidths(2)
        )));
        box.setMaxWidth(Double.MAX_VALUE);
        box.setStyle(DARK_TEXT);
        return box;
    }

    private static ListCell<MoInterviewScheduleService.InterviewMessageTemplate> interviewTemplateListCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(InterviewMessageTemplate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.label());
                    setStyle(DARK_TEXT + "-fx-background-color: #ffffff;");
                }
            }
        };
    }

    private static Scene wrapInScrollAndScene(NavigationManager nav, UiAppContext context, VBox pageBody) {
        ScrollPane scroll = new ScrollPane(pageBody);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle(
            "-fx-background: transparent;"
                + "-fx-background-color: transparent;"
                + "-fx-border-color: transparent;"
        );

        BorderPane root = UiTheme.createPage(
            "Interview schedule",
            UiTheme.createMoSidebar(nav, null),
            scroll,
            nav,
            context
        );
        return UiTheme.createScene(root);
    }

    private static LocalDate requireDate(DatePicker datePicker) {
        if (datePicker.getValue() == null) {
            throw new IllegalArgumentException("Please pick an interview date.");
        }
        return datePicker.getValue();
    }

    private static InterviewTemplateVariables buildVariables(
        UiAppContext context,
        String organiserId,
        String peerId,
        JobPosting job,
        LocalDate date,
        LocalTime start,
        LocalTime end,
        String location,
        String notes
    ) {
        return new InterviewTemplateVariables(
            context.displayNameForUser(peerId),
            context.displayNameForUser(organiserId),
            job.title(),
            job.jobId(),
            date.format(DATE_TEXT),
            start.format(TIME_TEXT),
            end.format(TIME_TEXT),
            location == null ? "" : location.trim(),
            notes == null ? "" : notes.trim()
        );
    }

    private static LocalTime parseTimeFlexible(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Time must not be blank (use HH:mm).");
        }
        String text = raw.trim();
        for (DateTimeFormatter formatter : new DateTimeFormatter[] {
            DateTimeFormatter.ofPattern("HH:mm"),
            DateTimeFormatter.ofPattern("H:mm")
        }) {
            try {
                return LocalTime.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new IllegalArgumentException("Invalid time \"" + text + "\". Use HH:mm such as 09:30.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
