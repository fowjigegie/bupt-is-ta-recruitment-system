package com.bupt.tarecruitment.auth;

import com.bupt.tarecruitment.common.storage.DataFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 基于文本文件存储用户账号。
 */
public final class TextFileUserRepository implements UserRepository {
    private static final String FIELD_SEPARATOR = "\\|";
    private static final String OUTPUT_SEPARATOR = "|";

    private final Path usersFilePath;

    public TextFileUserRepository() {
        this(Path.of("").toAbsolutePath().resolve("data"));
    }

    public TextFileUserRepository(Path dataDirectory) {
        this.usersFilePath = dataDirectory.resolve(DataFile.USERS.fileName());
        ensureFileExists();
    }

    @Override
    public Optional<UserAccount> findByUserId(String userId) {
        return findAll().stream()
            .filter(account -> account.userId().equals(userId))
            .findFirst();
    }

    @Override
    public List<UserAccount> findAll() {
        try {
            List<UserAccount> users = new ArrayList<>();

            for (String line : Files.readAllLines(usersFilePath, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }

                users.add(parseLine(line));
            }

            return users;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read users.", exception);
        }
    }

    @Override
    public void save(UserAccount userAccount) {
        List<UserAccount> users = new ArrayList<>(findAll());
        boolean replaced = false;

        for (int index = 0; index < users.size(); index++) {
            if (users.get(index).userId().equals(userAccount.userId())) {
                users.set(index, userAccount);
                replaced = true;
                break;
            }
        }

        if (!replaced) {
            users.add(userAccount);
        }

        writeAll(users);
    }

    private UserAccount parseLine(String line) {
        String[] fields = line.split(FIELD_SEPARATOR, -1);
        if (fields.length != 5) {
            throw new IllegalStateException("Invalid user record: " + line);
        }

        return new UserAccount(
            fields[0],
            fields[1],
            UserRole.valueOf(fields[2]),
            fields[3],
            AccountStatus.valueOf(fields[4])
        );
    }

    private void writeAll(List<UserAccount> users) {
        List<String> lines = new ArrayList<>();
        lines.add(DataFile.USERS.initialLines().getFirst());

        for (UserAccount user : users) {
            lines.add(formatLine(user));
        }

        try {
            Files.write(usersFilePath, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write users.", exception);
        }
    }

    private String formatLine(UserAccount user) {
        return String.join(
            OUTPUT_SEPARATOR,
            user.userId(),
            user.passwordHash(),
            user.role().name(),
            user.displayName(),
            user.status().name()
        );
    }

    private void ensureFileExists() {
        try {
            Files.createDirectories(usersFilePath.getParent());
            if (Files.notExists(usersFilePath)) {
                Files.write(usersFilePath, DataFile.USERS.initialLines(), StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to prepare user storage.", exception);
        }
    }
}
