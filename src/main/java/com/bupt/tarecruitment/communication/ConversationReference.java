package com.bupt.tarecruitment.communication;

import java.util.Objects;

public record ConversationReference(
    String jobId,
    String peerUserId
) {
    public ConversationReference {
        Objects.requireNonNull(jobId);
        Objects.requireNonNull(peerUserId);
    }
}
