package com.bupt.tarecruitment.bootstrap;

import com.bupt.tarecruitment.common.storage.DataFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 负责组装仓库、服务和启动上下文。
 */
public final class ProjectBootstrap {
    private static final List<String> SAMPLE_CV_ONE_LINES = List.of(
        "Demo Applicant",
        "Software Engineering undergraduate with Java and communication skills.",
        "Interested in teaching assistant and invigilation roles."
    );
    private static final List<String> SAMPLE_CV_TWO_LINES = List.of(
        "Demo Applicant",
        "Computer Science focused CV tailored for tutorial support.",
        "Highlights programming fundamentals and student-facing communication."
    );
    private static final List<String> SAMPLE_CV_THREE_LINES = List.of(
        "Apply Demo Applicant",
        "Computer Science applicant available for tutorials and lab support.",
        "Comfortable with Python, communication, and student-facing support work."
    );

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

            Path sampleCvOnePath = dataDirectory.resolve("cvs").resolve("ta001").resolve("cv001.txt");
            Files.createDirectories(sampleCvOnePath.getParent());
            if (Files.notExists(sampleCvOnePath)) {
                Files.write(sampleCvOnePath, SAMPLE_CV_ONE_LINES, StandardCharsets.UTF_8);
                createdFiles.add(sampleCvOnePath);
            }

            Path sampleCvTwoPath = dataDirectory.resolve("cvs").resolve("ta001").resolve("cv002.txt");
            Files.createDirectories(sampleCvTwoPath.getParent());
            if (Files.notExists(sampleCvTwoPath)) {
                Files.write(sampleCvTwoPath, SAMPLE_CV_TWO_LINES, StandardCharsets.UTF_8);
                createdFiles.add(sampleCvTwoPath);
            }

            Path sampleCvThreePath = dataDirectory.resolve("cvs").resolve("ta002").resolve("cv004.txt");
            Files.createDirectories(sampleCvThreePath.getParent());
            if (Files.notExists(sampleCvThreePath)) {
                Files.write(sampleCvThreePath, SAMPLE_CV_THREE_LINES, StandardCharsets.UTF_8);
                createdFiles.add(sampleCvThreePath);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to initialize project data directory.", exception);
        }

        return new StartupReport(dataDirectory, List.copyOf(createdFiles));
    }
}
