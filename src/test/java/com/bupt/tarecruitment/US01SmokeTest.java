package com.bupt.tarecruitment;

import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.applicant.ApplicantProfileService;
import com.bupt.tarecruitment.applicant.ApplicantProfileValidator;
import com.bupt.tarecruitment.applicant.TextFileApplicantProfileRepository;
import com.bupt.tarecruitment.auth.AuthService;
import com.bupt.tarecruitment.auth.AuthValidator;
import com.bupt.tarecruitment.auth.TextFileUserRepository;
import com.bupt.tarecruitment.auth.UserRepository;
import com.bupt.tarecruitment.auth.UserRole;
import com.bupt.tarecruitment.common.storage.DataFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class US01SmokeTest {
    private US01SmokeTest() {
    }

    public static void main(String[] args) {
        try {
            Path tempDataDirectory = Files.createTempDirectory("us01-smoke");
            Files.write(
                tempDataDirectory.resolve(DataFile.USERS.fileName()),
                List.of(DataFile.USERS.initialLines().getFirst()),
                StandardCharsets.UTF_8
            );
            Files.write(
                tempDataDirectory.resolve(DataFile.PROFILES.fileName()),
                List.of(DataFile.PROFILES.initialLines().getFirst()),
                StandardCharsets.UTF_8
            );

            UserRepository userRepository = new TextFileUserRepository(tempDataDirectory);
            AuthService authService = new AuthService(userRepository, new AuthValidator());
            authService.register("ta101", "pass101", UserRole.APPLICANT);
            authService.register("ta102", "pass102", UserRole.APPLICANT);
            authService.register("mo101", "pass201", UserRole.MO);

            ApplicantProfileService profileService = new ApplicantProfileService(
                new TextFileApplicantProfileRepository(tempDataDirectory),
                new ApplicantProfileValidator(),
                userRepository
            );

            ApplicantProfile createdProfile = new ApplicantProfile(
                "profile101",
                "ta101",
                "231221101",
                "Create Demo Applicant",
                "Software Engineering",
                2,
                "Not Graduated",
                List.of("Java", "Communication"),
                List.of("MON-09:00-11:00", "WED-14:00-16:00"),
                List.of("Teaching Assistant", "Lab Support")
            );

            ApplicantProfile savedProfile = profileService.createProfile(createdProfile);
            assertEquals("profile101", savedProfile.profileId(), "Profile creation returned the wrong profileId.");
            assertEquals("ta101", savedProfile.userId(), "Profile creation returned the wrong userId.");

            ApplicantProfile storedProfile = profileService.getProfileByUserId("ta101")
                .orElseThrow(() -> new IllegalStateException("Created profile was not persisted."));
            assertEquals("Create Demo Applicant", storedProfile.fullName(), "Stored full name is incorrect.");
            assertEquals("Software Engineering", storedProfile.programme(), "Stored programme is incorrect.");
            assertEquals(List.of("Java", "Communication"), storedProfile.skills(), "Stored skills are incorrect.");

            expectCreateFailure(profileService, createdProfile, "A profile already exists for userId");

            expectCreateFailure(
                profileService,
                new ApplicantProfile(
                    "profile102",
                    "ta102",
                    "231221101",
                    "Second Applicant",
                    "Computer Science",
                    3,
                    "Graduated",
                    List.of("Python"),
                    List.of("TUE-10:00-12:00"),
                    List.of("Lab Support")
                ),
                "studentId is already used by another applicant"
            );

            expectCreateFailure(
                profileService,
                new ApplicantProfile(
                    "profile103",
                    "ta102",
                    "231221103",
                    "",
                    "Computer Science",
                    2,
                    "Not Graduated",
                    List.of("Python"),
                    List.of("TUE-10:00-12:00"),
                    List.of("Lab Support")
                ),
                "fullName must not be blank."
            );

            expectCreateFailure(
                profileService,
                new ApplicantProfile(
                    "profile104",
                    "ghostApplicant",
                    "231221104",
                    "Ghost Applicant",
                    "Computer Science",
                    2,
                    "Not Graduated",
                    List.of("Python"),
                    List.of("TUE-10:00-12:00"),
                    List.of("Lab Support")
                ),
                "No registered user exists for userId"
            );

            expectCreateFailure(
                profileService,
                new ApplicantProfile(
                    "profile105",
                    "mo101",
                    "231221105",
                    "Module Organiser User",
                    "Computer Science",
                    2,
                    "Not Graduated",
                    List.of("Python"),
                    List.of("TUE-10:00-12:00"),
                    List.of("Lab Support")
                ),
                "is not an ACTIVE APPLICANT account"
            );

            System.out.println("US01 smoke test passed.");
            System.out.println("Temp data directory: " + tempDataDirectory);
        } catch (Exception exception) {
            throw new IllegalStateException("US01 smoke test failed.", exception);
        }
    }

    private static void expectCreateFailure(
        ApplicantProfileService profileService,
        ApplicantProfile profile,
        String expectedMessagePart
    ) {
        try {
            profileService.createProfile(profile);
            throw new IllegalStateException("Expected profile creation to fail for userId: " + profile.userId());
        } catch (IllegalArgumentException exception) {
            if (!exception.getMessage().contains(expectedMessagePart)) {
                throw new IllegalStateException("Unexpected profile creation message: " + exception.getMessage(), exception);
            }
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException(message + " Expected: " + expected + ", actual: " + actual);
        }
    }
}
