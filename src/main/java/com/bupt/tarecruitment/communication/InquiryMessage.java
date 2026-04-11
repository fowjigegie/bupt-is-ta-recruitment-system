package com.bupt.tarecruitment.communication;

import java.time.LocalDateTime;
import java.util.Objects;

// US08 的消息对象。
// 一条消息总是属于某个 jobId 下的一次 applicant <-> MO 对话，并记录已读状态。
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
