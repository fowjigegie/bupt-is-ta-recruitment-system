package com.bupt.tarecruitment.auth;

import java.util.Objects;

public final class UserAccessPolicy {
    private static final UserAccessPolicy NO_OP = new UserAccessPolicy();

    private final UserRepository userRepository;
    private final boolean enabled;

    private UserAccessPolicy() {
        this.userRepository = null;
        this.enabled = false;
    }

    public UserAccessPolicy(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.enabled = true;
    }

    public static UserAccessPolicy noOp() {
        return NO_OP;
    }

    // US00/US01/US05: 需要确保 userId 对应的账号真实存在、处于 ACTIVE 状态、且角色匹配。
    // 如果 policy 处于 no-op 模式（enabled=false），就直接跳过校验。
    public void requireActiveUserWithRole(String userId, UserRole requiredRole) {
        if (!enabled) {
            return;
        }

        UserAccount account = userRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("No registered user exists for userId: " + userId));

        if (account.status() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("User account is not active for userId: " + userId);
        }

        if (account.role() != requiredRole) {
            throw new IllegalArgumentException(
                "userId %s is not an ACTIVE %s account.".formatted(userId, requiredRole.name())
            );
        }
    }
}
