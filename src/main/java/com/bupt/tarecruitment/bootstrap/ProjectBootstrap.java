package com.bupt.tarecruitment.bootstrap;

import com.bupt.tarecruitment.common.storage.DataFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ProjectBootstrap {
    public StartupReport initialize() {
        Path projectRoot = Path.of("").toAbsolutePath();
        Path dataDirectory = projectRoot.resolve("data");
        List<Path> createdFiles = new ArrayList<>();

        try {
            Files.createDirectories(dataDirectory);

            for (DataFile dataFile : DataFile.values()) {
                Path filePath = dataDirectory.resolve(dataFile.fileName());
                if (Files.notExists(filePath)) {
                    Files.write(filePath, dataFile.initialLines(), StandardCharsets.UTF_8);
                    createdFiles.add(filePath);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to initialize project data directory.", exception);
        }

        return new StartupReport(dataDirectory, List.copyOf(createdFiles));
    }
}
