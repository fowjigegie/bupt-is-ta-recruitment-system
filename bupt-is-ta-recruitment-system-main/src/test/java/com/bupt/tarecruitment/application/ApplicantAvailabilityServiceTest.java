package com.bupt.tarecruitment.application;

import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.applicant.ApplicantProfileRepository;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;
import com.bupt.tarecruitment.job.JobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)

/**
 * 验证申请人可用时间覆盖检查服务的基础行为。
 */
class ApplicantAvailabilityServiceTest {

    @Mock
    private ApplicantProfileRepository profileRepository;

    @Mock
    private JobRepository jobRepository;

    private ApplicantAvailabilityService service;

    @BeforeEach
    void setUp() {
        service = new ApplicantAvailabilityService(profileRepository, jobRepository);
    }

    @Test
    void shouldReportFullCoverageWhenAvailabilityContainsAllJobSlots() {
        ApplicantProfile profile = applicantProfile(List.of("MON-09:00-12:00", "WED-14:00-16:00"));
        JobPosting job = jobPosting("job801", List.of("MON-10:00-11:00", "WED-14:00-16:00"));

        AvailabilityCheckResult result = service.analyze(profile, job);

        assertTrue(result.fitsAvailability());
        assertEquals(List.of("MON-10:00-11:00", "WED-14:00-16:00"), result.coveredJobSlots());
        assertEquals(List.of(), result.uncoveredJobSlots());
    }

    @Test
    void shouldReportUncoveredSlotsWhenAvailabilityDoesNotContainJobSchedule() {
        ApplicantProfile profile = applicantProfile(List.of("MON-09:00-11:00"));
        JobPosting job = jobPosting("job802", List.of("MON-10:00-12:00", "TUE-14:00-16:00"));

        AvailabilityCheckResult result = service.analyze(profile, job);

        assertFalse(result.fitsAvailability());
        assertEquals(List.of(), result.coveredJobSlots());
        assertEquals(List.of("MON-10:00-12:00", "TUE-14:00-16:00"), result.uncoveredJobSlots());
    }

    @Test
    void shouldReturnEmptyWhenApplicantHasNoProfile() {
        when(profileRepository.findByUserId("ta803")).thenReturn(Optional.empty());

        Optional<AvailabilityCheckResult> result = service.availabilityForApplicantAndJob("ta803", "job803");

        assertTrue(result.isEmpty());
    }

    private static ApplicantProfile applicantProfile(List<String> availabilitySlots) {
        return new ApplicantProfile(
            "profile-availability",
            "ta-availability",
            "231228888",
            "Availability Applicant",
            "Software Engineering",
            3,
            "Not Graduated",
            List.of("Java"),
            availabilitySlots,
            List.of("Teaching Assistant")
        );
    }

    private static JobPosting jobPosting(String jobId, List<String> scheduleSlots) {
        return new JobPosting(
            jobId,
            "mo-availability",
            "TA role " + jobId,
            "Availability Module",
            "Support teaching activities.",
            List.of("Communication"),
            2,
            scheduleSlots,
            JobStatus.OPEN
        );
    }
}
