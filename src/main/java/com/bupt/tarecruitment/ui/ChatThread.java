package com.bupt.tarecruitment.ui;

import java.time.LocalDateTime;

/**
 * 表示消息页中的单个会话摘要。
 */
record ChatThread(
    String jobId,
    String courseName,
    String peerUserId,
    String peerDisplayName,
    String preview,
    int unreadCount,
    LocalDateTime lastAt
) {
}
