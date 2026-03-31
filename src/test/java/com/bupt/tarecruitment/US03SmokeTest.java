package com.bupt.tarecruitment;

import com.bupt.tarecruitment.auth.AuthService;
import com.bupt.tarecruitment.auth.AuthValidator;
import com.bupt.tarecruitment.auth.TextFileUserRepository;
import com.bupt.tarecruitment.auth.UserRepository;
import com.bupt.tarecruitment.auth.UserRole;
import com.bupt.tarecruitment.common.storage.DataFile;
import com.bupt.tarecruitment.job.JobIdGenerator;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobPostingService;
import com.bupt.tarecruitment.job.JobRepository;
import com.bupt.tarecruitment.job.JobStatus;
import com.bupt.tarecruitment.job.TextFileJobRepository;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class US03SmokeTest {
    private US03SmokeTest() {
    }

    public static void main(String[] args) {
        try {
            Path tempDataDirectory = Files.createTempDirectory("us03-smoke");
            Files.write(
                tempDataDirectory.resolve(DataFile.USERS.fileName()),
                List.of(DataFile.USERS.initialLines().getFirst()),
                StandardCharsets.UTF_8
            );
            Files.write(
                tempDataDirectory.resolve(DataFile.JOBS.fileName()),
                DataFile.JOBS.initialLines(),
                StandardCharsets.UTF_8
            );

            UserRepository userRepository = new TextFileUserRepository(tempDataDirectory);
            AuthService authService = new AuthService(userRepository, new AuthValidator());
            authService.register("mo101", "pass101", UserRole.MO);
            authService.register("ta101", "pass201", UserRole.APPLICANT);

            JobRepository jobRepository = new TextFileJobRepository(tempDataDirectory);
            JobPostingService jobPostingService = new JobPostingService(
                jobRepository,
                new JobIdGenerator(jobRepository),
                userRepository
            );

            JobPosting publishedJob = jobPostingService.publish(
                "mo101",
                "TA for Algorithms",
                "COMP303",
                "Support weekly algorithm tutorials and marking.",
                List.of("Algorithms", "Communication"),
                3,
                List.of("THU-10:00-12:00")
            );

            JobPosting closedJob = new JobPosting(
                "job900",
                "mo101",
                "Closed TA Role",
                "COMP404",
                "Historical closed vacancy for browse filtering tests.",
                List.of("Testing"),
                2,
                List.of("FRI-09:00-11:00"),
                JobStatus.CLOSED
            );
            jobRepository.save(closedJob);

            List<JobPosting> openJobs = jobRepository.findAll().stream()
                .filter(job -> job.status() == JobStatus.OPEN)
                .toList();

            assertTrue(!openJobs.isEmpty(), "Open job browse list should not be empty.");
            assertTrue(
                openJobs.stream().allMatch(job -> job.status() == JobStatus.OPEN),
                "Browse list should only contain OPEN jobs."
            );
            assertTrue(
                openJobs.stream().anyMatch(job -> job.jobId().equals(publishedJob.jobId())),
                "Published OPEN job should be visible in browse results."
            );
            assertTrue(
                openJobs.stream().noneMatch(job -> job.jobId().equals(closedJob.jobId())),
                "Closed jobs must not appear in browse results."
            );

            JobPosting loadedJob = jobRepository.findByJobId(publishedJob.jobId())
                .orElseThrow(() -> new IllegalStateException("Published job should be retrievable by jobId."));

            assertEquals(publishedJob.jobId(), loadedJob.jobId(), "Unexpected jobId in job detail lookup.");
            assertEquals("TA for Algorithms", loadedJob.title(), "Unexpected title in job detail lookup.");
            assertEquals("COMP303", loadedJob.moduleOrActivity(), "Unexpected module/activity in job detail lookup.");
            assertEquals(3, loadedJob.weeklyHours(), "Unexpected weekly hours in job detail lookup.");
            assertEquals(List.of("Algorithms", "Communication"), loadedJob.requiredSkills(), "Unexpected required skills.");
            assertEquals(List.of("THU-10:00-12:00"), loadedJob.scheduleSlots(), "Unexpected schedule slots.");
            assertTrue(jobRepository.findByJobId("job404").isEmpty(), "Unknown jobId should not resolve to a job.");

            System.out.println("US03 smoke test passed.");
            System.out.println("Temp data directory: " + tempDataDirectory);
        } catch (Exception exception) {
            throw new IllegalStateException("US03 smoke test failed.", exception);
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
