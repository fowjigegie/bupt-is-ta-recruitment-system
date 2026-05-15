package com.bupt.tarecruitment.application;

import com.bupt.tarecruitment.common.storage.DataFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * 基于文本文件存储岗位申请。
 */
public final class TextFileApplicationRepository implements ApplicationRepository {
    private static final String FIELD_SEPARATOR = "\\|";
    private static final String OUTPUT_SEPARATOR = "|";
    private static final String REVIEWER_NOTE_PREFIX = "b64:";

    private final Path applicationsFilePath;

    public TextFileApplicationRepository() {
        this(Path.of("").toAbsolutePath().resolve("data"));
    }

    public TextFileApplicationRepository(Path dataDirectory) {
        this.applicationsFilePath = dataDirectory.resolve(DataFile.APPLICATIONS.fileName());
        ensureFileExists();
    }

    // 按 applicationId 查单条申请，常用于审核或更新状态后再保存。
    @Override
    public Optional<JobApplication> findByApplicationId(String applicationId) {
        return findAll().stream()
            .filter(application -> application.applicationId().equals(applicationId))
            .findFirst();
    }

    // US06 的核心读取入口：状态页就是通过 applicantUserId 把自己的申请全查出来。
    @Override
    public List<JobApplication> findByApplicantUserId(String applicantUserId) {
        return findAll().stream()
            .filter(application -> application.applicantUserId().equals(applicantUserId))
            .toList();
    }

    // 从 data/applications.txt 读取全部申请记录。
    @Override
    public List<JobApplication> findAll() {
        try {
            List<JobApplication> applications = new ArrayList<>();

            for (String line : Files.readAllLines(applicationsFilePath, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }

                applications.add(parseLine(line));
            }

            return applications;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read job applications.", exception);
        }
    }

    // save 采用"同 applicationId 覆盖，否则追加"的模式。
    @Override
    public void save(JobApplication application) {
        List<JobApplication> applications = new ArrayList<>(findAll());
        boolean replaced = false;

        for (int index = 0; index < applications.size(); index++) {
            if (applications.get(index).applicationId().equals(application.applicationId())) {
                applications.set(index, application);
                replaced = true;
                break;
            }
        }

        if (!replaced) {
            applications.add(application);
        }

        writeAll(applications);
    }

    // applications.txt 每行固定 7 个字段。
    // 第 7 个字段是 reviewerNote，为了防止分隔符冲突，这里会做编码/解码。
    private JobApplication parseLine(String line) {
        String[] fields = line.split(FIELD_SEPARATOR, -1);
        if (fields.length != 7) {
            throw new IllegalStateException("Invalid application record: " + line);
        }

        return new JobApplication(
            fields[0],
            fields[1],
            fields[2],
            fields[3],
            ApplicationStatus.valueOf(fields[4]),
            LocalDateTime.parse(fields[5]),
            decodeReviewerNote(fields[6])
        );
    }

    // 重新写出整份 applications.txt，保证文本存储和内存对象保持一致。
    private void writeAll(List<JobApplication> applications) {
        List<String> lines = new ArrayList<>();
        lines.add(DataFile.APPLICATIONS.initialLines().getFirst());

        for (JobApplication application : applications) {
            lines.add(formatLine(application));
        }

        try {
            Files.write(applicationsFilePath, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write job applications.", exception);
        }
    }

    // 把申请对象序列化成一行文本。
    private String formatLine(JobApplication application) {
        return String.join(
            OUTPUT_SEPARATOR,
            application.applicationId(),
            application.jobId(),
            application.applicantUserId(),
            application.cvId(),
            application.status().name(),
            application.submittedAt().toString(),
            encodeReviewerNote(application.reviewerNote())
        );
    }

    // reviewerNote 可能带空格、换行或特殊字符，因此用 b64: 前缀加 Base64 保存。
    private String encodeReviewerNote(String reviewerNote) {
        String normalizedNote = reviewerNote == null ? "" : reviewerNote;
        return REVIEWER_NOTE_PREFIX + Base64.getEncoder().encodeToString(normalizedNote.getBytes(StandardCharsets.UTF_8));
    }

    // 读取时把编码后的 reviewerNote 再还原回普通字符串。
    private String decodeReviewerNote(String rawValue) {
        if (!rawValue.startsWith(REVIEWER_NOTE_PREFIX)) {
            return rawValue;
        }

        try {
            byte[] decodedBytes = Base64.getDecoder().decode(rawValue.substring(REVIEWER_NOTE_PREFIX.length()));
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid encoded reviewer note.", exception);
        }
    }

    // 启动时保证 data/applications.txt 至少存在，并写入表头。
    private void ensureFileExists() {
        try {
            Files.createDirectories(applicationsFilePath.getParent());
            if (Files.notExists(applicationsFilePath)) {
                Files.write(applicationsFilePath, DataFile.APPLICATIONS.initialLines(), StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to prepare application storage.", exception);
        }
    }
}
