package com.bupt.tarecruitment.applicant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * File-backed storage for CV PDF attachments.
 */
public final class TextFileCvAttachmentStorage implements CvAttachmentStorage {
    private final Path dataDirectory;

    public TextFileCvAttachmentStorage() {
        this(Path.of("").toAbsolutePath().resolve("data"));
    }

    public TextFileCvAttachmentStorage(Path dataDirectory) {
        this.dataDirectory = Objects.requireNonNull(dataDirectory);
    }

    @Override
    public String savePdf(String applicantUserId, String cvId, byte[] pdfBytes) {
        if (applicantUserId == null || applicantUserId.isBlank()) {
            throw new IllegalArgumentException("applicantUserId must not be blank.");
        }
        if (cvId == null || cvId.isBlank()) {
            throw new IllegalArgumentException("cvId must not be blank.");
        }
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("pdfBytes must not be empty.");
        }

        String relativePath = Path.of("cvs", applicantUserId, cvId + ".pdf").toString().replace('\\', '/');
        Path targetPath = resolveCvPath(relativePath);
        try {
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, pdfBytes);
            return relativePath;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save CV PDF for cvId: " + cvId, exception);
        }
    }

    @Override
    public Optional<String> findPdfPath(String applicantUserId, String cvId) {
        if (applicantUserId == null || applicantUserId.isBlank() || cvId == null || cvId.isBlank()) {
            return Optional.empty();
        }
        String relativePath = Path.of("cvs", applicantUserId, cvId + ".pdf").toString().replace('\\', '/');
        Path targetPath = resolveCvPath(relativePath);
        return Files.exists(targetPath) ? Optional.of(relativePath) : Optional.empty();
    }

    @Override
    public byte[] loadPdf(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("relativePath must not be blank.");
        }
        Path targetPath = resolveCvPath(relativePath);
        if (Files.notExists(targetPath)) {
            throw new IllegalArgumentException("CV PDF file does not exist: " + relativePath);
        }
        try {
            return Files.readAllBytes(targetPath);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read CV PDF from: " + relativePath, exception);
        }
    }

    @Override
    public boolean deletePdf(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("relativePath must not be blank.");
        }
        Path targetPath = resolveCvPath(relativePath);
        try {
            return Files.deleteIfExists(targetPath);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete CV PDF from: " + relativePath, exception);
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
