package com.bupt.tarecruitment.auth;

import java.util.Objects;

/**
 * 表示系统中的用户账号信息。
 */
public record UserAccount(
    String userId,
    String passwordHash,
    UserRole role,
    String displayName,
    AccountStatus status
) {
    public UserAccount {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(passwordHash);
        Objects.requireNonNull(role);
        Objects.requireNonNull(displayName);
        Objects.requireNonNull(status);
    }
}
