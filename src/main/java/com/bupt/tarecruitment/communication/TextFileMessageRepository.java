package com.bupt.tarecruitment.communication;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.bupt.tarecruitment.common.storage.DataFile;

/**
 * 基于文本文件存储站内消息。
 */
public final class TextFileMessageRepository implements MessageRepository {
    private static final String FIELD_SEPARATOR = "\\|";
    private static final String OUTPUT_SEPARATOR = "|";

    private final Path messagesFilePath;

    public TextFileMessageRepository() {
        this(Path.of("").toAbsolutePath().resolve("data"));
    }

    public TextFileMessageRepository(Path dataDirectory) {
        this.messagesFilePath = dataDirectory.resolve(DataFile.MESSAGES.fileName());
        ensureFileExists();
    }

    // 读取全部消息记录。
    @Override
    public List<InquiryMessage> findAll() {
        try {
            List<InquiryMessage> messages = new ArrayList<>();
            for (String line : Files.readAllLines(messagesFilePath, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                messages.add(parseLine(line));
            }
            return messages;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read inquiry messages.", exception);
        }
    }

    // 消息天然带 jobId，因此可以按岗位切出单独会话。
    @Override
    public List<InquiryMessage> findByJobId(String jobId) {
        return findAll().stream()
            .filter(message -> message.jobId().equals(jobId))
            .toList();
    }

    // save 既负责新增新消息，也负责"同 messageId 覆盖"已读状态等更新。
    @Override
    public void save(InquiryMessage message) {
        List<InquiryMessage> messages = new ArrayList<>(findAll());
        boolean replaced = false;

        for (int index = 0; index < messages.size(); index++) {
            if (messages.get(index).messageId().equals(message.messageId())) {
                messages.set(index, message);
                replaced = true;
                break;
            }
        }

        if (!replaced) {
            messages.add(message);
        }

        writeAll(messages);
    }

    // messages.txt 每行固定 7 个字段：
    // messageId | jobId | sender | receiver | sentAt | content | readStatus
    private InquiryMessage parseLine(String line) {
        String[] fields = line.split(FIELD_SEPARATOR, -1);
        if (fields.length != 7) {
            throw new IllegalStateException("Invalid message record: " + line);
        }

        return new InquiryMessage(
            fields[0],
            fields[1],
            fields[2],
            fields[3],
            LocalDateTime.parse(fields[4]),
            fields[5],
            parseReadStatus(fields[6])
        );
    }

    // 兼容旧数据里的 true/false，同时把当前主线写法统一成 READ / UNREAD。
    private boolean parseReadStatus(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.equalsIgnoreCase("READ") || normalized.equalsIgnoreCase("true")) {
            return true;
        }
        if (normalized.equalsIgnoreCase("UNREAD") || normalized.equalsIgnoreCase("false") || normalized.isEmpty()) {
            return false;
        }
        throw new IllegalStateException("Invalid read status value: " + value);
    }

    // 把内存里的 boolean 已读状态转换成更直观的文本。
    private String formatReadStatus(boolean read) {
        return read ? "READ" : "UNREAD";
    }

    // 整体重写 messages.txt，保持文本存储实现简单。
    private void writeAll(List<InquiryMessage> messages) {
        List<String> lines = new ArrayList<>();
        lines.add(DataFile.MESSAGES.initialLines().getFirst());
        for (InquiryMessage message : messages) {
            lines.add(formatLine(message));
        }

        try {
            Files.write(messagesFilePath, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write inquiry messages.", exception);
        }
    }

    // 把消息对象序列化成一行文本。
    private String formatLine(InquiryMessage message) {
        return String.join(
            OUTPUT_SEPARATOR,
            message.messageId(),
            message.jobId(),
            message.senderUserId(),
            message.receiverUserId(),
            message.sentAt().toString(),
            message.content(),
            formatReadStatus(message.read())
        );
    }

    // 启动时保证 data/messages.txt 存在。
    private void ensureFileExists() {
        try {
            Files.createDirectories(messagesFilePath.getParent());
            if (Files.notExists(messagesFilePath)) {
                Files.write(messagesFilePath, DataFile.MESSAGES.initialLines(), StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to prepare message storage.", exception);
        }
    }
}
