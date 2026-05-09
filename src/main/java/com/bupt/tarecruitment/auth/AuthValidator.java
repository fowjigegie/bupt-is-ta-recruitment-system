package com.bupt.tarecruitment.auth;

import java.util.regex.Pattern;

/**
 * 校验账号注册与登录输入是否合法。
 */
public final class AuthValidator {
    private static final Pattern APPLICANT_ID_PATTERN = Pattern.compile("ta\\d{3,}");
    private static final Pattern MO_ID_PATTERN = Pattern.compile("mo\\d{3,}");
    private static final Pattern ADMIN_ID_PATTERN = Pattern.compile("admin\\d{3,}");

    public void validateRegistration(String userId, String password, UserRole role) {
        // US00: 注册时要求 userId / password / role 都不为空，且 userId 格式符合角色前缀规则
        requireNotBlank(userId, "userId");
        requireNotBlank(password, "password");
        requireRole(role);
        validateUserIdFormat(userId, role);
    }

    public void validateLogin(String userId, String password) {
        // 登录只要求非空，不检查角色前缀
        requireNotBlank(userId, "userId");
        requireNotBlank(password, "password");
    }

    private void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }

    private void requireRole(UserRole role) {
        if (role == null) {
            throw new IllegalArgumentException("role must not be null.");
        }
    }

    // US00: 注册时按角色强制 userId 前缀规则（ta### / mo### / admin###）
    private void validateUserIdFormat(String userId, UserRole role) {
        Pattern pattern;
        switch (role) {
            case APPLICANT:
                pattern = APPLICANT_ID_PATTERN;
                break;
            case MO:
                pattern = MO_ID_PATTERN;
                break;
            case ADMIN:
                pattern = ADMIN_ID_PATTERN;
                break;
            default:
                throw new IllegalArgumentException("Unsupported role: " + role);
        }

        // US00: 注册时按角色强制 userId 前缀规则
        // APPLICANT -> ta###, MO -> mo###, ADMIN -> admin###
        // 角色对应的 ID 前缀规则：ta### / mo### / admin###
        // 比如 ta001 / mo002 / admin001
        if (!pattern.matcher(userId).matches()) {
            throw new IllegalArgumentException(
                String.format("userId format is invalid for role %s. Expected prefix rule for this project.", role.name())
            );
        }
    }
}
