package com.bupt.tarecruitment.applicant;

import com.bupt.tarecruitment.common.storage.DataFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 基于文本文件存储申请人简历。
 */
public final class TextFileApplicantCvRepository implements ApplicantCvRepository {
    private static final String FIELD_SEPARATOR = "\\|";
    private static final String OUTPUT_SEPARATOR = "|";

    private final Path cvsFilePath;

    public TextFileApplicantCvRepository() {
        this(Path.of("").toAbsolutePath().resolve("data"));
    }

    public TextFileApplicantCvRepository(Path dataDirectory) {
        this.cvsFilePath = dataDirectory.resolve(DataFile.CVS.fileName());
        ensureFileExists();
    }

    // 按 cvId 查 metadata，常用于加载单个 CV、申请时回填当前选中的 CV。
    @Override
    public Optional<ApplicantCv> findByCvId(String cvId) {
        return findAll().stream()
            .filter(applicantCv -> applicantCv.cvId().equals(cvId))
            .findFirst();
    }

    // 列出某个 applicant 拥有的所有 CV 元数据。
    @Override
    public List<ApplicantCv> findByOwnerUserId(String ownerUserId) {
        return findAll().stream()
            .filter(applicantCv -> applicantCv.ownerUserId().equals(ownerUserId))
            .toList();
    }

    // 读取 data/cvs.txt 里的全部 metadata。
    // 注意：这里读出来的是"CV 信息索引"，不包含真正的正文内容。
    @Override
    public List<ApplicantCv> findAll() {
        try {
            List<ApplicantCv> applicantCvs = new ArrayList<>();

            for (String line : Files.readAllLines(cvsFilePath, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }

                applicantCvs.add(parseLine(line));
            }

            return applicantCvs;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read applicant CV metadata.", exception);
        }
    }

    // save 采用"同 cvId 覆盖、否则追加"的策略。
    @Override
    public void save(ApplicantCv applicantCv) {
        List<ApplicantCv> applicantCvs = new ArrayList<>(findAll());
        boolean replaced = false;

        for (int index = 0; index < applicantCvs.size(); index++) {
            if (applicantCvs.get(index).cvId().equals(applicantCv.cvId())) {
                applicantCvs.set(index, applicantCv);
                replaced = true;
                break;
            }
        }

        if (!replaced) {
            applicantCvs.add(applicantCv);
        }

        writeAll(applicantCvs);
    }

    // data/cvs.txt 每行 6 个字段，顺序必须和 formatLine 完全一致。
    @Override
    public boolean deleteByCvId(String cvId) {
        if (cvId == null || cvId.isBlank()) {
            throw new IllegalArgumentException("cvId must not be blank.");
        }

        List<ApplicantCv> remainingCvs = new ArrayList<>();
        boolean deleted = false;

        for (ApplicantCv applicantCv : findAll()) {
            if (applicantCv.cvId().equals(cvId.trim())) {
                deleted = true;
                continue;
            }
            remainingCvs.add(applicantCv);
        }

        if (deleted) {
            writeAll(remainingCvs);
        }
        return deleted;
    }

    private ApplicantCv parseLine(String line) {
        String[] fields = line.split(FIELD_SEPARATOR, -1);
        if (fields.length != 6) {
            throw new IllegalStateException("Invalid applicant CV metadata record: " + line);
        }

        return new ApplicantCv(
            fields[0],
            fields[1],
            fields[2],
            fields[3],
            LocalDateTime.parse(fields[4]),
            LocalDateTime.parse(fields[5])
        );
    }

    // 整个 metadata 文件重写一遍，保持 txt 存储实现简单直观。
    private void writeAll(List<ApplicantCv> applicantCvs) {
        List<String> lines = new ArrayList<>();
        lines.add(DataFile.CVS.initialLines().getFirst());

        for (ApplicantCv applicantCv : applicantCvs) {
            lines.add(formatLine(applicantCv));
        }

        try {
            Files.write(cvsFilePath, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write applicant CV metadata.", exception);
        }
    }

    // 把 metadata 对象转回文本文件的一行。
    private String formatLine(ApplicantCv applicantCv) {
        return String.join(
            OUTPUT_SEPARATOR,
            applicantCv.cvId(),
            applicantCv.ownerUserId(),
            applicantCv.title(),
            applicantCv.fileName(),
            applicantCv.createdAt().toString(),
            applicantCv.updatedAt().toString()
        );
    }

    // 第一次启动或测试时，如果 data/cvs.txt 不存在，就自动创建带表头的空文件。
    private void ensureFileExists() {
        try {
            Files.createDirectories(cvsFilePath.getParent());
            if (Files.notExists(cvsFilePath)) {
                Files.write(cvsFilePath, DataFile.CVS.initialLines(), StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to prepare applicant CV metadata storage.", exception);
        }
    }
}
