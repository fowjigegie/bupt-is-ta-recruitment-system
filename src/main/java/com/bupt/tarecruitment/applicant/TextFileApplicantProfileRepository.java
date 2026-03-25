package com.bupt.tarecruitment.applicant;

import com.bupt.tarecruitment.common.storage.DataFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    @Override
    public Optional<ApplicantProfile> findByUserId(String userId) {
        return findAll().stream()
            .filter(profile -> profile.userId().equals(userId))
            .findFirst();
    }

    @Override
    public Optional<ApplicantProfile> findByStudentId(String studentId) {
        return findAll().stream()
            .filter(profile -> profile.studentId().equals(studentId))
            .findFirst();
    }

    @Override
    public List<ApplicantProfile> findAll() {
        try {
            List<ApplicantProfile> profiles = new ArrayList<>();

            for (String line : Files.readAllLines(profilesFilePath, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }

                profiles.add(parseLine(line));
            }

            return profiles;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read applicant profiles.", exception);
        }
    }

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
        String[] fields = line.split(FIELD_SEPARATOR, -1);
        if (fields.length != 11) {
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
            fields[10]
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

    private void writeAll(List<ApplicantProfile> profiles) {
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
            profile.cvFileName()
        );
    }

    private void ensureFileExists() {
        try {
            Files.createDirectories(profilesFilePath.getParent());
            if (Files.notExists(profilesFilePath)) {
                Files.write(profilesFilePath, DataFile.PROFILES.initialLines(), StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to prepare applicant profile storage.", exception);
        }
    }
}
