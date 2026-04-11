package com.bupt.tarecruitment.applicant;

import com.bupt.tarecruitment.common.storage.DataFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ApplicantProfileRepositoryRoundTripTest {
    private ApplicantProfileRepositoryRoundTripTest() {
    }

    // 这组测试直接验证 repository 的读写闭环：
    // save 之后能否正确读回，以及再次 save 同一 userId 时是否会覆盖而不是重复新增。
    public static void main(String[] args) {
        try {
            Path tempDataDirectory = Files.createTempDirectory("profile-repository-roundtrip");
            Files.write(
                tempDataDirectory.resolve(DataFile.PROFILES.fileName()),
                List.of(DataFile.PROFILES.initialLines().getFirst()),
                StandardCharsets.UTF_8
            );

            TextFileApplicantProfileRepository repository = new TextFileApplicantProfileRepository(tempDataDirectory);

            ApplicantProfile created = new ApplicantProfile(
                "profile801",
                "ta801",
                "231228801",
                "Repository Demo Applicant",
                "Computer Science",
                2,
                "Not Graduated",
                List.of("Java", "Communication"),
                List.of("MON-09:00-11:00", "WED-14:00-16:00"),
                List.of("Teaching Assistant", "Lab Support")
            );
            repository.save(created);

            ApplicantProfile stored = repository.findByUserId("ta801")
                .orElseThrow(() -> new IllegalStateException("Saved profile was not found by userId."));
            assertEquals(created, stored, "Repository should preserve all profile fields after save.");

            ApplicantProfile updated = new ApplicantProfile(
                "profile801",
                "ta801",
                "231228801",
                "Repository Demo Applicant Updated",
                "Software Engineering",
                3,
                "Graduated",
                List.of("Python", "Leadership"),
                List.of("TUE-10:00-12:00"),
                List.of("Lab Support")
            );
            repository.save(updated);

            ApplicantProfile reloaded = repository.findByStudentId("231228801")
                .orElseThrow(() -> new IllegalStateException("Updated profile was not found by studentId."));
            assertEquals(updated, reloaded, "Repository should replace existing profile for the same userId.");
            assertEquals(1, repository.findAll().size(), "Repository update should not duplicate profile rows.");

            System.out.println("ApplicantProfileRepository round-trip test passed.");
            System.out.println("Temp data directory: " + tempDataDirectory);
        } catch (Exception exception) {
            throw new IllegalStateException("ApplicantProfileRepository round-trip test failed.", exception);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException(message + " Expected: " + expected + ", actual: " + actual);
        }
    }
}
