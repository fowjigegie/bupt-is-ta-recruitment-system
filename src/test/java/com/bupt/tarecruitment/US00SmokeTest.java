package com.bupt.tarecruitment;

import com.bupt.tarecruitment.auth.AuthService;
import com.bupt.tarecruitment.auth.AuthValidator;
import com.bupt.tarecruitment.auth.TextFileUserRepository;
import com.bupt.tarecruitment.auth.UserAccount;
import com.bupt.tarecruitment.auth.UserRole;
import com.bupt.tarecruitment.auth.UserRepository;
import com.bupt.tarecruitment.common.storage.DataFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 覆盖 US00 场景的冒烟测试。
 */
public final class US00SmokeTest {
    private US00SmokeTest() {
    }

    public static void main(String[] args) {
        try {
            Path tempDataDirectory = Files.createTempDirectory("us00-smoke");
            Files.write(
                tempDataDirectory.resolve(DataFile.USERS.fileName()),
                List.of(DataFile.USERS.initialLines().getFirst()),
                StandardCharsets.UTF_8
            );

            UserRepository repository = new TextFileUserRepository(tempDataDirectory);
            AuthService service = new AuthService(repository, new AuthValidator());

            UserAccount applicantAccount = service.register("ta101", "pass101", UserRole.APPLICANT);
            assertEquals("ta101", applicantAccount.userId(), "Unexpected registered applicant userId.");
            assertEquals(UserRole.APPLICANT, applicantAccount.role(), "Unexpected registered applicant role.");
            assertTrue(repository.findByUserId("ta101").isPresent(), "Registered applicant was not saved.");

            UserAccount loggedInAccount = service.login("ta101", "pass101");
            assertEquals(UserRole.APPLICANT, loggedInAccount.role(), "Unexpected role after applicant login.");
            assertTrue(service.getCurrentUser().isPresent(), "Current user should exist after login.");
            assertEquals(
                UserRole.APPLICANT,
                service.getCurrentUser().orElseThrow().role(),
                "Current user role was not preserved after login."
            );

            service.logout();
            assertTrue(service.getCurrentUser().isEmpty(), "Current user should be cleared after logout.");

            UserAccount moAccount = service.register("mo101", "pass201", UserRole.MO);
            assertEquals(UserRole.MO, moAccount.role(), "Unexpected registered MO role.");
            UserAccount adminAccount = service.register("admin101", "pass301", UserRole.ADMIN);
            assertEquals(UserRole.ADMIN, adminAccount.role(), "Unexpected registered admin role.");

            expectRegisterFailure(service, "", "pass401", UserRole.APPLICANT, "userId must not be blank.");
            expectRegisterFailure(service, "ta401", "", UserRole.APPLICANT, "password must not be blank.");
            expectRegisterFailure(service, "ta401", "pass401", null, "role must not be null.");
            expectRegisterFailure(
                service,
                "staff401",
                "pass401",
                UserRole.MO,
                "userId format is invalid"
            );
            expectRegisterFailure(
                service,
                "ta101",
                "pass999",
                UserRole.APPLICANT,
                "A user already exists"
            );

            expectLoginFailure(service, "ta101", "wrong-password", "Invalid password");
            expectLoginFailure(service, "ta999", "pass999", "No user exists");
            expectLoginFailure(service, "", "pass999", "userId must not be blank.");
            expectLoginFailure(service, "ta101", "", "password must not be blank.");

            System.out.println("US00 smoke test passed.");
            System.out.println("Temp data directory: " + tempDataDirectory);
        } catch (Exception exception) {
            throw new IllegalStateException("US00 smoke test failed.", exception);
        }
    }

    private static void expectRegisterFailure(
        AuthService service,
        String userId,
        String password,
        UserRole role,
        String expectedMessagePart
    ) {
        try {
            service.register(userId, password, role);
            throw new IllegalStateException("Expected register failure but operation succeeded.");
        } catch (IllegalArgumentException exception) {
            if (!exception.getMessage().contains(expectedMessagePart)) {
                throw new IllegalStateException("Unexpected register failure message: " + exception.getMessage(), exception);
            }
        }
    }

    private static void expectLoginFailure(
        AuthService service,
        String userId,
        String password,
        String expectedMessagePart
    ) {
        try {
            service.login(userId, password);
            throw new IllegalStateException("Expected login failure but operation succeeded.");
        } catch (IllegalArgumentException exception) {
            if (!exception.getMessage().contains(expectedMessagePart)) {
                throw new IllegalStateException("Unexpected login failure message: " + exception.getMessage(), exception);
            }
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException(message + " Expected: " + expected + ", actual: " + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
