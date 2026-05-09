package com.bupt.tarecruitment.mo;

import com.bupt.tarecruitment.application.ApplicationRepository;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * MO candidate pool management. It is separate from formal application statuses,
 * so MO can mark candidates as SHORTLISTED / MAYBE / NOT_SUITABLE before final review.
 */
public final class MoShortlistService {
    private static final String FILE_NAME = "mo_shortlist.txt";
    private static final String DELIMITER = "\t";

    private final Path filePath;
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final MoDecisionLogService decisionLogService;

    public MoShortlistService(
        Path dataDirectory,
        ApplicationRepository applicationRepository,
        JobRepository jobRepository,
        MoDecisionLogService decisionLogService
    ) {
        Objects.requireNonNull(dataDirectory);
        this.filePath = dataDirectory.resolve(FILE_NAME);
        this.applicationRepository = Objects.requireNonNull(applicationRepository);
        this.jobRepository = Objects.requireNonNull(jobRepository);
        this.decisionLogService = Objects.requireNonNull(decisionLogService);
        ensureFileExists();
    }

    public MoShortlistEntry mark(
        String organiserId,
        String applicationId,
        MoShortlistStatus status,
        String note
    ) {
        requireNonBlank(organiserId, "organiserId");
        requireNonBlank(applicationId, "applicationId");
        Objects.requireNonNull(status, "status must not be null.");

        JobApplication application = applicationRepository.findByApplicationId(applicationId.trim())
            .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        JobPosting job = jobRepository.findByJobId(application.jobId())
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + application.jobId()));
        if (!job.organiserId().equals(organiserId.trim())) {
            throw new IllegalArgumentException("You can only shortlist applicants for your own jobs.");
        }

        MoShortlistEntry entry = new MoShortlistEntry(
            organiserId.trim(),
            application.jobId(),
            application.applicationId(),
            application.applicantUserId(),
            status,
            note == null ? "" : note,
            LocalDateTime.now()
        );

        Map<String, MoShortlistEntry> entriesByApplication = readLatestByApplication();
        entriesByApplication.put(entry.applicationId(), entry);
        writeAll(entriesByApplication.values().stream()
            .sorted(Comparator.comparing(MoShortlistEntry::updatedAt))
            .toList());

        decisionLogService.record(
            entry.organiserId(),
            entry.jobId(),
            entry.applicationId(),
            entry.applicantUserId(),
            "SHORTLIST_" + status.name(),
            entry.note()
        );
        return entry;
    }

    public Optional<MoShortlistEntry> findByApplicationId(String applicationId) {
        if (applicationId == null || applicationId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(readLatestByApplication().get(applicationId.trim()));
    }

    public List<MoShortlistEntry> listForJob(String organiserId, String jobId) {
        String normalizedOrganiserId = normalize(organiserId);
        String normalizedJobId = normalize(jobId);
        return readLatestByApplication().values().stream()
            .filter(entry -> entry.organiserId().equals(normalizedOrganiserId))
            .filter(entry -> entry.jobId().equals(normalizedJobId))
            .sorted(Comparator.comparing(MoShortlistEntry::updatedAt).reversed())
            .toList();
    }

    public List<MoShortlistEntry> listForOrganiser(String organiserId) {
        String normalizedOrganiserId = normalize(organiserId);
        return readLatestByApplication().values().stream()
            .filter(entry -> entry.organiserId().equals(normalizedOrganiserId))
            .sorted(Comparator.comparing(MoShortlistEntry::updatedAt).reversed())
            .toList();
    }

    private Map<String, MoShortlistEntry> readLatestByApplication() {
        ensureFileExists();
        Map<String, MoShortlistEntry> entries = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(filePath, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                parse(line).ifPresent(entry -> entries.put(entry.applicationId(), entry));
            }
            return entries;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read MO shortlist file.", exception);
        }
    }

    private void writeAll(List<MoShortlistEntry> entries) {
        List<String> lines = new ArrayList<>();
        for (MoShortlistEntry entry : entries) {
            lines.add(serialize(entry));
        }
        try {
            Files.write(filePath, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write MO shortlist file.", exception);
        }
    }

    private Optional<MoShortlistEntry> parse(String line) {
        String[] parts = line.split(DELIMITER, -1);
        if (parts.length < 7) {
            return Optional.empty();
        }
        try {
            return Optional.of(new MoShortlistEntry(
                unescape(parts[0]),
                unescape(parts[1]),
                unescape(parts[2]),
                unescape(parts[3]),
                MoShortlistStatus.valueOf(unescape(parts[4])),
                unescape(parts[5]),
                LocalDateTime.parse(unescape(parts[6]))
            ));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private String serialize(MoShortlistEntry entry) {
        return String.join(DELIMITER,
            escape(entry.organiserId()),
            escape(entry.jobId()),
            escape(entry.applicationId()),
            escape(entry.applicantUserId()),
            escape(entry.status().name()),
            escape(entry.note()),
            escape(entry.updatedAt().toString())
        );
    }

    private void ensureFileExists() {
        try {
            Files.createDirectories(filePath.getParent());
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to initialize MO shortlist file.", exception);
        }
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
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
