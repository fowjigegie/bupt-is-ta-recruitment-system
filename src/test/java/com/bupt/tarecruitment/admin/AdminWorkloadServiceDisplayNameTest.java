package com.bupt.tarecruitment.admin;

import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.applicant.ApplicantProfileRepository;
import com.bupt.tarecruitment.application.ApplicationRepository;
import com.bupt.tarecruitment.application.ApplicationStatus;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.auth.AccountStatus;
import com.bupt.tarecruitment.auth.UserAccount;
import com.bupt.tarecruitment.auth.UserRepository;
import com.bupt.tarecruitment.auth.UserRole;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;
import com.bupt.tarecruitment.job.JobStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminWorkloadServiceDisplayNameTest {
    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicantProfileRepository profileRepository;

    @Test
    void shouldPreferProfileFullNameForApplicantDisplayName() {
        when(applicationRepository.findAll()).thenReturn(List.of(acceptedApplication("application001", "job001", "ta001")));
        when(jobRepository.findByJobId("job001")).thenReturn(Optional.of(job("job001")));
        when(profileRepository.findByUserId("ta001")).thenReturn(Optional.of(profile("ta001", "Profile Real Name")));

        AdminWorkloadService service = new AdminWorkloadService(
            applicationRepository,
            jobRepository,
            userRepository,
            profileRepository
        );

        WorkloadSummary summary = service.listAcceptedTaWorkloads(10).getFirst();

        assertEquals("Profile Real Name", summary.applicantDisplayName());
        verify(userRepository, never()).findByUserId("ta001");
    }

    @Test
    void shouldFallbackToUserDisplayNameWhenProfileIsMissing() {
        when(applicationRepository.findAll()).thenReturn(List.of(acceptedApplication("application001", "job001", "ta001")));
        when(jobRepository.findByJobId("job001")).thenReturn(Optional.of(job("job001")));
        when(profileRepository.findByUserId("ta001")).thenReturn(Optional.empty());
        when(userRepository.findByUserId("ta001")).thenReturn(Optional.of(
            new UserAccount("ta001", "hash", UserRole.APPLICANT, "User Display Name", AccountStatus.ACTIVE)
        ));

        AdminWorkloadService service = new AdminWorkloadService(
            applicationRepository,
            jobRepository,
            userRepository,
            profileRepository
        );

        WorkloadSummary summary = service.listAcceptedTaWorkloads(10).getFirst();

        assertEquals("User Display Name", summary.applicantDisplayName());
    }

    private static JobApplication acceptedApplication(String applicationId, String jobId, String applicantUserId) {
        return new JobApplication(
            applicationId,
            jobId,
            applicantUserId,
            "cv001",
            ApplicationStatus.ACCEPTED,
            LocalDateTime.parse("2026-04-02T10:00:00"),
            ""
        );
    }

    private static JobPosting job(String jobId) {
        return new JobPosting(
            jobId,
            "mo001",
            "Teaching Assistant",
            "Software Engineering",
            "Support seminars.",
            List.of("Communication"),
            2,
            List.of("MON-09:00-11:00"),
            JobStatus.OPEN
        );
    }

    private static ApplicantProfile profile(String userId, String fullName) {
        return new ApplicantProfile(
            "profile001",
            userId,
            "231229001",
            fullName,
            "Software Engineering",
            2,
            "Not Graduated",
            List.of("Java"),
            List.of("MON-09:00-11:00"),
            List.of("Teaching Assistant")
        );
    }
}
