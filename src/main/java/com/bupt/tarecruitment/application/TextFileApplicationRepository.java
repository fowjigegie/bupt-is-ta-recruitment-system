package com.bupt.tarecruitment.application;

import com.bupt.tarecruitment.common.storage.DataFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TextFileApplicationRepository implements ApplicationRepository {
    private static final String FIELD_SEPARATOR = "\\|";
    private static final String OUTPUT_SEPARATOR = "|";

    private final Path applicationsFilePath;

    public TextFileApplicationRepository() {
        this(Path.of("").toAbsolutePath().resolve("data"));
    }

    public TextFileApplicationRepository(Path dataDirectory) {
        this.applicationsFilePath = dataDirectory.resolve(DataFile.APPLICATIONS.fileName());
        ensureFileExists();
    }

    @Override
    public Optional<JobApplication> findByApplicationId(String applicationId) {
        return findAll().stream()
            .filter(application -> application.applicationId().equals(applicationId))
            .findFirst();
    }

    @Override
    public List<JobApplication> findByApplicantUserId(String applicantUserId) {
        return findAll().stream()
            .filter(application -> application.applicantUserId().equals(applicantUserId))
            .toList();
    }

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
            fields[6]
        );
    }

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

    private String formatLine(JobApplication application) {
        return String.join(
            OUTPUT_SEPARATOR,
            application.applicationId(),
            application.jobId(),
            application.applicantUserId(),
            application.cvId(),
            application.status().name(),
            application.submittedAt().toString(),
            application.reviewerNote()
        );
    }

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
