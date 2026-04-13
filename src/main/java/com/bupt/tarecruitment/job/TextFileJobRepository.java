package com.bupt.tarecruitment.job;

import com.bupt.tarecruitment.common.storage.DataFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 基于文本文件存储岗位信息。
 */
public final class TextFileJobRepository implements JobRepository {
    private static final String FIELD_SEPARATOR = "\\|";
    private static final String OUTPUT_SEPARATOR = "|";
    private static final String LIST_SEPARATOR = ";";

    private final Path jobsFilePath;

    public TextFileJobRepository() {
        this(Path.of("").toAbsolutePath().resolve("data"));
    }

    public TextFileJobRepository(Path dataDirectory) {
        this.jobsFilePath = dataDirectory.resolve(DataFile.JOBS.fileName());
        ensureFileExists();
    }

    @Override
    public Optional<JobPosting> findByJobId(String jobId) {
        return findAll().stream()
            .filter(job -> job.jobId().equals(jobId))
            .findFirst();
    }

    @Override
    public List<JobPosting> findAll() {
        try {
            List<JobPosting> jobs = new ArrayList<>();
            for (String line : Files.readAllLines(jobsFilePath, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                jobs.add(parseLine(line));
            }
            return jobs;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read jobs.", exception);
        }
    }

    @Override
    public void save(JobPosting jobPosting) {
        List<JobPosting> jobs = new ArrayList<>(findAll());
        boolean replaced = false;

        for (int index = 0; index < jobs.size(); index++) {
            if (jobs.get(index).jobId().equals(jobPosting.jobId())) {
                jobs.set(index, jobPosting);
                replaced = true;
                break;
            }
        }

        if (!replaced) {
            jobs.add(jobPosting);
        }

        writeAll(jobs);
    }

    private JobPosting parseLine(String line) {
        String[] fields = line.split(FIELD_SEPARATOR, -1);
        if (fields.length != 9) {
            throw new IllegalStateException("Invalid job record: " + line);
        }

        return new JobPosting(
            fields[0],
            fields[1],
            fields[2],
            fields[3],
            fields[4],
            parseList(fields[5]),
            Integer.parseInt(fields[6]),
            parseList(fields[7]),
            JobStatus.valueOf(fields[8])
        );
    }

    private List<String> parseList(String fieldValue) {
        if (fieldValue == null || fieldValue.isBlank()) {
            return List.of();
        }

        String[] values = fieldValue.split(LIST_SEPARATOR);
        List<String> parsedValues = new ArrayList<>();
        for (String value : values) {
            if (!value.isBlank()) {
                parsedValues.add(value.trim());
            }
        }
        return parsedValues;
    }

    private void writeAll(List<JobPosting> jobs) {
        List<String> lines = new ArrayList<>();
        lines.add(DataFile.JOBS.initialLines().getFirst());

        for (JobPosting job : jobs) {
            lines.add(formatLine(job));
        }

        try {
            Files.write(jobsFilePath, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write jobs.", exception);
        }
    }

    private String formatLine(JobPosting job) {
        return String.join(
            OUTPUT_SEPARATOR,
            job.jobId(),
            job.organiserId(),
            job.title(),
            job.moduleOrActivity(),
            job.description(),
            String.join(LIST_SEPARATOR, job.requiredSkills()),
            Integer.toString(job.weeklyHours()),
            String.join(LIST_SEPARATOR, job.scheduleSlots()),
            job.status().name()
        );
    }

    private void ensureFileExists() {
        try {
            Files.createDirectories(jobsFilePath.getParent());
            if (Files.notExists(jobsFilePath)) {
                Files.write(jobsFilePath, DataFile.JOBS.initialLines(), StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to prepare job storage.", exception);
        }
    }
}

