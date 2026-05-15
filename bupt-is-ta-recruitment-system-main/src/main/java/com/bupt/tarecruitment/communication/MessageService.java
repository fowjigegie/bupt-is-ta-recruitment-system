package com.bupt.tarecruitment.communication;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 处理会话查询、发送消息和已读标记。
 */
public final class MessageService {
    private final MessageRepository repository;

    public MessageService(MessageRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    // US08 的核心服务：
    // 负责列出某个 job 下的双方聊天记录、发送新消息、统计未读数、标记已读。
    public List<InquiryMessage> listConversation(String jobId, String userA, String userB) {
        requireNonBlank(jobId, "jobId");
        requireNonBlank(userA, "userA");
        requireNonBlank(userB, "userB");

        // 只保留当前这两个人在当前岗位下互相发送的消息，再按时间排序。
        return repository.findByJobId(jobId).stream()
            .filter(message ->
                (message.senderUserId().equals(userA) && message.receiverUserId().equals(userB))
                    || (message.senderUserId().equals(userB) && message.receiverUserId().equals(userA))
            )
            .sorted(Comparator.comparing(InquiryMessage::sentAt).thenComparing(InquiryMessage::messageId))
            .toList();
    }

    // 发送消息时会生成新的 messageId，并默认把 read 标记为 false。
    public InquiryMessage sendMessage(String jobId, String senderUserId, String receiverUserId, String content) {
        requireNonBlank(jobId, "jobId");
        requireNonBlank(senderUserId, "senderUserId");
        requireNonBlank(receiverUserId, "receiverUserId");
        requireNonBlank(content, "content");

        if (senderUserId.equals(receiverUserId)) {
            throw new IllegalArgumentException("senderUserId and receiverUserId must be different.");
        }

        InquiryMessage message = new InquiryMessage(
            nextMessageId(),
            jobId.trim(),
            senderUserId.trim(),
            receiverUserId.trim(),
            LocalDateTime.now(),
            content.trim(),
            false
        );
        repository.save(message);
        return message;
    }

    // 首页红点和消息页未读提示都会依赖这个统计。
    public long countUnreadMessagesForUser(String viewerUserId) {
        requireNonBlank(viewerUserId, "viewerUserId");

        return repository.findAll().stream()
            .filter(message -> message.receiverUserId().equals(viewerUserId.trim()))
            .filter(message -> !message.read())
            .count();
    }

    // 用户打开某个 conversation 后，把"发给我且还没读"的消息批量标记为已读。
    public int markConversationAsRead(String jobId, String viewerUserId, String peerUserId) {
        requireNonBlank(jobId, "jobId");
        requireNonBlank(viewerUserId, "viewerUserId");
        requireNonBlank(peerUserId, "peerUserId");

        List<InquiryMessage> conversation = listConversation(jobId, viewerUserId, peerUserId);
        int updatedCount = 0;
        for (InquiryMessage message : conversation) {
            if (message.receiverUserId().equals(viewerUserId) && !message.read()) {
                InquiryMessage updated = new InquiryMessage(
                    message.messageId(),
                    message.jobId(),
                    message.senderUserId(),
                    message.receiverUserId(),
                    message.sentAt(),
                    message.content(),
                    true
                );
                repository.save(updated);
                updatedCount++;
            }
        }
        return updatedCount;
    }

    // 用于 dashboard 等场景快速找到"最近一次聊过的会话"。
    public Optional<ConversationReference> findMostRecentConversationForUser(String viewerUserId) {
        requireNonBlank(viewerUserId, "viewerUserId");

        return repository.findAll().stream()
            .filter(message -> message.senderUserId().equals(viewerUserId.trim()) || message.receiverUserId().equals(viewerUserId.trim()))
            .sorted(Comparator.comparing(InquiryMessage::sentAt).thenComparing(InquiryMessage::messageId).reversed())
            .findFirst()
            .map(message -> new ConversationReference(
                message.jobId(),
                message.senderUserId().equals(viewerUserId.trim()) ? message.receiverUserId() : message.senderUserId()
            ));
    }

    // 消息编号是全局递增的 message001 / message002 / ...
    private String nextMessageId() {
        int nextSequence = repository.findAll().stream()
            .map(InquiryMessage::messageId)
            .filter(messageId -> messageId.startsWith("message"))
            .map(messageId -> messageId.substring("message".length()))
            .filter(numberPart -> !numberPart.isBlank())
            .mapToInt(Integer::parseInt)
            .max()
            .orElse(0) + 1;

        return "message%03d".formatted(nextSequence);
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }
}
