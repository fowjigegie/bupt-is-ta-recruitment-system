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

    @Override
    public String saveCv(String userId, String cvContent) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank.");
        }

        if (cvContent == null || cvContent.isBlank()) {
            throw new IllegalArgumentException("cvContent must not be blank.");
        }

        String relativePath = Path.of("cvs", userId, "current.txt").toString().replace('\\', '/');
        Path targetPath = resolveCvPath(relativePath);

        try {
            Files.createDirectories(targetPath.getParent());
            Files.writeString(targetPath, cvContent, StandardCharsets.UTF_8);
            return relativePath;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save CV text for userId: " + userId, exception);
        }
    }

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

    private Path resolveCvPath(String relativePath) {
        Path cvsRoot = dataDirectory.resolve("cvs").normalize();
        Path resolvedPath = dataDirectory.resolve(relativePath).normalize();
        if (!resolvedPath.startsWith(cvsRoot)) {
            throw new IllegalArgumentException("CV path must stay inside data/cvs.");
        }
        return resolvedPath;
    }
}
