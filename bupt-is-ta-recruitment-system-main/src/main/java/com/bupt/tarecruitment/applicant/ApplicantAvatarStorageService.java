package com.bupt.tarecruitment.applicant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * 负责申请人头像文件的存储、删除与本地路径解析。
 */
public final class ApplicantAvatarStorageService {
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg");

    private final Path dataDirectory;
    private final Path avatarsDirectory;

    public ApplicantAvatarStorageService(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.avatarsDirectory = dataDirectory.resolve("avatars");
    }

    public String storeAvatar(String userId, Path sourcePath) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank.");
        }
        if (sourcePath == null || Files.notExists(sourcePath)) {
            throw new IllegalArgumentException("Selected avatar file does not exist.");
        }

        String extension = extractSupportedExtension(sourcePath);
        Path targetDirectory = avatarsDirectory.resolve(userId);
        Path targetPath = targetDirectory.resolve("profile" + extension);

        try {
            Files.createDirectories(targetDirectory);
            deleteAvatarVariants(targetDirectory);
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return dataDirectory.relativize(targetPath).toString().replace('\\', '/');
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save applicant avatar.", exception);
        }
    }

    public Optional<Path> resolveAvatarForUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }

        Path targetDirectory = avatarsDirectory.resolve(userId);
        for (String extension : SUPPORTED_EXTENSIONS) {
            Path candidate = targetDirectory.resolve("profile" + extension);
            if (Files.exists(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    public boolean hasAvatarForUser(String userId) {
        return resolveAvatarForUser(userId).isPresent();
    }

    public void deleteAvatarForUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }

        Path targetDirectory = avatarsDirectory.resolve(userId);
        try {
            deleteAvatarVariants(targetDirectory);
            Files.deleteIfExists(targetDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete applicant avatar.", exception);
        }
    }

    public void deleteAvatar(String avatarPath) {
        resolveStoredPath(avatarPath).ifPresent(path -> {
            try {
                Files.deleteIfExists(path);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to delete applicant avatar.", exception);
            }
        });
    }

    public Optional<Path> resolveAvatar(String avatarPath) {
        return resolveStoredPath(avatarPath).filter(Files::exists);
    }

    private Optional<Path> resolveStoredPath(String avatarPath) {
        if (avatarPath == null || avatarPath.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(dataDirectory.resolve(avatarPath.replace('\\', '/')));
    }

    private static String extractSupportedExtension(Path sourcePath) {
        String fileName = sourcePath.getFileName().toString().toLowerCase(Locale.ROOT);
        for (String extension : SUPPORTED_EXTENSIONS) {
            if (fileName.endsWith(extension)) {
                return extension;
            }
        }
        throw new IllegalArgumentException("Only .png, .jpg, and .jpeg avatar files are supported.");
    }

    private static void deleteAvatarVariants(Path targetDirectory) throws IOException {
        for (String extension : SUPPORTED_EXTENSIONS) {
            Files.deleteIfExists(targetDirectory.resolve("profile" + extension));
        }
    }
}
