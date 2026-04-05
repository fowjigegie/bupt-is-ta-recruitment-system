package com.bupt.tarecruitment;

import com.bupt.tarecruitment.job.JobBrowseFilter;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobStatus;

import java.util.List;

public final class US07SmokeTest {
    private US07SmokeTest() {
    }

    public static void main(String[] args) {
        List<JobPosting> jobs = List.of(
            job("job001", "mo001", "TA for Software Engineering", "EBU6304", "Java lab support", List.of("Java", "Teamwork"), List.of("MON-10:00-12:00"), JobStatus.OPEN),
            job("job007", "mo003", "TA for Probability and Statistics", "STAT210", "Statistics tutorials", List.of("Statistics", "Python", "Communication"), List.of("THU-10:00-12:00"), JobStatus.OPEN),
            job("job012", "mo006", "TA for Mobile App Development", "COMP330", "Android studio practices", List.of("Java", "Android", "UI Design"), List.of("FRI-14:00-16:00"), JobStatus.OPEN),
            job("job099", "mo099", "Closed TA Role", "COMP999", "Should never appear", List.of("Testing"), List.of("MON-08:00-09:00"), JobStatus.CLOSED)
        );

        List<JobPosting> allOpenNewest = JobBrowseFilter.filterAndSortOpenJobs(jobs, "", "All skills", "All organisers", true);
        assertEquals(3, allOpenNewest.size(), "Only OPEN jobs should appear in browse results.");
        assertEquals("job012", allOpenNewest.getFirst().jobId(), "Newest job ID should appear first.");

        List<JobPosting> keywordFiltered = JobBrowseFilter.filterAndSortOpenJobs(jobs, "statistics", "All skills", "All organisers", true);
        assertEquals(List.of("job007"), extractIds(keywordFiltered), "Keyword filter should match title/module/description text.");

        List<JobPosting> skillFiltered = JobBrowseFilter.filterAndSortOpenJobs(jobs, "", "Java", "All organisers", true);
        assertEquals(List.of("job012", "job001"), extractIds(skillFiltered), "Skill filter should keep only matching jobs.");

        List<JobPosting> organiserFiltered = JobBrowseFilter.filterAndSortOpenJobs(jobs, "", "All skills", "mo003", true);
        assertEquals(List.of("job007"), extractIds(organiserFiltered), "Organiser filter should keep only the selected organiser's jobs.");

        List<JobPosting> combinedFiltered = JobBrowseFilter.filterAndSortOpenJobs(jobs, "android", "Java", "mo006", true);
        assertEquals(List.of("job012"), extractIds(combinedFiltered), "Combined filters should narrow results correctly.");

        List<JobPosting> oldestFirst = JobBrowseFilter.filterAndSortOpenJobs(jobs, "", "All skills", "All organisers", false);
        assertEquals("job001", oldestFirst.getFirst().jobId(), "Oldest job ID should appear first when sorting oldest-first.");

        System.out.println("US07 smoke test passed.");
    }

    private static JobPosting job(
        String jobId,
        String organiserId,
        String title,
        String moduleOrActivity,
        String description,
        List<String> requiredSkills,
        List<String> scheduleSlots,
        JobStatus status
    ) {
        return new JobPosting(
            jobId,
            organiserId,
            title,
            moduleOrActivity,
            description,
            requiredSkills,
            3,
            scheduleSlots,
            status
        );
    }

    private static List<String> extractIds(List<JobPosting> jobs) {
        return jobs.stream().map(JobPosting::jobId).toList();
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException(message + " Expected: " + expected + ", actual: " + actual);
        }
    }
}
