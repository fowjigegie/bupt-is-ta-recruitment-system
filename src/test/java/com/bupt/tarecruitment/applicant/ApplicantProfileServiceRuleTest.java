package com.bupt.tarecruitment.applicant;

import com.bupt.tarecruitment.auth.AccountStatus;
import com.bupt.tarecruitment.auth.UserAccount;
import com.bupt.tarecruitment.auth.UserRepository;
import com.bupt.tarecruitment.auth.UserRole;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 验证申请人画像服务中的业务规则。
 */
public final class ApplicantProfileServiceRuleTest {
    private ApplicantProfileServiceRuleTest() {
    }

    // 这组测试使用内存版 repository 和 userRepository，
    // 重点验证 ApplicantProfileService 的业务规则，而不是文件读写细节。
    public static void main(String[] args) {
        InMemoryApplicantProfileRepository profileRepository = new InMemoryApplicantProfileRepository();
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        ApplicantProfileService service = new ApplicantProfileService(
            profileRepository,
            new ApplicantProfileValidator(),
            userRepository
        );

        userRepository.save(activeUser("ta701", UserRole.APPLICANT));
        userRepository.save(activeUser("ta702", UserRole.APPLICANT));
        userRepository.save(activeUser("mo701", UserRole.MO));

        ApplicantProfile originalProfile = new ApplicantProfile(
            "profile701",
            "ta701",
            "231227701",
            "Service Rule Applicant",
            "Computer Science",
            2,
            "Not Graduated",
            List.of("Java"),
            List.of("MON-09:00-11:00"),
            List.of("Teaching Assistant")
        );

        ApplicantProfile savedProfile = service.createProfile(originalProfile);
        assertEquals("profile701", savedProfile.profileId(), "Service should return the saved profile.");
        assertEquals(1, profileRepository.findAll().size(), "Create should persist exactly one profile.");

        ApplicantProfile updatedProfile = new ApplicantProfile(
            "profile701",
            "ta701",
            "231227701",
            "Service Rule Applicant Updated",
            "Software Engineering",
            3,
            "Graduated",
            List.of("Python", "Communication"),
            List.of("TUE-14:00-16:00"),
            List.of("Lab Support")
        );
        ApplicantProfile updated = service.updateProfile(updatedProfile);
        assertEquals("Service Rule Applicant Updated", updated.fullName(), "Update should return the modified profile.");
        assertEquals("Software Engineering", profileRepository.findByUserId("ta701").orElseThrow().programme(), "Update should replace stored profile.");

        service.createProfile(new ApplicantProfile(
            "profile702",
            "ta702",
            "231227702",
            "Second Service Applicant",
            "Computer Science",
            2,
            "Not Graduated",
            List.of("SQL"),
            List.of("WED-10:00-12:00"),
            List.of("Lab Support")
        ));

        expectServiceFailure(
            () -> service.createProfile(originalProfile),
            "A profile already exists for userId"
        );

        expectServiceFailure(
            () -> service.updateProfile(new ApplicantProfile(
                "wrong-id",
                "ta701",
                "231227701",
                "Service Rule Applicant Updated",
                "Software Engineering",
                3,
                "Graduated",
                List.of("Python"),
                List.of("TUE-14:00-16:00"),
                List.of("Lab Support")
            )),
            "profileId does not match the existing profile"
        );

        expectServiceFailure(
            () -> service.updateProfile(new ApplicantProfile(
                "profile701",
                "ta701",
                "231227702",
                "Service Rule Applicant Updated",
                "Software Engineering",
                3,
                "Graduated",
                List.of("Python"),
                List.of("TUE-14:00-16:00"),
                List.of("Lab Support")
            )),
            "studentId is already used by another applicant"
        );

        expectServiceFailure(
            () -> service.createProfile(new ApplicantProfile(
                "profile703",
                "mo701",
                "231227703",
                "Module Organiser User",
                "Computer Science",
                2,
                "Not Graduated",
                List.of("Python"),
                List.of("THU-10:00-12:00"),
                List.of("Lab Support")
            )),
            "is not an ACTIVE APPLICANT account"
        );

        System.out.println("ApplicantProfileService rule test passed.");
    }

    private static UserAccount activeUser(String userId, UserRole role) {
        return new UserAccount(userId, "hash", role, userId, AccountStatus.ACTIVE);
    }

    private static void expectServiceFailure(Runnable action, String expectedMessagePart) {
        try {
            action.run();
            throw new IllegalStateException("Expected ApplicantProfileService action to fail.");
        } catch (IllegalArgumentException exception) {
            if (!exception.getMessage().contains(expectedMessagePart)) {
                throw new IllegalStateException("Unexpected service failure message: " + exception.getMessage(), exception);
            }
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException(message + " Expected: " + expected + ", actual: " + actual);
        }
    }

    private static final class InMemoryApplicantProfileRepository implements ApplicantProfileRepository {
        private final Map<String, ApplicantProfile> profilesByUserId = new LinkedHashMap<>();

        @Override
        public Optional<ApplicantProfile> findByUserId(String userId) {
            return Optional.ofNullable(profilesByUserId.get(userId));
        }

        @Override
        public Optional<ApplicantProfile> findByStudentId(String studentId) {
            return profilesByUserId.values().stream()
                .filter(profile -> profile.studentId().equals(studentId))
                .findFirst();
        }

        @Override
        public List<ApplicantProfile> findAll() {
            return new ArrayList<>(profilesByUserId.values());
        }

        @Override
        public void save(ApplicantProfile profile) {
            profilesByUserId.put(profile.userId(), profile);
        }
    }

    private static final class InMemoryUserRepository implements UserRepository {
        private final Map<String, UserAccount> usersById = new LinkedHashMap<>();

        @Override
        public Optional<UserAccount> findByUserId(String userId) {
            return Optional.ofNullable(usersById.get(userId));
        }

        @Override
        public List<UserAccount> findAll() {
            return new ArrayList<>(usersById.values());
        }

        @Override
        public void save(UserAccount userAccount) {
            usersById.put(userAccount.userId(), userAccount);
        }
    }
}
