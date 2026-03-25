package com.bupt.tarecruitment.auth;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<UserAccount> findByUserId(String userId);

    List<UserAccount> findAll();

    void save(UserAccount userAccount);
}
