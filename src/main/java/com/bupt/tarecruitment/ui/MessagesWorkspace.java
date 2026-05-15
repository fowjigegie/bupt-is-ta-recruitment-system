package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.auth.UserRole;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
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

/**
 * 维护消息页的左右栏控件和当前会话状态。
 */
final class MessagesWorkspace {
    private final ObjectProperty<ChatThread> selectedThread;
    private final VBox threadListBox;
    private final Label conversationTitle;
    private final VBox messageFlow;
    private final ScrollPane messageScroll;
    private final TextArea messageInput;
    private final Label statusLabel;
    private final Button refreshButton;
    private final Button sendButton;
    private final VBox leftPane;
    private final VBox rightPane;

    private MessagesWorkspace(
        ObjectProperty<ChatThread> selectedThread,
        VBox threadListBox,
        Label conversationTitle,
        VBox messageFlow,
        ScrollPane messageScroll,
        TextArea messageInput,
        Label statusLabel,
        Button refreshButton,
        Button sendButton,
        VBox leftPane,
        VBox rightPane
    ) {
        this.selectedThread = selectedThread;
        this.threadListBox = threadListBox;
        this.conversationTitle = conversationTitle;
        this.messageFlow = messageFlow;
        this.messageScroll = messageScroll;
        this.messageInput = messageInput;
        this.statusLabel = statusLabel;
        this.refreshButton = refreshButton;
        this.sendButton = sendButton;
        this.leftPane = leftPane;
        this.rightPane = rightPane;
    }

    static MessagesWorkspace create(UserRole role, NavigationManager nav) {
        ObjectProperty<ChatThread> selectedThread = new SimpleObjectProperty<>(null);

        VBox threadListBox = new VBox(10);
        threadListBox.setPadding(new Insets(6, 2, 6, 2));

        ScrollPane threadScroll = new ScrollPane(threadListBox);
        threadScroll.setFitToWidth(true);
        threadScroll.setPrefViewportHeight(460);
        threadScroll.setStyle(
            "-fx-background:#faf9fc;" +
                "-fx-background-color:#faf9fc;" +
                "-fx-border-color:transparent;" +
                "-fx-background-radius:14;"
        );

        Label conversationTitle = new Label("Select a conversation");
        conversationTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        conversationTitle.setTextFill(Color.web("#4664a8"));
        conversationTitle.setPadding(new Insets(4, 10, 4, 10));
        conversationTitle.setBackground(new Background(new BackgroundFill(
            Color.web("#fff2bf"),
            new CornerRadii(8),
            Insets.EMPTY
        )));

        VBox messageFlow = new VBox(10);
        messageFlow.setPadding(new Insets(8, 10, 8, 10));

        ScrollPane messageScroll = new ScrollPane(messageFlow);
        messageScroll.setFitToWidth(true);
        messageScroll.setPrefViewportHeight(360);
        messageScroll.setStyle(
            "-fx-background:#ffffff;" +
                "-fx-background-color:#ffffff;" +
                "-fx-border-color:#f0d9e9;" +
                "-fx-border-radius:12;" +
                "-fx-background-radius:12;"
        );

        TextArea messageInput = new TextArea();
        messageInput.setPromptText("Type your message to the selected user... (Ctrl+Enter to send)");
        messageInput.setWrapText(true);
        messageInput.setStyle(
            "-fx-background-color: #ffffff;"
                + "-fx-control-inner-background: #ffffff;"
                + "-fx-background-radius: 12;"
                + "-fx-border-color: #f0d9e9;"
                + "-fx-border-radius: 12;"
                + "-fx-padding: 8 12 8 12;"
        );

        Label statusLabel = UiTheme.createMutedText("");
        statusLabel.setTextFill(Color.web("#b00020"));

        Button refreshButton = UiTheme.createSoftButton("Refresh", 110, 42);
        Button sendButton = UiTheme.createPrimaryButton("Send", 120, 42);

        HBox sendRefreshRow = new HBox(8, sendButton, refreshButton);
        sendRefreshRow.setAlignment(Pos.CENTER_RIGHT);

        VBox buttonStack = new VBox(8, sendRefreshRow);
        buttonStack.setFillWidth(true);
        if (role == UserRole.MO) {
            Button interviewSchedule = new Button("Interview schedule");
            interviewSchedule.setMinHeight(40);
            interviewSchedule.setMaxWidth(Double.MAX_VALUE);
            interviewSchedule.setStyle(
                "-fx-text-fill: #111111 !important;"
                    + "-fx-font-size: 14px;"
                    + "-fx-background-color: #ffffff;"
                    + "-fx-border-color: #4664a8;"
                    + "-fx-border-radius: 10;"
                    + "-fx-background-radius: 10;"
            );
            interviewSchedule.setOnAction(event -> nav.goTo(PageId.MO_INTERVIEW_SCHEDULE));
            buttonStack.getChildren().add(interviewSchedule);
        }

        HBox composer = new HBox(10, messageInput, buttonStack);
        HBox.setHgrow(messageInput, Priority.ALWAYS);
        composer.setAlignment(Pos.TOP_LEFT);
        messageInput.minHeightProperty().bind(buttonStack.heightProperty());
        messageInput.prefHeightProperty().bind(buttonStack.heightProperty());
        messageInput.maxHeightProperty().bind(buttonStack.heightProperty());

        VBox leftPane = new VBox(10, UiTheme.createSectionTitle("Conversations"), threadScroll);
        leftPane.setPadding(new Insets(14));
        leftPane.setBackground(new Background(new BackgroundFill(Color.web("#fdfcff"), new CornerRadii(18), Insets.EMPTY)));
        leftPane.setBorder(new Border(new BorderStroke(
            Color.web("#f0d9e9"), BorderStrokeStyle.SOLID, new CornerRadii(18), new BorderWidths(1.2)
        )));
        leftPane.setPrefWidth(420);

        VBox rightPane = new VBox(12, conversationTitle, messageScroll, composer, statusLabel);
        rightPane.setPadding(new Insets(14));
        rightPane.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(18), Insets.EMPTY)));
        rightPane.setBorder(new Border(new BorderStroke(
            Color.web("#f0d9e9"), BorderStrokeStyle.SOLID, new CornerRadii(18), new BorderWidths(1.2)
        )));
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        return new MessagesWorkspace(
            selectedThread,
            threadListBox,
            conversationTitle,
            messageFlow,
            messageScroll,
            messageInput,
            statusLabel,
            refreshButton,
            sendButton,
            leftPane,
            rightPane
        );
    }

    ChatThread selectedThread() {
        return selectedThread.get();
    }

    void setSelectedThread(ChatThread thread) {
        selectedThread.set(thread);
    }

    VBox threadListBox() {
        return threadListBox;
    }

    Label conversationTitle() {
        return conversationTitle;
    }

    VBox messageFlow() {
        return messageFlow;
    }

    ScrollPane messageScroll() {
        return messageScroll;
    }

    TextArea messageInput() {
        return messageInput;
    }

    Button refreshButton() {
        return refreshButton;
    }

    Button sendButton() {
        return sendButton;
    }

    VBox leftPane() {
        return leftPane;
    }

    VBox rightPane() {
        return rightPane;
    }

    void showError(String text) {
        statusLabel.setTextFill(Color.web("#b00020"));
        statusLabel.setText(text);
    }

    void showSuccess(String text) {
        statusLabel.setTextFill(Color.web("#2e7d32"));
        statusLabel.setText(text);
    }

    void clearStatus() {
        statusLabel.setText("");
    }
}
