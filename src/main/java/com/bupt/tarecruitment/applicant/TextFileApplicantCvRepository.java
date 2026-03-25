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

    @Override
    public Optional<ApplicantCv> findByCvId(String cvId) {
        return findAll().stream()
            .filter(applicantCv -> applicantCv.cvId().equals(cvId))
            .findFirst();
    }

    @Override
    public List<ApplicantCv> findByOwnerUserId(String ownerUserId) {
        return findAll().stream()
            .filter(applicantCv -> applicantCv.ownerUserId().equals(ownerUserId))
            .toList();
    }

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
