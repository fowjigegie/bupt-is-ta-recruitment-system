package com.bupt.tarecruitment.communication;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 表示一条站内咨询消息。
 */
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
