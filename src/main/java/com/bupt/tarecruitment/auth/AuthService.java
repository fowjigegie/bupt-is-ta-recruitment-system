package com.bupt.tarecruitment.auth;

import java.util.Objects;
import java.util.Optional;

public final class AuthService {
    private final UserRepository repository;
    private final AuthValidator validator;

    private UserAccount currentUser;

    public AuthService(UserRepository repository, AuthValidator validator) {
        this.repository = Objects.requireNonNull(repository);
        this.validator = Objects.requireNonNull(validator);
    }

    public UserAccount register(String userId, String password, UserRole role) {
        // US00: 注册只负责“创建账号”，不做自动登录
        // 这里的 validateRegistration 只做格式/必填校验，不负责查重
        validator.validateRegistration(userId, password, role);

        // 查重：同一个 userId 只能注册一次
        if (repository.findByUserId(userId).isPresent()) {
            throw new IllegalArgumentException("A user already exists for userId: " + userId);
        }

        // 新账号默认 ACTIVE，displayName 默认等于 userId
        UserAccount account = new UserAccount(
            userId,
            password,
            role,
            userId,
            AccountStatus.ACTIVE
        );
        repository.save(account);
        return account;
    }

    public UserAccount login(String userId, String password) {
        // US00: 登录校验账号存在、状态有效、密码匹配
        validator.validateLogin(userId, password);

        // 账号必须存在
        UserAccount account = repository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("No user exists for userId: " + userId));

        // 账号必须是 ACTIVE
        if (account.status() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Account is not active for userId: " + userId);
        }

        // 密码对不上直接拒绝
        if (!account.passwordHash().equals(password)) {
            throw new IllegalArgumentException("Invalid password for userId: " + userId);
        }

        // 记录当前会话用户（UI 会用这个判断是否已登录）
        currentUser = account;
        return account;
    }

    public Optional<UserAccount> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public void logout() {
        currentUser = null;
    }
}
