package com.bupt.tarecruitment.auth;

import java.util.regex.Pattern;

public final class AuthValidator {
    private static final Pattern APPLICANT_ID_PATTERN = Pattern.compile("ta\\d{3,}");
    private static final Pattern MO_ID_PATTERN = Pattern.compile("mo\\d{3,}");
    private static final Pattern ADMIN_ID_PATTERN = Pattern.compile("admin\\d{3,}");

    public void validateRegistration(String userId, String password, UserRole role) {
        requireNotBlank(userId, "userId");
        requireNotBlank(password, "password");
        requireRole(role);
        validateUserIdFormat(userId, role);
    }

    public void validateLogin(String userId, String password) {
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

        if (!pattern.matcher(userId).matches()) {
            throw new IllegalArgumentException(
                String.format("userId format is invalid for role %s. Expected prefix rule for this project.", role.name())
            );
        }
    }
}
