package com.bupt.tarecruitment;

import com.bupt.tarecruitment.application.ApplicationRepository;
import com.bupt.tarecruitment.application.ApplicationStatus;
import com.bupt.tarecruitment.application.ApplicationStatusPresenter;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.application.TextFileApplicationRepository;
import com.bupt.tarecruitment.common.storage.DataFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 覆盖 US06 场景的冒烟测试。
 */
public final class US06SmokeTest {
    private US06SmokeTest() {
    }

    public static void main(String[] args) {
        try {
            Path tempDataDirectory = Files.createTempDirectory("us06-smoke");
            bootstrapApplicationsFile(tempDataDirectory);

            ApplicationRepository applicationRepository = new TextFileApplicationRepository(tempDataDirectory);
            applicationRepository.save(buildApplication("application101", "job001", "ta101", "cv101", ApplicationStatus.SUBMITTED, "2026-04-05T09:00:00"));
            applicationRepository.save(buildApplication("application102", "job002", "ta101", "cv102", ApplicationStatus.SHORTLISTED, "2026-04-05T10:00:00"));
            applicationRepository.save(buildApplication("application103", "job003", "ta101", "cv103", ApplicationStatus.ACCEPTED, "2026-04-05T11:00:00"));
            applicationRepository.save(buildApplication("application104", "job004", "ta101", "cv104", ApplicationStatus.REJECTED, "2026-04-05T12:00:00"));
            applicationRepository.save(buildApplication("application105", "job005", "ta101", "cv105", ApplicationStatus.WITHDRAWN, "2026-04-05T13:00:00"));
            applicationRepository.save(buildApplication("application201", "job101", "ta201", "cv201", ApplicationStatus.SUBMITTED, "2026-04-05T14:00:00"));

            List<JobApplication> ta101Applications = applicationRepository.findByApplicantUserId("ta101").stream()
                .sorted(Comparator.comparing(JobApplication::submittedAt).reversed())
                .toList();

            assertEquals(5, ta101Applications.size(), "Applicant should see all of their applications.");
            assertEquals("application105", ta101Applications.getFirst().applicationId(), "Newest application should appear first.");
            assertEquals(
                List.of("Withdrawn", "Rejected", "Accepted", "Shortlisted", "Submitted"),
                ta101Applications.stream().map(application -> ApplicationStatusPresenter.toDisplayText(application.status())).toList(),
                "Statuses should map to readable labels in the same order the status page expects."
            );

            assertEquals("Submitted", ApplicationStatusPresenter.toDisplayText(ApplicationStatus.SUBMITTED), "Submitted status label mismatch.");
            assertEquals("Shortlisted", ApplicationStatusPresenter.toDisplayText(ApplicationStatus.SHORTLISTED), "Shortlisted status label mismatch.");
            assertEquals("Accepted", ApplicationStatusPresenter.toDisplayText(ApplicationStatus.ACCEPTED), "Accepted status label mismatch.");
            assertEquals("Rejected", ApplicationStatusPresenter.toDisplayText(ApplicationStatus.REJECTED), "Rejected status label mismatch.");
            assertEquals("Withdrawn", ApplicationStatusPresenter.toDisplayText(ApplicationStatus.WITHDRAWN), "Withdrawn status label mismatch.");

            System.out.println("US06 smoke test passed.");
        } catch (Exception exception) {
            throw new IllegalStateException("US06 smoke test failed.", exception);
        }
    }

    private static void bootstrapApplicationsFile(Path tempDataDirectory) throws Exception {
        Files.write(
            tempDataDirectory.resolve(DataFile.APPLICATIONS.fileName()),
            List.of(DataFile.APPLICATIONS.initialLines().getFirst()),
            StandardCharsets.UTF_8
        );
    }

    private static JobApplication buildApplication(
        String applicationId,
        String jobId,
        String applicantUserId,
        String cvId,
        ApplicationStatus status,
        String submittedAt
    ) {
        return new JobApplication(
            applicationId,
            jobId,
            applicantUserId,
            cvId,
            status,
            LocalDateTime.parse(submittedAt),
            ""
        );
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException(message + " Expected: " + expected + ", actual: " + actual);
        }
    }
}
