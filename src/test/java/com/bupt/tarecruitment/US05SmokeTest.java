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

public final class US05SmokeTest {
    private US05SmokeTest() {
    }

    public static void main(String[] args) {
        try {
            Path tempDataDirectory = Files.createTempDirectory("us05-smoke");
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
            authService.register("ta201", "pass201", UserRole.APPLICANT);
            authService.register("ta202", "pass202", UserRole.APPLICANT);

            TextFileApplicantProfileRepository profileRepository = new TextFileApplicantProfileRepository(tempDataDirectory);
            ApplicantProfileService profileService = new ApplicantProfileService(
                profileRepository,
                new ApplicantProfileValidator(),
                userRepository
            );

            ApplicantProfile originalProfile = new ApplicantProfile(
                "profile201",
                "ta201",
                "231225201",
                "Edit Demo Applicant",
                "Software Engineering",
                2,
                "Not Graduated",
                List.of("Java"),
                List.of("MON-09:00-11:00"),
                List.of("Teaching Assistant")
            );
            profileService.createProfile(originalProfile);

            ApplicantProfile updatedProfile = new ApplicantProfile(
                "profile201",
                "ta201",
                "231225201",
                "Edited Demo Applicant",
                "Computer Science",
                3,
                "Graduated",
                List.of("Python", "Communication"),
                List.of("TUE-14:00-16:00", "FRI-10:00-12:00"),
                List.of("Teaching Assistant", "Lab Support")
            );

            ApplicantProfile savedProfile = profileService.updateProfile(updatedProfile);
            assertEquals("Edited Demo Applicant", savedProfile.fullName(), "Profile update did not return new full name.");
            assertEquals("Computer Science", savedProfile.programme(), "Profile update did not return new programme.");
            assertEquals(3, savedProfile.yearOfStudy(), "Profile update did not return new year of study.");

            ApplicantProfile storedProfile = profileService.getProfileByUserId("ta201")
                .orElseThrow(() -> new IllegalStateException("Updated profile was not persisted."));
            assertEquals("profile201", storedProfile.profileId(), "Profile ID should remain stable after edit.");
            assertEquals(List.of("Python", "Communication"), storedProfile.skills(), "Skills were not updated.");
            assertEquals(
                List.of("TUE-14:00-16:00", "FRI-10:00-12:00"),
                storedProfile.availabilitySlots(),
                "Availability was not updated."
            );
            assertEquals(
                List.of("Teaching Assistant", "Lab Support"),
                storedProfile.desiredPositions(),
                "Desired positions were not updated."
            );

            expectUpdateFailure(
                profileService,
                new ApplicantProfile(
                    "profile202",
                    "ta202",
                    "231225202",
                    "Missing Existing Profile",
                    "Computer Science",
                    2,
                    "Not Graduated",
                    List.of("Python"),
                    List.of("WED-09:00-11:00"),
                    List.of("Lab Support")
                ),
                "No profile exists for userId"
            );

            expectUpdateFailure(
                profileService,
                new ApplicantProfile(
                    "profile201",
                    "ta201",
                    "231225201",
                    "",
                    "Computer Science",
                    3,
                    "Graduated",
                    List.of("Python"),
                    List.of("TUE-14:00-16:00"),
                    List.of("Lab Support")
                ),
                "fullName must not be blank."
            );

            System.out.println("US05 smoke test passed.");
            System.out.println("Temp data directory: " + tempDataDirectory);
        } catch (Exception exception) {
            throw new IllegalStateException("US05 smoke test failed.", exception);
        }
    }

    private static void expectUpdateFailure(
        ApplicantProfileService profileService,
        ApplicantProfile profile,
        String expectedMessagePart
    ) {
        try {
            profileService.updateProfile(profile);
            throw new IllegalStateException("Expected profile update to fail for userId: " + profile.userId());
        } catch (IllegalArgumentException exception) {
            if (!exception.getMessage().contains(expectedMessagePart)) {
                throw new IllegalStateException("Unexpected profile update message: " + exception.getMessage(), exception);
            }
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException(message + " Expected: " + expected + ", actual: " + actual);
        }
    }
}
