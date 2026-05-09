package com.bupt.tarecruitment.communication;

import java.util.Objects;

/**
 * 表示某个岗位上下文中的会话定位信息。
 */
public record ConversationReference(
    String jobId,
    String peerUserId
) {
    public ConversationReference {
        Objects.requireNonNull(jobId);
        Objects.requireNonNull(peerUserId);
    }
}
