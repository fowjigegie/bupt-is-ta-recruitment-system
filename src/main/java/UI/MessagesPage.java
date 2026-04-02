package UI;

import com.bupt.tarecruitment.auth.UserAccount;
import com.bupt.tarecruitment.auth.UserRole;
import com.bupt.tarecruitment.communication.InquiryMessage;
import com.bupt.tarecruitment.job.JobPosting;
import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagesPage extends Application {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.MESSAGES, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        VBox center = new VBox(16);
        center.setPadding(new Insets(24, 36, 24, 36));

        if (!context.session().isAuthenticated()) {
            center.getChildren().addAll(
                UiTheme.createPageHeading("Messages"),
                UiTheme.createWhiteCard("Sign in required", "Please sign in before opening messages."),
                new HBox(UiTheme.createBackButton(nav))
            );
            BorderPane root = UiTheme.createPage("Messages", null, center, nav, context);
            return UiTheme.createScene(root);
        }

        String userId = context.session().userId();
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

        TextField messageInput = new TextField();
        messageInput.setPromptText("Type your message to the selected user...");
        messageInput.setPrefHeight(42);
        messageInput.setStyle(
            "-fx-background-color: #ffffff;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #f0d9e9;" +
                "-fx-border-radius: 12;" +
                "-fx-padding: 0 12 0 12;"
        );

        Label statusLabel = UiTheme.createMutedText("");
        statusLabel.setTextFill(Color.web("#b00020"));

        Button refreshButton = UiTheme.createSoftButton("Refresh", 110, 42);
        Button sendButton = UiTheme.createPrimaryButton("Send", 120, 42);

        Runnable refreshThreads = () -> {
            List<ChatThread> threads = buildThreads(context, userId);
            renderThreadList(context, threads, selectedThread, threadListBox, messageFlow, conversationTitle, messageScroll, statusLabel);
        };

        Runnable sendMessageAction = () -> {
            ChatThread active = selectedThread.get();
            if (active == null) {
                statusLabel.setText("Please select a conversation first.");
                statusLabel.setTextFill(Color.web("#b00020"));
                return;
            }
            String content = messageInput.getText().trim();
            if (content.isBlank()) {
                statusLabel.setText("Message content must not be blank.");
                statusLabel.setTextFill(Color.web("#b00020"));
                return;
            }

            try {
                context.services().messageService().sendMessage(
                    active.jobId(),
                    userId,
                    active.peerUserId(),
                    content
                );
                messageInput.clear();
                statusLabel.setText("Message sent.");
                statusLabel.setTextFill(Color.web("#2e7d32"));
                refreshThreads.run();
                selectedThread.set(active);
                renderConversation(context, userId, active, messageFlow, conversationTitle, messageScroll);
            } catch (IllegalArgumentException exception) {
                statusLabel.setText(exception.getMessage());
                statusLabel.setTextFill(Color.web("#b00020"));
            }
        };

        refreshButton.setOnAction(event -> refreshThreads.run());
        sendButton.setOnAction(event -> sendMessageAction.run());
        messageInput.setOnAction(event -> sendMessageAction.run());

        HBox composer = new HBox(10, messageInput, sendButton, refreshButton);
        HBox.setHgrow(messageInput, Priority.ALWAYS);
        composer.setAlignment(Pos.CENTER_LEFT);

        VBox leftPane = new VBox(10,
            UiTheme.createSectionTitle("Conversations"),
            threadScroll
        );
        leftPane.setPadding(new Insets(14));
        leftPane.setBackground(new Background(new BackgroundFill(Color.web("#fdfcff"), new CornerRadii(18), Insets.EMPTY)));
        leftPane.setBorder(new Border(new BorderStroke(
            Color.web("#f0d9e9"), BorderStrokeStyle.SOLID, new CornerRadii(18), new BorderWidths(1.2)
        )));
        leftPane.setPrefWidth(420);

        VBox rightPane = new VBox(12,
            conversationTitle,
            messageScroll,
            composer,
            statusLabel
        );
        rightPane.setPadding(new Insets(14));
        rightPane.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(18), Insets.EMPTY)));
        rightPane.setBorder(new Border(new BorderStroke(
            Color.web("#f0d9e9"), BorderStrokeStyle.SOLID, new CornerRadii(18), new BorderWidths(1.2)
        )));
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        HBox contentRow = new HBox(14, leftPane, rightPane);
        HBox footer = new HBox(UiTheme.createBackButton(nav));
        footer.setAlignment(Pos.CENTER_LEFT);

        center.getChildren().addAll(
            UiTheme.createPageHeading("Messages"),
            UiTheme.createMutedText("Chat with MO per job context. Select a conversation and send messages below."),
            contentRow,
            footer
        );

        refreshThreads.run();

        BorderPane root = UiTheme.createPage(
            "Messages",
            context.session().role() == UserRole.MO
                ? UiTheme.createMoSidebar(nav, null)
                : UiTheme.createApplicantSidebar(nav, null),
            center,
            nav,
            context
        );
        return UiTheme.createScene(root);
    }

    private static List<ChatThread> buildThreads(UiAppContext context, String currentUserId) {
        Map<String, ChatThread> threadMap = new HashMap<>();

        for (InquiryMessage message : context.services().messageRepository().findAll()) {
            if (!message.senderUserId().equals(currentUserId) && !message.receiverUserId().equals(currentUserId)) {
                continue;
            }

            String peerUserId = message.senderUserId().equals(currentUserId)
                ? message.receiverUserId()
                : message.senderUserId();
            String key = peerUserId;

            String peerDisplay = resolveUserDisplay(context, peerUserId);
            int unread = (message.receiverUserId().equals(currentUserId) && !message.read()) ? 1 : 0;
            ChatThread candidate = new ChatThread(
                message.jobId(),
                peerUserId,
                peerDisplay,
                message.content(),
                unread,
                message.sentAt()
            );

            ChatThread existing = threadMap.get(key);
            if (existing == null) {
                threadMap.put(key, candidate);
            } else {
                int unreadCount = existing.unreadCount() + unread;
                ChatThread updated = existing.lastAt().isBefore(message.sentAt())
                    ? new ChatThread(
                        message.jobId(),
                        peerUserId,
                        peerDisplay,
                        message.content(),
                        unreadCount,
                        message.sentAt()
                    )
                    : new ChatThread(
                        existing.jobId(),
                        existing.peerUserId(),
                        existing.peerDisplayName(),
                        existing.preview(),
                        unreadCount,
                        existing.lastAt()
                    );
                threadMap.put(key, updated);
            }
        }

        String selectedJobId = context.selectedJobId();
        String selectedPeer = context.selectedChatPeerUserId();
        if (selectedJobId != null) {
            String peer = selectedPeer != null ? selectedPeer : deriveDefaultPeerByJob(context, selectedJobId, currentUserId);
            if (peer != null) {
                String key = peer;
                threadMap.putIfAbsent(
                    key,
                    new ChatThread(
                        selectedJobId,
                        peer,
                        resolveUserDisplay(context, peer),
                        "(no messages yet)",
                        0,
                        LocalDateTime.MIN
                    )
                );
            }
        }

        return threadMap.values().stream()
            .sorted(Comparator.comparing(ChatThread::lastAt).reversed())
            .toList();
    }

    private static void renderThreadList(
        UiAppContext context,
        List<ChatThread> threads,
        ObjectProperty<ChatThread> selectedThread,
        VBox threadListBox,
        VBox messageFlow,
        Label conversationTitle,
        ScrollPane messageScroll,
        Label statusLabel
    ) {
        threadListBox.getChildren().clear();

        if (threads.isEmpty()) {
            threadListBox.getChildren().add(UiTheme.createWhiteCard(
                "No conversations",
                "Open chat from a job card/detail page to start a conversation."
            ));
            selectedThread.set(null);
            conversationTitle.setText("Select a conversation");
            messageFlow.getChildren().clear();
            return;
        }

        for (ChatThread thread : threads) {
            Button rowButton = createThreadButton(thread);
            rowButton.setOnAction(event -> {
                selectedThread.set(thread);
                context.openChatContext(thread.jobId(), thread.peerUserId());
                renderConversation(context, context.session().userId(), thread, messageFlow, conversationTitle, messageScroll);
                statusLabel.setText("");
            });
            threadListBox.getChildren().add(rowButton);
        }

        ChatThread current = selectedThread.get();
        boolean existsInList = false;
        if (current != null) {
            for (ChatThread thread : threads) {
                if (thread.peerUserId().equals(current.peerUserId())) {
                    existsInList = true;
                    break;
                }
            }
        }

        if (!existsInList) {
            String selectedJobId = context.selectedJobId();
            String selectedPeerUserId = context.selectedChatPeerUserId();
            if (selectedPeerUserId != null) {
                for (ChatThread thread : threads) {
                    if (thread.peerUserId().equals(selectedPeerUserId)) {
                        current = (selectedJobId == null)
                            ? thread
                            : new ChatThread(
                                selectedJobId,
                                thread.peerUserId(),
                                thread.peerDisplayName(),
                                thread.preview(),
                                thread.unreadCount(),
                                thread.lastAt()
                            );
                        existsInList = true;
                        selectedThread.set(current);
                        break;
                    }
                }
            }
        }

        if (current == null || !existsInList) {
            current = threads.getFirst();
            selectedThread.set(current);
        }

        String preferredJobId = context.selectedJobId();
        String preferredPeerUserId = context.selectedChatPeerUserId();
        if (preferredJobId != null
            && preferredPeerUserId != null
            && current.peerUserId().equals(preferredPeerUserId)) {
            current = new ChatThread(
                preferredJobId,
                current.peerUserId(),
                current.peerDisplayName(),
                current.preview(),
                current.unreadCount(),
                current.lastAt()
            );
            selectedThread.set(current);
        }

        context.openChatContext(current.jobId(), current.peerUserId());
        renderConversation(context, context.session().userId(), current, messageFlow, conversationTitle, messageScroll);
    }

    private static void renderConversation(
        UiAppContext context,
        String currentUserId,
        ChatThread thread,
        VBox messageFlow,
        Label conversationTitle,
        ScrollPane messageScroll
    ) {
        conversationTitle.setText("Conversation with " + thread.peerDisplayName());

        List<InquiryMessage> messages = listConversationByPeer(context, currentUserId, thread.peerUserId());
        markConversationByPeerAsRead(context, currentUserId, thread.peerUserId(), messages);

        messageFlow.getChildren().clear();
        if (messages.isEmpty()) {
            messageFlow.getChildren().add(UiTheme.createMutedText("No messages yet. Start the conversation below."));
        } else {
            for (InquiryMessage message : messages) {
                messageFlow.getChildren().add(createBubble(message, currentUserId));
            }
        }
        messageScroll.setVvalue(1.0);
    }

    private static Button createThreadButton(ChatThread thread) {
        String unreadSuffix = thread.unreadCount() > 0 ? "  (unread: %d)".formatted(thread.unreadCount()) : "";
        String preview = thread.preview() == null ? "" : thread.preview();
        String headline = thread.peerDisplayName() + unreadSuffix;

        Label nameLabel = new Label(headline);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        nameLabel.setTextFill(Color.web("#2f3553"));

        Label previewLabel = new Label(preview);
        previewLabel.setFont(Font.font("Arial", 12));
        previewLabel.setTextFill(Color.web("#8a8f9d"));
        previewLabel.setWrapText(true);
        previewLabel.setMaxWidth(320);

        VBox textBox = new VBox(4, nameLabel, previewLabel);
        textBox.setAlignment(Pos.CENTER_LEFT);

        Button button = new Button();
        button.setGraphic(textBox);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(74);
        button.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #fff7fd, #fff1f8);" +
                "-fx-text-fill: #374151;" +
                "-fx-background-radius: 16;" +
                "-fx-border-radius: 16;" +
                "-fx-border-color: #efd8e8;" +
                "-fx-border-width: 1.2;" +
                "-fx-padding: 10 14 10 14;"
        );
        button.setOnMouseEntered(event -> button.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #ffeefa, #ffe3f3);" +
                "-fx-text-fill: #2f3553;" +
                "-fx-background-radius: 16;" +
                "-fx-border-radius: 16;" +
                "-fx-border-color: #e7bcd4;" +
                "-fx-border-width: 1.2;" +
                "-fx-padding: 10 14 10 14;"
        ));
        button.setOnMouseExited(event -> button.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #fff7fd, #fff1f8);" +
                "-fx-text-fill: #374151;" +
                "-fx-background-radius: 16;" +
                "-fx-border-radius: 16;" +
                "-fx-border-color: #efd8e8;" +
                "-fx-border-width: 1.2;" +
                "-fx-padding: 10 14 10 14;"
        ));
        return button;
    }

    private static HBox createBubble(InquiryMessage message, String currentUserId) {
        boolean mine = message.senderUserId().equals(currentUserId);
        Label content = new Label(
            "%s%n%s".formatted(message.content(), message.sentAt().format(TIME_FORMAT))
        );
        content.setWrapText(true);
        content.setMaxWidth(420);
        content.setStyle(
            "-fx-padding: 10 12 10 12;" +
                "-fx-background-radius: 14;" +
                (mine
                    ? "-fx-background-color: #dbeafe; -fx-text-fill: #1f2937;"
                    : "-fx-background-color: #fce7f3; -fx-text-fill: #1f2937;")
        );

        HBox row = new HBox(content);
        row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return row;
    }

    private static String resolveUserDisplay(UiAppContext context, String userId) {
        return context.services().userRepository().findByUserId(userId)
            .map(UserAccount::displayName)
            .filter(displayName -> !displayName.isBlank())
            .orElse(userId);
    }

    private static String deriveDefaultPeerByJob(UiAppContext context, String jobId, String currentUserId) {
        return context.services().jobRepository().findByJobId(jobId)
            .map(JobPosting::organiserId)
            .filter(organiserId -> !organiserId.equals(currentUserId))
            .orElse(null);
    }

    private static List<InquiryMessage> listConversationByPeer(UiAppContext context, String userId, String peerUserId) {
        return context.services().messageRepository().findAll().stream()
            .filter(message ->
                (message.senderUserId().equals(userId) && message.receiverUserId().equals(peerUserId))
                    || (message.senderUserId().equals(peerUserId) && message.receiverUserId().equals(userId))
            )
            .sorted(Comparator.comparing(InquiryMessage::sentAt).thenComparing(InquiryMessage::messageId))
            .toList();
    }

    private static void markConversationByPeerAsRead(
        UiAppContext context,
        String userId,
        String peerUserId,
        List<InquiryMessage> messages
    ) {
        messages.stream()
            .map(InquiryMessage::jobId)
            .distinct()
            .forEach(jobId -> context.services().messageService().markConversationAsRead(jobId, userId, peerUserId));
    }

    private record ChatThread(
        String jobId,
        String peerUserId,
        String peerDisplayName,
        String preview,
        int unreadCount,
        LocalDateTime lastAt
    ) {
    }

    public static void main(String[] args) {
        launch(args);
    }
}
