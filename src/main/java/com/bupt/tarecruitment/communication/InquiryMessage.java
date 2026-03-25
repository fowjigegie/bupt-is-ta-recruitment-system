package com.bupt.tarecruitment.communication;

import java.time.LocalDateTime;
import java.util.Objects;

public record InquiryMessage(
    String messageId,
    String jobId,
    String senderUserId,
    String receiverUserId,
    LocalDateTime sentAt,
    String content,
    boolean read
) {
    public InquiryMessage {
        Objects.requireNonNull(messageId);
        Objects.requireNonNull(jobId);
        Objects.requireNonNull(senderUserId);
        Objects.requireNonNull(receiverUserId);
        Objects.requireNonNull(sentAt);
        Objects.requireNonNull(content);
    }
}
