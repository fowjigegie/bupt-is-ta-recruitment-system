package com.bupt.tarecruitment.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Shows the AI assistant dialog.
 */
public final class FakeAiAssistantDialog {
    private FakeAiAssistantDialog() {
    }

    public static void show(Window owner, UiAppContext context) {
        Stage dialog = new Stage();
        dialog.setTitle("BUPT-TA AI Assistant");
        dialog.initModality(Modality.WINDOW_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        VBox transcript = new VBox(12);
        transcript.setPadding(new Insets(8, 6, 8, 6));

        boolean realApiEnabled = context.services().aiAssistantService().isRealApiEnabled();
        String providerLabel = context.services().aiAssistantService().providerLabel();
        transcript.getChildren().addAll(
            createAssistantBubble(
                realApiEnabled
                    ? "Hello, I'm the BUPT-TA AI Assistant.\nTry asking: \"Recommend the best jobs for me\" or \"What skills am I missing for TA for Software Engineering?\""
                    : "Hello, I'm the BUPT-TA built-in helper.\nI can answer with local recruitment-system data. Try asking about recommended jobs, missing skills, timetable checks, or application status."
            ),
            createMetaLabel("Assistant mode: " + providerLabel)
        );
        if (context.selectedJobId() != null) {
            transcript.getChildren().add(createMetaLabel("Current selected job context: " + context.selectedJobId()));
        } else {
            transcript.getChildren().add(createMetaLabel("No job is currently selected. You can still ask about a named job by typing its title, module code, or job ID."));
        }

        ScrollPane transcriptScroll = new ScrollPane(transcript);
        transcriptScroll.setFitToWidth(true);
        transcriptScroll.setPrefViewportHeight(430);
        transcriptScroll.setStyle(
            "-fx-background:#ffffff;" +
                "-fx-background-color:#ffffff;" +
                "-fx-border-color:transparent;"
        );

        TextField inputField = new TextField();
        inputField.setPromptText("Ask the AI assistant...");
        inputField.setPrefHeight(44);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        var sendButton = UiTheme.createPrimaryButton("Send", 130, 44);

        Runnable scrollToBottom = () -> transcriptScroll.setVvalue(1.0);
        Runnable[] sendActionRef = new Runnable[1];
        sendActionRef[0] = () -> {
            String question = inputField.getText().trim();
            if (question.isBlank()) {
                return;
            }

            transcript.getChildren().add(createUserBubble(question));
            inputField.clear();
            inputField.setDisable(true);
            sendButton.setDisable(true);

            Label thinkingLabel = createBubbleLabel("Thinking...");
            HBox thinkingRow = wrapAssistantBubble(thinkingLabel);
            transcript.getChildren().add(thinkingRow);
            scrollToBottom.run();

            Timeline thinkingAnimation = new Timeline(
                new KeyFrame(Duration.ZERO, event -> thinkingLabel.setText("Thinking.")),
                new KeyFrame(Duration.millis(350), event -> thinkingLabel.setText("Thinking..")),
                new KeyFrame(Duration.millis(700), event -> thinkingLabel.setText("Thinking..."))
            );
            thinkingAnimation.setCycleCount(Timeline.INDEFINITE);
            thinkingAnimation.play();

            Task<String> answerTask = new Task<>() {
                @Override
                protected String call() {
                    return context.services().aiAssistantService().answer(
                        question,
                        context.session().isAuthenticated() ? context.session().userId() : null,
                        context.selectedJobId()
                    );
                }
            };
            answerTask.setOnSucceeded(event -> {
                thinkingAnimation.stop();
                thinkingLabel.setText(answerTask.getValue());
                inputField.setDisable(false);
                sendButton.setDisable(false);
                inputField.requestFocus();
                scrollToBottom.run();
            });
            answerTask.setOnFailed(event -> {
                thinkingAnimation.stop();
                thinkingLabel.setText("The AI assistant could not answer right now. Please try again later.");
                inputField.setDisable(false);
                sendButton.setDisable(false);
                inputField.requestFocus();
                scrollToBottom.run();
            });
            Thread worker = new Thread(answerTask, "ai-assistant-request");
            worker.setDaemon(true);
            worker.start();
        };

        sendButton.setOnAction(event -> sendActionRef[0].run());
        inputField.setOnAction(event -> sendActionRef[0].run());

        HBox inputRow = new HBox(12, inputField, sendButton);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("AI Assistant");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#4664a8"));

        Label subtitle = new Label(
            context.services().aiAssistantService().isRealApiEnabled()
                ? "Cloud AI with local recruitment-system context."
                : "Built-in helper using local system data. Set DASHSCOPE_API_KEY or NVIDIA_API_KEY before launch to enable Cloud AI."
        );
        subtitle.setFont(Font.font("Arial", 14));
        subtitle.setTextFill(Color.web("#8b7fa0"));

        Label pluginTag = new Label(context.services().aiAssistantService().isRealApiEnabled() ? "Cloud AI" : "Built-in");
        pluginTag.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        pluginTag.setTextFill(Color.web("#b05a88"));
        pluginTag.setStyle(
            "-fx-background-color: #ffe9f3;" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: #f3b7db;" +
                "-fx-border-width: 1.2;" +
                "-fx-border-radius: 16;" +
                "-fx-padding: 6 12 6 12;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox headerRow = new HBox(14, title, spacer, pluginTag);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        var closeButton = UiTheme.createOutlineButton("Close", 120, 42);
        closeButton.setOnAction(event -> dialog.close());
        HBox footerRow = new HBox(closeButton);
        footerRow.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(16, headerRow, subtitle, transcriptScroll, inputRow, footerRow);
        content.setPadding(new Insets(24));
        content.setBackground(UiTheme.pageBackground());

        Scene scene = new Scene(content, 860, 640);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private static Node createUserBubble(String text) {
        Label bubble = createBubbleLabel(text);
        bubble.setStyle(
            "-fx-background-color: #4969ad;" +
                "-fx-background-radius: 18 18 6 18;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 15px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 12 14 12 14;"
        );
        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_RIGHT);
        return row;
    }

    private static Node createAssistantBubble(String text) {
        Label bubble = createBubbleLabel(text);
        return wrapAssistantBubble(bubble);
    }

    private static HBox wrapAssistantBubble(Label bubble) {
        applyAssistantBubbleStyle(bubble);
        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static Label createBubbleLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(560);
        return label;
    }

    private static void applyAssistantBubbleStyle(Label bubble) {
        bubble.setStyle(
            "-fx-background-color: white;" +
                "-fx-background-radius: 18 18 18 6;" +
                "-fx-border-color: #f4d9e6;" +
                "-fx-border-width: 1.5;" +
                "-fx-border-radius: 18 18 18 6;" +
                "-fx-text-fill: #5c3f6b;" +
                "-fx-font-size: 15px;" +
                "-fx-padding: 12 14 12 14;"
        );
    }

    private static Label createMetaLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        label.setTextFill(Color.web("#8b7fa0"));
        label.setPadding(new Insets(2, 6, 4, 6));
        return label;
    }
}
