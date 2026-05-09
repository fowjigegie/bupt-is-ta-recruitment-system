package com.bupt.tarecruitment.applicant;

import com.bupt.tarecruitment.auth.AccountStatus;
import com.bupt.tarecruitment.auth.UserAccount;
import com.bupt.tarecruitment.auth.UserRepository;
import com.bupt.tarecruitment.auth.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)

/**
 * 验证申请人画像更新流程的单元行为。
 */
class ApplicantProfileServiceUpdateUnitTest {

    @Mock
    private ApplicantProfileRepository profileRepository;

    @Mock
    private UserRepository userRepository;

    private ApplicantProfileService service;

    @BeforeEach
    void setUp() {
        service = new ApplicantProfileService(
            profileRepository,
            new ApplicantProfileValidator(),
            userRepository
        );
    }

    // 正常更新流程：
    // userId 能找到旧 profile，且新旧 profileId 一致时，允许覆盖保存。
    @Test
    void shouldUpdateProfileWhenExistingRecordMatchesUserAndProfileId() {
        ApplicantProfile existing = profile("profile951", "ta951", "231229951", "Original Name");
        ApplicantProfile updated = profile("profile951", "ta951", "231229951", "Updated Name");

        when(userRepository.findByUserId("ta951")).thenReturn(Optional.of(activeApplicant("ta951")));
        when(profileRepository.findByUserId("ta951")).thenReturn(Optional.of(existing));
        when(profileRepository.findByStudentId("231229951")).thenReturn(Optional.of(existing));

        ApplicantProfile saved = service.updateProfile(updated);

        assertSame(updated, saved);
        verify(profileRepository).save(updated);
        verify(userRepository).save(new UserAccount(
            "ta951",
            "hash",
            UserRole.APPLICANT,
            "Updated Name",
            AccountStatus.ACTIVE
        ));
    }

    // update 不是 create，如果旧记录都不存在，就不能更新。
    @Test
    void shouldRejectUpdateWhenNoExistingProfileIsFound() {
        ApplicantProfile updated = profile("profile951", "ta951", "231229951", "Updated Name");

        when(userRepository.findByUserId("ta951")).thenReturn(Optional.of(activeApplicant("ta951")));
        when(profileRepository.findByUserId("ta951")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.updateProfile(updated)
        );

        assertEquals("No profile exists for userId: ta951", exception.getMessage());
        verify(profileRepository, never()).save(updated);
    }

    // profileId 必须和现有 profile 对得上，防止把别人的记录误改掉。
    @Test
    void shouldRejectUpdateWhenProfileIdDoesNotMatchExistingProfile() {
        ApplicantProfile existing = profile("profile951", "ta951", "231229951", "Original Name");
        ApplicantProfile updated = profile("wrong-id", "ta951", "231229951", "Updated Name");

        when(userRepository.findByUserId("ta951")).thenReturn(Optional.of(activeApplicant("ta951")));
        when(profileRepository.findByUserId("ta951")).thenReturn(Optional.of(existing));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.updateProfile(updated)
        );

        assertEquals(
            "profileId does not match the existing profile for userId: ta951",
            exception.getMessage()
        );
        verify(profileRepository, never()).save(updated);
    }

    // 编辑时也要继续保证 studentId 全局唯一。
    @Test
    void shouldRejectUpdateWhenStudentIdBelongsToAnotherApplicant() {
        ApplicantProfile existing = profile("profile951", "ta951", "231229951", "Original Name");
        ApplicantProfile otherApplicant = profile("profile952", "ta952", "231229952", "Other Applicant");
        ApplicantProfile updated = profile("profile951", "ta951", "231229952", "Updated Name");

        when(userRepository.findByUserId("ta951")).thenReturn(Optional.of(activeApplicant("ta951")));
        when(profileRepository.findByUserId("ta951")).thenReturn(Optional.of(existing));
        when(profileRepository.findByStudentId("231229952")).thenReturn(Optional.of(otherApplicant));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.updateProfile(updated)
        );

        assertEquals(
            "studentId is already used by another applicant: 231229952",
            exception.getMessage()
        );
        verify(profileRepository, never()).save(updated);
    }

    // 只有 ACTIVE APPLICANT 可以更新自己的 profile。
    @Test
    void shouldRejectUpdateWhenUserRoleIsNotApplicant() {
        ApplicantProfile updated = profile("profile951", "mo951", "231229951", "Updated Name");

        when(userRepository.findByUserId("mo951")).thenReturn(Optional.of(activeMo("mo951")));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.updateProfile(updated)
        );

        assertEquals("userId mo951 is not an ACTIVE APPLICANT account.", exception.getMessage());
        verify(profileRepository, never()).save(updated);
    }

    // 被禁用的 applicant 账号也不能继续更新资料。
    @Test
    void shouldRejectUpdateWhenApplicantAccountIsDisabled() {
        ApplicantProfile updated = profile("profile951", "ta951", "231229951", "Updated Name");

        when(userRepository.findByUserId("ta951")).thenReturn(Optional.of(disabledApplicant("ta951")));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.updateProfile(updated)
        );

        assertEquals("User account is not active for userId: ta951", exception.getMessage());
        verify(profileRepository, never()).save(updated);
    }

    private static ApplicantProfile profile(String profileId, String userId, String studentId, String fullName) {
        return new ApplicantProfile(
            profileId,
            userId,
            studentId,
            fullName,
            "Software Engineering",
            3,
            "Not Graduated",
            List.of("Java", "Communication"),
            List.of("TUE-14:00-16:00"),
            List.of("Teaching Assistant", "Lab Support")
        );
    }

    private static UserAccount activeApplicant(String userId) {
        return new UserAccount(userId, "hash", UserRole.APPLICANT, "Applicant " + userId, AccountStatus.ACTIVE);
    }

    private static UserAccount activeMo(String userId) {
        return new UserAccount(userId, "hash", UserRole.MO, "MO " + userId, AccountStatus.ACTIVE);
    }

    private static UserAccount disabledApplicant(String userId) {
        return new UserAccount(userId, "hash", UserRole.APPLICANT, "Applicant " + userId, AccountStatus.DISABLED);
    }
}
