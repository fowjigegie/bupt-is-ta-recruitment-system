package UI;

import com.bupt.tarecruitment.ai.ChatTurn;
import com.bupt.tarecruitment.ai.JobAdvisorFallback;
import com.bupt.tarecruitment.ai.OpenAiCompatibleClient;
import com.bupt.tarecruitment.applicant.ApplicantCv;
import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobStatus;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Modal chat UI for the AI job advisor on the More Jobs page.
 * <p>
 * Uses a background thread for API calls so the JavaFX thread stays responsive.
 * When no API key is configured, falls back to {@link JobAdvisorFallback}.
 */
final class JobAiChatWindow {
    /** Max characters of resume text injected into the system prompt. */
    private static final int MAX_CV_CHARS = 12_000;
    /** Max characters of the OPEN jobs listing injected into the system prompt. */
    private static final int MAX_JOBS_CHARS = 24_000;

    private JobAiChatWindow() {
    }

    /**
     * Opens a window-modal stage owned by the main app; loads profile, CV text, and open jobs
     * into the system prompt, then wires Send / Enter to cloud completion or local fallback.
     */
    static void open(NavigationManager nav, UiAppContext context, List<JobPosting> openJobsSnapshot) {
        Stage owner = nav.stage();
        Stage chatStage = new Stage();
        chatStage.initOwner(owner);
        chatStage.initModality(Modality.WINDOW_MODAL);
        chatStage.setTitle("BUPT-TA · AI Job Advisor");

        // Snapshot context for the model (and fallback) for this session only.
        String userId = context.session().userId();
        Optional<ApplicantProfile> profile = context.services().profileRepository().findByUserId(userId);
        String cvDigest = loadCvDigest(context, userId);
        String jobsCatalog = formatJobsCatalog(openJobsSnapshot);
        String systemPrompt = buildSystemPrompt(userId, profile, cvDigest, jobsCatalog);

        VBox messagesList = new VBox(12);
        messagesList.setFillWidth(true);
        messagesList.setPadding(new Insets(16, 18, 24, 18));

        ScrollPane messageScroll = new ScrollPane(messagesList);
        messageScroll.setFitToWidth(true);
        messageScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messageScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        messageScroll.setStyle("-fx-background: #ffffff; -fx-background-color: #ffffff;");
        messageScroll.setPannable(false);
        VBox.setVgrow(messageScroll, Priority.ALWAYS);

        TextArea input = new TextArea();
        input.setWrapText(true);
        input.setPrefRowCount(2);
        input.setPromptText(
            "Hello - I'm the BUPT-TA AI assistant. Type your question below and click Send."
        );
        input.setStyle(
            "-fx-font-family: Arial, 'Segoe UI', sans-serif;"
                + "-fx-font-size: 14px;"
                + "-fx-control-inner-background: #fffafd;"
                + "-fx-background-radius: 14;"
                + "-fx-border-color: #e8c4d8;"
                + "-fx-border-radius: 14;"
                + "-fx-border-width: 1.2;"
                + "-fx-padding: 10;"
        );

        // Rolling chat history for the remote API (trimmed before each request).
        List<ChatTurn> apiHistory = new ArrayList<>();
        OpenAiCompatibleClient client = new OpenAiCompatibleClient();
        boolean cloudAi = client.hasApiKey();
        if (cloudAi) {
            chatStage.setTitle("BUPT-TA · AI Job Advisor (Cloud)");
        }

        // After new bubbles are added, jump to the latest message on the FX thread.
        Runnable scrollChatToBottom = () -> Platform.runLater(() -> {
            messageScroll.layout();
            messageScroll.setVvalue(1.0);
        });

        String welcomeText = cloudAi
            ? (
                "Hello - I'm the BUPT-TA AI assistant.\n"
                    + "API key is loaded — each Send calls the remote chat API (model "
                    + client.model()
                    + "). Replies are real model output (not scripted).\n"
                    + "Context: your profile, resume text, and OPEN jobs. Override base URL with OPENAI_API_BASE if you use a mirror.\n"
                    + "Type your question below and click Send."
            )
            : (
                "Hello - I'm the BUPT-TA AI assistant.\n"
                    + "Cloud AI is not configured — using quick local matching only.\n"
                    + "Set environment variable OPENAI_API_KEY, or create data/openai-api-key.txt (first line = key).\n"
                    + "Type your question below and click Send."
            );
        messagesList.getChildren().add(chatBubbleRow(false, welcomeText));
        scrollChatToBottom.run();

        Button sendButton = UiTheme.createSoftButton("Send", 120, 44);
        sendButton.setStyle(
            "-fx-background-color: linear-gradient(to right, #ffd699, #ffb3d9);"
                + "-fx-text-fill: #c45786;"
                + "-fx-font-weight: bold;"
                + "-fx-font-size: 14px;"
                + "-fx-background-radius: 22;"
                + "-fx-cursor: hand;"
        );

        Button closeButton = UiTheme.createOutlineButton("Close", 120, 44);

        Runnable sendAction = () -> {
            String userText = input.getText() == null ? "" : input.getText().trim();
            if (userText.isBlank()) {
                return;
            }
            input.clear();
            messagesList.getChildren().add(chatBubbleRow(true, userText));
            final HBox thinkingRow = chatBubbleRow(false, "Thinking…");
            messagesList.getChildren().add(thinkingRow);
            scrollChatToBottom.run();
            sendButton.setDisable(true);

            // Network / file I/O off the FX thread; UI updates via Platform.runLater.
            Runnable runCall = () -> {
                try {
                    String answer;
                    if (client.hasApiKey()) {
                        List<ChatTurn> trimmed = OpenAiCompatibleClient.trimHistory(apiHistory, 10);
                        answer = client.complete(systemPrompt, trimmed, userText);
                    } else {
                        answer = JobAdvisorFallback.localAdvice(profile, openJobsSnapshot)
                            + "\n\nQ: \"" + userText + "\"\n"
                            + "(Offline — set OPENAI_API_KEY for full chat.)";
                    }
                    String finalAnswer = answer == null || answer.isBlank()
                        ? "(No reply text from the model. Check OPENAI_API_BASE / network, or try again.)"
                        : answer;
                    Platform.runLater(() -> {
                        messagesList.getChildren().remove(thinkingRow);
                        messagesList.getChildren().add(chatBubbleRow(false, finalAnswer));
                        scrollChatToBottom.run();
                        apiHistory.add(new ChatTurn("user", userText));
                        apiHistory.add(new ChatTurn("assistant", finalAnswer));
                        sendButton.setDisable(false);
                    });
                } catch (Throwable exception) {
                    String fallback = JobAdvisorFallback.localAdvice(profile, openJobsSnapshot);
                    String detail = exception.getMessage() == null ? exception.toString() : exception.getMessage();
                    Platform.runLater(() -> {
                        messagesList.getChildren().remove(thinkingRow);
                        String errBody = client.hasApiKey()
                            ? (
                                "Could not reach the cloud AI:\n" + detail
                                    + "\n\n--- Local fallback ---\n\n" + fallback
                            )
                            : ("Request failed:\n" + detail + "\n\n" + fallback);
                        messagesList.getChildren().add(chatBubbleRow(false, errBody));
                        scrollChatToBottom.run();
                        apiHistory.add(new ChatTurn("user", userText));
                        apiHistory.add(new ChatTurn("assistant", fallback));
                        sendButton.setDisable(false);
                    });
                }
            };
            new Thread(runCall, "job-ai-chat").start();
        };

        sendButton.setOnAction(event -> sendAction.run());

        // Enter sends; Shift+Enter inserts a newline (default TextArea behavior).
        input.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                event.consume();
                sendAction.run();
            }
        });

        closeButton.setOnAction(event -> chatStage.close());

        HBox actions = new HBox(14, sendButton, closeButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(4, 0, 0, 0));

        Label composerHint = UiTheme.createMutedText(
            cloudAi
                ? "Remote API — VPN/Clash: set user env HTTPS_PROXY=http://127.0.0.1:<HTTP port> (Java does not use browser proxy)."
                : "Local heuristics — add OPENAI_API_KEY or data/openai-api-key.txt for cloud AI."
        );

        Label inputCaption = new Label("Message");
        inputCaption.setTextFill(Color.web("#6b5b7a"));
        inputCaption.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        VBox composer = new VBox(10, composerHint, inputCaption, input, actions);
        composer.setPadding(new Insets(16, 4, 4, 4));

        Label header = UiTheme.createSectionTitle("AI Job Advisor");
        header.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        Label subHeader = UiTheme.createMutedText(
            cloudAi
                ? ("Live API · " + client.model() + " · chat completions · OPEN jobs vs your resume & profile")
                : "Offline · OPEN jobs vs your resume & profile"
        );
        subHeader.setFont(Font.font("Arial", 14));

        VBox top = new VBox(8, header, subHeader);
        top.setPadding(new Insets(8, 8, 16, 8));

        // Rounded white card wrapping the scrollable transcript.
        VBox chatShell = new VBox(messageScroll);
        chatShell.setPadding(new Insets(0));
        chatShell.setBackground(new Background(new BackgroundFill(
            Color.WHITE,
            new CornerRadii(24),
            Insets.EMPTY
        )));
        chatShell.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(24),
            new BorderWidths(2)
        )));
        VBox.setVgrow(chatShell, Priority.ALWAYS);

        BorderPane inner = new BorderPane();
        inner.setTop(top);
        inner.setCenter(chatShell);
        inner.setBottom(composer);
        inner.setPadding(new Insets(20, 28, 24, 28));

        BorderPane root = new BorderPane(inner);
        root.setBackground(UiTheme.pageBackground());

        Scene scene = new Scene(root, UiTheme.WINDOW_WIDTH, UiTheme.WINDOW_HEIGHT);
        chatStage.setScene(scene);
        chatStage.setMinWidth(900);
        chatStage.setMinHeight(560);
        chatStage.show();
    }

    /** User messages align right (blue); assistant aligns left (pink). */
    private static HBox chatBubbleRow(boolean fromUser, String text) {
        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(560);
        bubble.setFont(Font.font("Arial", 14));
        if (fromUser) {
            bubble.setStyle(
                "-fx-padding: 12 16;"
                    + "-fx-background-radius: 18;"
                    + "-fx-background-color: linear-gradient(to bottom right, #dbeafe, #e9d5ff);"
                    + "-fx-text-fill: #1f2937;"
                    + "-fx-border-color: #c7d2fe;"
                    + "-fx-border-radius: 18;"
                    + "-fx-border-width: 1;"
            );
        } else {
            bubble.setStyle(
                "-fx-padding: 12 16;"
                    + "-fx-background-radius: 18;"
                    + "-fx-background-color: #fce7f3;"
                    + "-fx-text-fill: #1f2937;"
                    + "-fx-border-color: #f9c6e4;"
                    + "-fx-border-radius: 18;"
                    + "-fx-border-width: 1;"
            );
        }

        HBox row = new HBox(bubble);
        row.setAlignment(fromUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setPadding(new Insets(2, 8, 2, 8));
        return row;
    }

    /**
     * Builds the fixed system message: role, applicant summary, resume excerpts, and OPEN postings.
     */
    private static String buildSystemPrompt(
        String userId,
        Optional<ApplicantProfile> profile,
        String cvDigest,
        String jobsCatalog
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a career advisor for the BUPT Teaching Assistant recruitment system (BUPT-TA).\n");
        sb.append("Applicant user id: ").append(userId).append("\n");
        sb.append("Answer in clear English. Reference specific job ids and skill requirements when helpful.\n\n");
        if (profile.isPresent()) {
            ApplicantProfile p = profile.get();
            sb.append("[Applicant profile]\n");
            sb.append("Name: ").append(p.fullName()).append("\n");
            sb.append("Student id: ").append(p.studentId()).append("\n");
            sb.append("Programme: ").append(p.programme()).append(" | Year: ").append(p.yearOfStudy()).append("\n");
            sb.append("Education level: ").append(p.educationLevel()).append("\n");
            sb.append("Skills: ").append(String.join(", ", p.skills())).append("\n");
            sb.append("Availability: ").append(String.join("; ", p.availabilitySlots())).append("\n");
            sb.append("Desired roles/keywords: ").append(String.join("; ", p.desiredPositions())).append("\n\n");
        } else {
            sb.append("[Applicant profile] No profile record found for this user.\n\n");
        }
        sb.append("[Resume excerpts]\n");
        sb.append(cvDigest.isBlank() ? "(No saved resume text found.)\n\n" : cvDigest + "\n\n");
        sb.append("[OPEN TA postings]\n");
        sb.append(jobsCatalog);
        return sb.toString();
    }

    /**
     * Loads the newest CVs for the user, concatenates plain text up to {@link #MAX_CV_CHARS}, newest first.
     */
    private static String loadCvDigest(UiAppContext context, String userId) {
        List<ApplicantCv> cvs = context.services().cvRepository().findByOwnerUserId(userId).stream()
            .sorted(Comparator.comparing(ApplicantCv::updatedAt).reversed())
            .toList();
        if (cvs.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        int budget = MAX_CV_CHARS;
        for (ApplicantCv cv : cvs) {
            if (budget <= 200) {
                break;
            }
            String text;
            try {
                text = context.services().cvStorage().loadCv(cv.fileName());
            } catch (RuntimeException ignored) {
                text = "(Could not read: " + cv.title() + ")";
            }
            String block = "---- CV: " + cv.title() + " ----\n" + text + "\n";
            if (block.length() > budget) {
                block = block.substring(0, budget) + "\n… (truncated)\n";
            }
            out.append(block);
            budget -= block.length();
        }
        return out.toString().trim();
    }

    /**
     * Formats only {@link JobStatus#OPEN} jobs as a plain-text catalog, capped by {@link #MAX_JOBS_CHARS}.
     */
    private static String formatJobsCatalog(List<JobPosting> jobs) {
        List<JobPosting> open = jobs.stream()
            .filter(j -> j.status() == JobStatus.OPEN)
            .sorted(Comparator.comparing(JobPosting::jobId))
            .toList();
        if (open.isEmpty()) {
            return "(No OPEN postings.)\n";
        }
        StringBuilder sb = new StringBuilder();
        for (JobPosting j : open) {
            sb.append("- ").append(j.jobId()).append(" | ").append(j.title()).append("\n");
            sb.append("  MO: ").append(j.organiserId()).append(" | Module / activity: ").append(j.moduleOrActivity()).append("\n");
            sb.append("  Weekly hours: ").append(j.weeklyHours()).append(" | Schedule: ")
                .append(j.scheduleSlots().isEmpty() ? "(none listed)" : String.join(", ", j.scheduleSlots())).append("\n");
            sb.append("  Required skills: ")
                .append(j.requiredSkills().isEmpty() ? "(none listed)" : String.join(", ", j.requiredSkills())).append("\n");
            sb.append("  Description: ").append(j.description().replace("\n", " ")).append("\n\n");
            if (sb.length() > MAX_JOBS_CHARS) {
                sb.append("… (posting list truncated)\n");
                break;
            }
        }
        return sb.toString();
    }
}
