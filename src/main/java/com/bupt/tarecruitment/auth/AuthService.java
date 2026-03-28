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
        validator.validateRegistration(userId, password, role);

        if (repository.findByUserId(userId).isPresent()) {
            throw new IllegalArgumentException("A user already exists for userId: " + userId);
        }

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
        validator.validateLogin(userId, password);

        UserAccount account = repository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("No user exists for userId: " + userId));

        if (account.status() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Account is not active for userId: " + userId);
        }

        if (!account.passwordHash().equals(password)) {
            throw new IllegalArgumentException("Invalid password for userId: " + userId);
        }

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
