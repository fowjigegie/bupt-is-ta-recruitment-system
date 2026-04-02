package com.bupt.tarecruitment.communication;

import com.bupt.tarecruitment.common.storage.DataFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Override
    public List<InquiryMessage> findByJobId(String jobId) {
        return findAll().stream()
            .filter(message -> message.jobId().equals(jobId))
            .toList();
    }

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

    private String formatReadStatus(boolean read) {
        return read ? "READ" : "UNREAD";
    }

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
