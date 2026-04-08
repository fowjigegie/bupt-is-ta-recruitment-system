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
class ApplicantProfileServiceCreateUnitTest {

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

    @Test
    void shouldCreateProfileWhenApplicantIsActiveAndProfileIsUnique() {
        ApplicantProfile profile = validProfile("profile901", "ta901", "231229901");

        when(userRepository.findByUserId("ta901")).thenReturn(Optional.of(activeApplicant("ta901")));
        when(profileRepository.findByUserId("ta901")).thenReturn(Optional.empty());
        when(profileRepository.findByStudentId("231229901")).thenReturn(Optional.empty());

        ApplicantProfile created = service.createProfile(profile);

        assertSame(profile, created);
        verify(profileRepository).save(profile);
    }

    @Test
    void shouldRejectDuplicateProfileForSameUser() {
        ApplicantProfile profile = validProfile("profile901", "ta901", "231229901");

        when(userRepository.findByUserId("ta901")).thenReturn(Optional.of(activeApplicant("ta901")));
        when(profileRepository.findByUserId("ta901")).thenReturn(Optional.of(profile));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.createProfile(profile)
        );

        assertEquals("A profile already exists for userId: ta901", exception.getMessage());
        verify(profileRepository, never()).save(profile);
        verify(profileRepository, never()).findByStudentId("231229901");
    }

    @Test
    void shouldRejectStudentIdAlreadyUsedByAnotherApplicant() {
        ApplicantProfile newProfile = validProfile("profile902", "ta902", "231229901");
        ApplicantProfile existingProfile = validProfile("profile901", "ta901", "231229901");

        when(userRepository.findByUserId("ta902")).thenReturn(Optional.of(activeApplicant("ta902")));
        when(profileRepository.findByUserId("ta902")).thenReturn(Optional.empty());
        when(profileRepository.findByStudentId("231229901")).thenReturn(Optional.of(existingProfile));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.createProfile(newProfile)
        );

        assertEquals(
            "studentId is already used by another applicant: 231229901",
            exception.getMessage()
        );
        verify(profileRepository, never()).save(newProfile);
    }

    @Test
    void shouldRejectCreateWhenUserIsNotRegistered() {
        ApplicantProfile profile = validProfile("profile901", "ghost901", "231229901");

        when(userRepository.findByUserId("ghost901")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.createProfile(profile)
        );

        assertEquals("No registered user exists for userId: ghost901", exception.getMessage());
        verify(profileRepository, never()).save(profile);
    }

    @Test
    void shouldRejectCreateWhenUserRoleIsNotApplicant() {
        ApplicantProfile profile = validProfile("profile901", "mo901", "231229901");

        when(userRepository.findByUserId("mo901")).thenReturn(Optional.of(activeMo("mo901")));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.createProfile(profile)
        );

        assertEquals("userId mo901 is not an ACTIVE APPLICANT account.", exception.getMessage());
        verify(profileRepository, never()).save(profile);
    }

    @Test
    void shouldRejectCreateWhenApplicantAccountIsDisabled() {
        ApplicantProfile profile = validProfile("profile901", "ta901", "231229901");

        when(userRepository.findByUserId("ta901")).thenReturn(Optional.of(disabledApplicant("ta901")));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.createProfile(profile)
        );

        assertEquals("User account is not active for userId: ta901", exception.getMessage());
        verify(profileRepository, never()).save(profile);
    }

    private static ApplicantProfile validProfile(String profileId, String userId, String studentId) {
        return new ApplicantProfile(
            profileId,
            userId,
            studentId,
            "Unit Test Applicant",
            "Software Engineering",
            2,
            "Not Graduated",
            List.of("Java", "Communication"),
            List.of("MON-09:00-11:00", "WED-14:00-16:00"),
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
