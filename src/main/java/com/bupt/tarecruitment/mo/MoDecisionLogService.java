package com.bupt.tarecruitment.mo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Stores MO decision activity as a lightweight audit trail in the data directory.
 */
public final class MoDecisionLogService {
    private static final String FILE_NAME = "mo_decision_log.txt";
    private static final String DELIMITER = "\t";

    private final Path filePath;

    public MoDecisionLogService(Path dataDirectory) {
        Objects.requireNonNull(dataDirectory);
        this.filePath = dataDirectory.resolve(FILE_NAME);
        ensureFileExists();
    }

    public void record(
        String organiserId,
        String jobId,
        String applicationId,
        String applicantUserId,
        String action,
        String detail
    ) {
        MoDecisionLogEntry entry = new MoDecisionLogEntry(
            LocalDateTime.now(),
            normalize(organiserId),
            normalize(jobId),
            normalize(applicationId),
            normalize(applicantUserId),
            normalize(action),
            normalize(detail)
        );

        try {
            Files.writeString(
                filePath,
                serialize(entry) + System.lineSeparator(),
                StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write MO decision log.", exception);
        }
    }

    public List<MoDecisionLogEntry> listRecentForOrganiser(String organiserId, int limit) {
        String normalizedOrganiserId = normalize(organiserId);
        return readAll().stream()
            .filter(entry -> entry.organiserId().equals(normalizedOrganiserId))
            .sorted(Comparator.comparing(MoDecisionLogEntry::timestamp).reversed())
            .limit(Math.max(1, limit))
            .toList();
    }

    public List<MoDecisionLogEntry> listForApplication(String applicationId) {
        String normalizedApplicationId = normalize(applicationId);
        return readAll().stream()
            .filter(entry -> entry.applicationId().equals(normalizedApplicationId))
            .sorted(Comparator.comparing(MoDecisionLogEntry::timestamp).reversed())
            .toList();
    }

    private List<MoDecisionLogEntry> readAll() {
        ensureFileExists();
        try {
            List<MoDecisionLogEntry> entries = new ArrayList<>();
            for (String line : Files.readAllLines(filePath, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                parse(line).ifPresent(entries::add);
            }
            return entries;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read MO decision log.", exception);
        }
    }

    private java.util.Optional<MoDecisionLogEntry> parse(String line) {
        String[] parts = line.split(DELIMITER, -1);
        if (parts.length < 7) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(new MoDecisionLogEntry(
                LocalDateTime.parse(unescape(parts[0])),
                unescape(parts[1]),
                unescape(parts[2]),
                unescape(parts[3]),
                unescape(parts[4]),
                unescape(parts[5]),
                unescape(parts[6])
            ));
        } catch (RuntimeException exception) {
            return java.util.Optional.empty();
        }
    }

    private String serialize(MoDecisionLogEntry entry) {
        return String.join(DELIMITER,
            escape(entry.timestamp().toString()),
            escape(entry.organiserId()),
            escape(entry.jobId()),
            escape(entry.applicationId()),
            escape(entry.applicantUserId()),
            escape(entry.action()),
            escape(entry.detail())
        );
    }

    private void ensureFileExists() {
        try {
            Files.createDirectories(filePath.getParent());
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to initialize MO decision log file.", exception);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String escape(String value) {
        return normalize(value).replace("\\", "\\\\").replace("\t", " ").replace("\n", " ").replace("\r", " ");
    }

    private static String unescape(String value) {
        return value == null ? "" : value.replace("\\\\", "\\");
    }
}
