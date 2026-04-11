package com.bupt.tarecruitment.applicant;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class TextFileCvStorage implements CvTextStorage {
    private final Path dataDirectory;

    public TextFileCvStorage() {
        this(Path.of("").toAbsolutePath().resolve("data"));
    }

    public TextFileCvStorage(Path dataDirectory) {
        this.dataDirectory = Objects.requireNonNull(dataDirectory);
    }

    // 真正把整份 CV 文本落到 data/cvs/<applicant>/<cvId>.txt。
    // metadata 里只保存这个相对路径，不直接塞进 data/cvs.txt。
    @Override
    public String saveCv(String applicantUserId, String cvId, String cvContent) {
        if (applicantUserId == null || applicantUserId.isBlank()) {
            throw new IllegalArgumentException("applicantUserId must not be blank.");
        }

        if (cvId == null || cvId.isBlank()) {
            throw new IllegalArgumentException("cvId must not be blank.");
        }

        if (cvContent == null || cvContent.isBlank()) {
            throw new IllegalArgumentException("cvContent must not be blank.");
        }

        // 每个 applicant 都有自己的子目录，便于后续按人管理多份 CV。
        String relativePath = Path.of("cvs", applicantUserId, cvId + ".txt").toString().replace('\\', '/');
        Path targetPath = resolveCvPath(relativePath);

        try {
            Files.createDirectories(targetPath.getParent());
            Files.writeString(targetPath, cvContent, StandardCharsets.UTF_8);
            return relativePath;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save CV text for cvId: " + cvId, exception);
        }
    }

    // 通过 metadata 保存的相对路径，把 CV 正文读回内存。
    @Override
    public String loadCv(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("relativePath must not be blank.");
        }

        Path targetPath = resolveCvPath(relativePath);
        if (Files.notExists(targetPath)) {
            throw new IllegalArgumentException("CV file does not exist: " + relativePath);
        }

        try {
            return Files.readString(targetPath, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read CV text from: " + relativePath, exception);
        }
    }

    // 做一次路径越界保护，确保读写都只能发生在 data/cvs/ 目录之内。
    private Path resolveCvPath(String relativePath) {
        Path cvsRoot = dataDirectory.resolve("cvs").normalize();
        Path resolvedPath = dataDirectory.resolve(relativePath).normalize();
        if (!resolvedPath.startsWith(cvsRoot)) {
            throw new IllegalArgumentException("CV path must stay inside data/cvs.");
        }
        return resolvedPath;
    }
}
