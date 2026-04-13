package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.auth.UserAccount;
import com.bupt.tarecruitment.auth.UserRole;
import com.bupt.tarecruitment.communication.InquiryMessage;
import com.bupt.tarecruitment.job.JobPosting;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 封装消息页中的会话构建、列表渲染和消息气泡展示。
 */
final class MessagesConversationView {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private MessagesConversationView() {
    }

    static List<ChatThread> buildThreads(UiAppContext context, String currentUserId) {
        Map<String, ChatThread> threadMap = new HashMap<>();

        for (InquiryMessage message : context.services().messageRepository().findAll()) {
            if (!message.senderUserId().equals(currentUserId) && !message.receiverUserId().equals(currentUserId)) {
                continue;
            }

            String peerUserId = message.senderUserId().equals(currentUserId)
                ? message.receiverUserId()
                : message.senderUserId();
            String key = threadKey(message.jobId(), peerUserId);

            String peerDisplay = resolveUserDisplay(context, peerUserId);
            String courseName = resolveCourseName(context, message.jobId());
            int unread = (message.receiverUserId().equals(currentUserId) && !message.read()) ? 1 : 0;
            ChatThread candidate = new ChatThread(
                message.jobId(),
                courseName,
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
                        courseName,
                        peerUserId,
                        peerDisplay,
                        message.content(),
                        unreadCount,
                        message.sentAt()
                    )
                    : new ChatThread(
                        existing.jobId(),
                        existing.courseName(),
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
                String key = threadKey(selectedJobId, peer);
                threadMap.putIfAbsent(
                    key,
                    new ChatThread(
                        selectedJobId,
                        resolveCourseName(context, selectedJobId),
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

    static void renderThreadList(
        UiAppContext context,
        List<ChatThread> threads,
        MessagesWorkspace workspace
    ) {
        workspace.threadListBox().getChildren().clear();

        if (threads.isEmpty()) {
            String emptyBody = context.session().role() == UserRole.MO
                ? "Applicants can start a chat from Job Detail. Each job-specific conversation will appear here."
                : "Open chat from a job card or Job Detail page to start a conversation with the MO.";
            workspace.threadListBox().getChildren().add(UiTheme.createWhiteCard("No conversations", emptyBody));
            workspace.setSelectedThread(null);
            workspace.conversationTitle().setText("Select a conversation");
            workspace.messageFlow().getChildren().clear();
            return;
        }

        for (ChatThread thread : threads) {
            Button rowButton = createThreadButton(thread);
            rowButton.setOnAction(event -> {
                workspace.setSelectedThread(thread);
                context.openChatContext(thread.jobId(), thread.peerUserId());
                renderConversation(context, context.session().userId(), thread, workspace);
                workspace.clearStatus();
            });
            workspace.threadListBox().getChildren().add(rowButton);
        }

        ChatThread current = workspace.selectedThread();
        boolean existsInList = false;
        if (current != null) {
            for (ChatThread thread : threads) {
                if (threadKey(thread.jobId(), thread.peerUserId())
                    .equals(threadKey(current.jobId(), current.peerUserId()))) {
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
                    if (thread.peerUserId().equals(selectedPeerUserId)
                        && (selectedJobId == null || thread.jobId().equals(selectedJobId))) {
                        current = (selectedJobId == null)
                            ? thread
                            : new ChatThread(
                                selectedJobId,
                                resolveCourseName(context, selectedJobId),
                                thread.peerUserId(),
                                thread.peerDisplayName(),
                                thread.preview(),
                                thread.unreadCount(),
                                thread.lastAt()
                            );
                        existsInList = true;
                        workspace.setSelectedThread(current);
                        break;
                    }
                }
            }
        }

        if (current == null || !existsInList) {
            current = threads.getFirst();
            workspace.setSelectedThread(current);
        }

        String preferredJobId = context.selectedJobId();
        String preferredPeerUserId = context.selectedChatPeerUserId();
        if (preferredJobId != null
            && preferredPeerUserId != null
            && current.peerUserId().equals(preferredPeerUserId)) {
            current = new ChatThread(
                preferredJobId,
                resolveCourseName(context, preferredJobId),
                current.peerUserId(),
                current.peerDisplayName(),
                current.preview(),
                current.unreadCount(),
                current.lastAt()
            );
            workspace.setSelectedThread(current);
        }

        context.openChatContext(current.jobId(), current.peerUserId());
        renderConversation(context, context.session().userId(), current, workspace);
    }

    static void renderConversation(
        UiAppContext context,
        String currentUserId,
        ChatThread thread,
        MessagesWorkspace workspace
    ) {
        workspace.conversationTitle().setText("Conversation with %s | %s".formatted(thread.peerDisplayName(), thread.courseName()));

        List<InquiryMessage> messages = listConversationByPeerAndJob(context, currentUserId, thread.peerUserId(), thread.jobId());
        markConversationAsRead(context, currentUserId, thread.peerUserId(), thread.jobId());

        workspace.messageFlow().getChildren().clear();
        if (messages.isEmpty()) {
            workspace.messageFlow().getChildren().add(UiTheme.createMutedText("No messages yet. Start the conversation below."));
        } else {
            for (InquiryMessage message : messages) {
                workspace.messageFlow().getChildren().add(createBubble(message, currentUserId));
            }
        }
        workspace.messageScroll().setVvalue(1.0);
    }

    private static Button createThreadButton(ChatThread thread) {
        String unreadSuffix = thread.unreadCount() > 0 ? "  (unread: %d)".formatted(thread.unreadCount()) : "";
        String preview = thread.preview() == null ? "" : thread.preview();
        String headline = "%s | %s%s".formatted(thread.peerDisplayName(), thread.courseName(), unreadSuffix);

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
        Label content = new Label("%s%n%s".formatted(message.content(), message.sentAt().format(TIME_FORMAT)));
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

    private static String resolveCourseName(UiAppContext context, String jobId) {
        String title = context.services().jobRepository().findByJobId(jobId)
            .map(JobPosting::title)
            .orElse(jobId);
        if (title.toLowerCase().startsWith("ta for ")) {
            return title.substring("ta for ".length());
        }
        return title;
    }

    private static List<InquiryMessage> listConversationByPeerAndJob(
        UiAppContext context,
        String userId,
        String peerUserId,
        String jobId
    ) {
        return context.services().messageRepository().findAll().stream()
            .filter(message ->
                message.jobId().equals(jobId)
                    && ((message.senderUserId().equals(userId) && message.receiverUserId().equals(peerUserId))
                    || (message.senderUserId().equals(peerUserId) && message.receiverUserId().equals(userId)))
            )
            .sorted(Comparator.comparing(InquiryMessage::sentAt).thenComparing(InquiryMessage::messageId))
            .toList();
    }

    private static void markConversationAsRead(
        UiAppContext context,
        String userId,
        String peerUserId,
        String jobId
    ) {
        context.services().messageService().markConversationAsRead(jobId, userId, peerUserId);
    }

    private static String threadKey(String jobId, String peerUserId) {
        return jobId + "|" + peerUserId;
    }
}
