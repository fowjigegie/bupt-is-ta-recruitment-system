package com.bupt.tarecruitment.ai;

import java.util.Objects;

public record ChatTurn(String role, String content) {
    public ChatTurn {
        Objects.requireNonNull(role);
        Objects.requireNonNull(content);
        if (!role.equals("user") && !role.equals("assistant")) {
            throw new IllegalArgumentException("role must be user or assistant");
        }
    }
}
