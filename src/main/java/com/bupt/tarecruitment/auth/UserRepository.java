package com.bupt.tarecruitment.auth;

import java.util.List;
import java.util.Optional;

/**
 * 定义用户账号的持久化接口。
 */
public interface UserRepository {
    Optional<UserAccount> findByUserId(String userId);

    List<UserAccount> findAll();

    void save(UserAccount userAccount);
}
