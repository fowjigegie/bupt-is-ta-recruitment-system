package com.bupt.tarecruitment.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bupt.tarecruitment.applicant.ApplicantCv;
import com.bupt.tarecruitment.applicant.ApplicantCvRepository;
import com.bupt.tarecruitment.applicant.ApplicantProfileRepository;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;
import com.bupt.tarecruitment.job.JobStatus;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationIdGenerator applicationIdGenerator;

    @Mock
    private ApplicantProfileRepository profileRepository;

    @Mock
    private ApplicantCvRepository cvRepository;

    private JobApplicationService service;

    private final String applicantUserId = "user001";
    private final String jobId = "job001";
    private final String cvId = "cv001";

    @BeforeEach
    void setUp() {
        service = new JobApplicationService(
            jobRepository,
            applicationRepository,
            applicationIdGenerator,
            profileRepository,
            cvRepository
        );
    }

    @Test
    void shouldApplyToOpenJobSuccessfully() {
        JobPosting job = mock(JobPosting.class);
        ApplicantCv cv = mock(ApplicantCv.class);

        when(profileRepository.findByUserId(applicantUserId)).thenReturn(existingProfile());
        when(jobRepository.findByJobId(jobId)).thenReturn(Optional.of(job));
        when(job.status()).thenReturn(JobStatus.OPEN);
        when(job.scheduleSlots()).thenReturn(List.of());
        when(applicationRepository.findByApplicantUserId(applicantUserId)).thenReturn(List.of());
        when(cvRepository.findByCvId(cvId)).thenReturn(Optional.of(cv));
        when(cv.ownerUserId()).thenReturn(applicantUserId);
        when(applicationIdGenerator.nextApplicationId()).thenReturn("application001");

        JobApplication result = service.applyToJobWithCv(applicantUserId, jobId, cvId);

        assertNotNull(result);
        assertEquals("application001", result.applicationId());
        assertEquals(jobId, result.jobId());
        assertEquals(applicantUserId, result.applicantUserId());
        assertEquals(cvId, result.cvId());
        assertEquals(ApplicationStatus.SUBMITTED, result.status());
        assertEquals("", result.reviewerNote());
        assertNotNull(result.submittedAt());

        ArgumentCaptor<JobApplication> captor = ArgumentCaptor.forClass(JobApplication.class);
        verify(applicationRepository).save(captor.capture());

        JobApplication saved = captor.getValue();
        assertEquals("application001", saved.applicationId());
        assertEquals(jobId, saved.jobId());
        assertEquals(applicantUserId, saved.applicantUserId());
        assertEquals(cvId, saved.cvId());
        assertEquals(ApplicationStatus.SUBMITTED, saved.status());
    }

    @Test
    void shouldThrowWhenApplicantUserIdIsBlank() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.applyToJobWithCv(" ", jobId, cvId)
        );

        assertEquals("applicantUserId must not be blank.", ex.getMessage());
    }

    @Test
    void shouldThrowWhenJobIdIsBlank() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.applyToJobWithCv(applicantUserId, " ", cvId)
        );

        assertEquals("jobId must not be blank.", ex.getMessage());
    }

    @Test
    void shouldThrowWhenCvIdIsBlank() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.applyToJobWithCv(applicantUserId, jobId, " ")
        );

        assertEquals("cvId must not be blank.", ex.getMessage());
    }

    @Test
    void shouldThrowWhenApplicantProfileDoesNotExist() {
        when(profileRepository.findByUserId(applicantUserId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.applyToJobWithCv(applicantUserId, jobId, cvId)
        );

        assertEquals(
            "No applicant profile exists for userId: " + applicantUserId,
            ex.getMessage()
        );
    }

    @Test
    void shouldThrowWhenJobDoesNotExist() {
        when(profileRepository.findByUserId(applicantUserId)).thenReturn(existingProfile());
        when(jobRepository.findByJobId(jobId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.applyToJobWithCv(applicantUserId, jobId, cvId)
        );

        assertEquals("No job exists for jobId: " + jobId, ex.getMessage());
    }

    @Test
    void shouldThrowWhenJobIsNotOpen() {
        JobPosting job = mock(JobPosting.class);

        when(profileRepository.findByUserId(applicantUserId)).thenReturn(existingProfile());
        when(jobRepository.findByJobId(jobId)).thenReturn(Optional.of(job));
        when(job.status()).thenReturn(JobStatus.CLOSED);

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.applyToJobWithCv(applicantUserId, jobId, cvId)
        );

        assertEquals("Only OPEN jobs can be applied to.", ex.getMessage());
    }

    @Test
    void shouldBlockDuplicateApplicationWhenExistingApplicationIsNotWithdrawn() {
        JobPosting job = mock(JobPosting.class);

        JobApplication existing = new JobApplication(
            "application999",
            jobId,
            applicantUserId,
            "cv999",
            ApplicationStatus.SUBMITTED,
            LocalDateTime.now().minusDays(1),
            ""
        );

        when(profileRepository.findByUserId(applicantUserId)).thenReturn(existingProfile());
        when(jobRepository.findByJobId(jobId)).thenReturn(Optional.of(job));
        when(job.status()).thenReturn(JobStatus.OPEN);
        when(applicationRepository.findByApplicantUserId(applicantUserId)).thenReturn(List.of(existing));

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.applyToJobWithCv(applicantUserId, jobId, cvId)
        );

        assertEquals("Duplicate application is not allowed for this job.", ex.getMessage());
    }

    @Test
    void shouldAllowReapplyWhenPreviousApplicationWasWithdrawn() {
        JobPosting job = mock(JobPosting.class);
        ApplicantCv cv = mock(ApplicantCv.class);

        JobApplication withdrawnApplication = new JobApplication(
            "application000",
            jobId,
            applicantUserId,
            "oldCv",
            ApplicationStatus.WITHDRAWN,
            LocalDateTime.now().minusDays(2),
            ""
        );

        when(profileRepository.findByUserId(applicantUserId)).thenReturn(existingProfile());
        when(jobRepository.findByJobId(jobId)).thenReturn(Optional.of(job));
        when(job.status()).thenReturn(JobStatus.OPEN);
        when(job.scheduleSlots()).thenReturn(List.of());
        when(applicationRepository.findByApplicantUserId(applicantUserId))
            .thenReturn(List.of(withdrawnApplication));
        when(cvRepository.findByCvId(cvId)).thenReturn(Optional.of(cv));
        when(cv.ownerUserId()).thenReturn(applicantUserId);
        when(applicationIdGenerator.nextApplicationId()).thenReturn("application002");

        JobApplication result = service.applyToJobWithCv(applicantUserId, jobId, cvId);

        assertEquals("application002", result.applicationId());
        verify(applicationRepository).save(any(JobApplication.class));
    }

    @Test
    void shouldThrowWhenCvDoesNotExist() {
        JobPosting job = mock(JobPosting.class);

        when(profileRepository.findByUserId(applicantUserId)).thenReturn(existingProfile());
        when(jobRepository.findByJobId(jobId)).thenReturn(Optional.of(job));
        when(job.status()).thenReturn(JobStatus.OPEN);
        when(applicationRepository.findByApplicantUserId(applicantUserId)).thenReturn(List.of());
        when(cvRepository.findByCvId(cvId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.applyToJobWithCv(applicantUserId, jobId, cvId)
        );

        assertEquals("No CV exists for cvId: " + cvId, ex.getMessage());
    }

    @Test
    void shouldThrowWhenCvDoesNotBelongToApplicant() {
        JobPosting job = mock(JobPosting.class);
        ApplicantCv cv = mock(ApplicantCv.class);

        when(profileRepository.findByUserId(applicantUserId)).thenReturn(existingProfile());
        when(jobRepository.findByJobId(jobId)).thenReturn(Optional.of(job));
        when(job.status()).thenReturn(JobStatus.OPEN);
        when(applicationRepository.findByApplicantUserId(applicantUserId)).thenReturn(List.of());
        when(cvRepository.findByCvId(cvId)).thenReturn(Optional.of(cv));
        when(cv.ownerUserId()).thenReturn("another-user");

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.applyToJobWithCv(applicantUserId, jobId, cvId)
        );

        assertEquals(
            "The selected CV does not belong to applicantUserId: " + applicantUserId,
            ex.getMessage()
        );
    }

    @Test
    void shouldWithdrawApplicationSuccessfully() {
        JobApplication existing = new JobApplication(
            "application001",
            jobId,
            applicantUserId,
            cvId,
            ApplicationStatus.SUBMITTED,
            LocalDateTime.of(2026, 3, 31, 10, 0),
            ""
        );

        when(applicationRepository.findByApplicationId("application001"))
            .thenReturn(Optional.of(existing));

        JobApplication result = service.withdrawApplication(applicantUserId, "application001");

        assertEquals("application001", result.applicationId());
        assertEquals(ApplicationStatus.WITHDRAWN, result.status());
        assertEquals(existing.submittedAt(), result.submittedAt());

        ArgumentCaptor<JobApplication> captor = ArgumentCaptor.forClass(JobApplication.class);
        verify(applicationRepository).save(captor.capture());

        JobApplication saved = captor.getValue();
        assertEquals(ApplicationStatus.WITHDRAWN, saved.status());
        assertEquals(applicantUserId, saved.applicantUserId());
    }

    @Test
    void shouldThrowWhenApplicationNotFoundDuringWithdraw() {
        when(applicationRepository.findByApplicationId("application001"))
            .thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.withdrawApplication(applicantUserId, "application001")
        );

        assertEquals("Application not found.", ex.getMessage());
    }

    @Test
    void shouldThrowWhenWithdrawingOthersApplication() {
        JobApplication existing = new JobApplication(
            "application001",
            jobId,
            "other-user",
            cvId,
            ApplicationStatus.SUBMITTED,
            LocalDateTime.now(),
            ""
        );

        when(applicationRepository.findByApplicationId("application001"))
            .thenReturn(Optional.of(existing));

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.withdrawApplication(applicantUserId, "application001")
        );

        assertEquals("You can only withdraw your own application.", ex.getMessage());
    }

    @Test
    void shouldThrowWhenApplicationAlreadyWithdrawn() {
        JobApplication existing = new JobApplication(
            "application001",
            jobId,
            applicantUserId,
            cvId,
            ApplicationStatus.WITHDRAWN,
            LocalDateTime.now(),
            ""
        );

        when(applicationRepository.findByApplicationId("application001"))
            .thenReturn(Optional.of(existing));

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.withdrawApplication(applicantUserId, "application001")
        );

        assertEquals("This application has already been withdrawn.", ex.getMessage());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Optional existingProfile() {
        return (Optional) Optional.of(new Object());
    }
}
