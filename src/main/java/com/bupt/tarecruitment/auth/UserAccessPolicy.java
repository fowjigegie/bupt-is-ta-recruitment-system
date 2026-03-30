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
