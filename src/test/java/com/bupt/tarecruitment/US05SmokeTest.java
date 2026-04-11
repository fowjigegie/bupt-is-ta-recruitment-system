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

    // 这组 smoke test 关注的是 US05 的完整编辑流程：
    // 先准备已有 profile，再执行 update，
    // 然后验证“更新后能读回、profileId 必须一致、studentId 不能撞车、非法账号不能更新”。
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
            authService.register("ta203", "pass203", UserRole.APPLICANT);
            authService.register("mo201", "pass301", UserRole.MO);

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
            profileService.createProfile(new ApplicantProfile(
                "profile202",
                "ta202",
                "231225202",
                "Second Applicant",
                "Computer Science",
                2,
                "Not Graduated",
                List.of("Python"),
                List.of("WED-09:00-11:00"),
                List.of("Lab Support")
            ));

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
                    "profile203",
                    "ta203",
                    "231225203",
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
                    "wrong-profile-id",
                    "ta201",
                    "231225201",
                    "Edited Demo Applicant",
                    "Computer Science",
                    3,
                    "Graduated",
                    List.of("Python"),
                    List.of("TUE-14:00-16:00"),
                    List.of("Lab Support")
                ),
                "profileId does not match the existing profile"
            );

            expectUpdateFailure(
                profileService,
                new ApplicantProfile(
                    "profile201",
                    "ta201",
                    "231225202",
                    "Edited Demo Applicant",
                    "Computer Science",
                    3,
                    "Graduated",
                    List.of("Python"),
                    List.of("TUE-14:00-16:00"),
                    List.of("Lab Support")
                ),
                "studentId is already used by another applicant"
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

            expectUpdateFailure(
                profileService,
                new ApplicantProfile(
                    "profile301",
                    "mo201",
                    "231225301",
                    "Module Organiser User",
                    "Computer Science",
                    2,
                    "Not Graduated",
                    List.of("Python"),
                    List.of("TUE-14:00-16:00"),
                    List.of("Lab Support")
                ),
                "is not an ACTIVE APPLICANT account"
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
