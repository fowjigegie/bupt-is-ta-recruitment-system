package com.bupt.tarecruitment.communication;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class MessageService {
    private final MessageRepository repository;

    public MessageService(MessageRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public List<InquiryMessage> listConversation(String jobId, String userA, String userB) {
        requireNonBlank(jobId, "jobId");
        requireNonBlank(userA, "userA");
        requireNonBlank(userB, "userB");

        return repository.findByJobId(jobId).stream()
            .filter(message ->
                (message.senderUserId().equals(userA) && message.receiverUserId().equals(userB))
                    || (message.senderUserId().equals(userB) && message.receiverUserId().equals(userA))
            )
            .sorted(Comparator.comparing(InquiryMessage::sentAt).thenComparing(InquiryMessage::messageId))
            .toList();
    }

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

    public long countUnreadMessagesForUser(String viewerUserId) {
        requireNonBlank(viewerUserId, "viewerUserId");

        return repository.findAll().stream()
            .filter(message -> message.receiverUserId().equals(viewerUserId.trim()))
            .filter(message -> !message.read())
            .count();
    }

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
