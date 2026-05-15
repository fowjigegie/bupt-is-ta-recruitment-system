package com.bupt.tarecruitment.applicant;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bupt.tarecruitment.common.storage.DataFile;

/**
 * 基于文本文件存储申请人画像。
 */
public final class TextFileApplicantProfileRepository implements ApplicantProfileRepository {
    private static final String FIELD_SEPARATOR = "\\|";
    private static final String OUTPUT_SEPARATOR = "|";
    private static final String LIST_SEPARATOR = ";";

    private final Path profilesFilePath;

    public TextFileApplicantProfileRepository() {
        this(Path.of("").toAbsolutePath().resolve("data"));
    }

    public TextFileApplicantProfileRepository(Path dataDirectory) {
        this.profilesFilePath = dataDirectory.resolve(DataFile.PROFILES.fileName());
        ensureFileExists();
    }

    // 按 userId 查 profile。
    @Override
    public Optional<ApplicantProfile> findByUserId(String userId) {
        return findAll().stream()
            .filter(profile -> profile.userId().equals(userId))
            .findFirst();
    }

    // 按 studentId 查 profile，供 service 做学号唯一性检查。
    @Override
    public Optional<ApplicantProfile> findByStudentId(String studentId) {
        return findAll().stream()
            .filter(profile -> profile.studentId().equals(studentId))
            .findFirst();
    }

    // 从 data/profiles.txt 读取全部 profile。
    @Override
    public List<ApplicantProfile> findAll() {
        try {
            List<ApplicantProfile> profiles = new ArrayList<>();

            for (String line : Files.readAllLines(profilesFilePath, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }

                // 每一行解析成一个 ApplicantProfile 对象。
                profiles.add(parseLine(line));
            }

            return profiles;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read applicant profiles.", exception);
        }
    }

    // 文本存储实现里，默认按 userId 覆盖保存。
    // 所以同一个 applicant 最终只会在文件里保留一条 profile 记录。
    @Override
    public void save(ApplicantProfile profile) {
        List<ApplicantProfile> profiles = new ArrayList<>(findAll());
        boolean replaced = false;

        for (int index = 0; index < profiles.size(); index++) {
            if (profiles.get(index).userId().equals(profile.userId())) {
                profiles.set(index, profile);
                replaced = true;
                break;
            }
        }

        if (!replaced) {
            profiles.add(profile);
        }

        writeAll(profiles);
    }

    private ApplicantProfile parseLine(String line) {
        // profiles.txt 每一行的字段顺序必须和 formatLine(...) 保持一致。
        String[] fields = line.split(FIELD_SEPARATOR, -1);
        if (fields.length != 10 && fields.length != 11) {
            throw new IllegalStateException("Invalid applicant profile record: " + line);
        }

        return new ApplicantProfile(
            fields[0],
            fields[1],
            fields[2],
            fields[3],
            fields[4],
            Integer.parseInt(fields[5]),
            fields[6],
            parseList(fields[7]),
            parseList(fields[8]),
            parseList(fields[9]),
            fields.length == 11 ? fields[10] : ""
        );
    }

    private List<String> parseList(String fieldValue) {
        if (fieldValue == null || fieldValue.isBlank()) {
            return List.of();
        }

        // skills / availability / desiredPositions 都用 ";" 做分隔。
        String[] values = fieldValue.split(LIST_SEPARATOR);
        List<String> parsedValues = new ArrayList<>();
        for (String value : values) {
            if (!value.isBlank()) {
                parsedValues.add(value.trim());
            }
        }
        return parsedValues;
    }

    private void writeAll(List<ApplicantProfile> profiles) {
        // 这里采用整文件重写，逻辑简单，适合当前项目的文本存储方式。
        List<String> lines = new ArrayList<>();
        lines.add(DataFile.PROFILES.initialLines().getFirst());

        for (ApplicantProfile profile : profiles) {
            lines.add(formatLine(profile));
        }

        try {
            Files.write(profilesFilePath, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write applicant profiles.", exception);
        }
    }

    private String formatLine(ApplicantProfile profile) {
        // 注意输出字段顺序：必须与 parseLine(...) 一致。
        return String.join(
            OUTPUT_SEPARATOR,
            profile.profileId(),
            profile.userId(),
            profile.studentId(),
            profile.fullName(),
            profile.programme(),
            Integer.toString(profile.yearOfStudy()),
            profile.educationLevel(),
            String.join(LIST_SEPARATOR, profile.skills()),
            String.join(LIST_SEPARATOR, profile.availabilitySlots()),
            String.join(LIST_SEPARATOR, profile.desiredPositions()),
            profile.avatarPath()
        );
    }

    private void ensureFileExists() {
        try {
            Files.createDirectories(profilesFilePath.getParent());
            if (Files.notExists(profilesFilePath)) {
                // 第一次运行时自动创建空文件并写入表头。
                Files.write(profilesFilePath, DataFile.PROFILES.initialLines(), StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to prepare applicant profile storage.", exception);
        }
    }
}
