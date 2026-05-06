package com.bupt.tarecruitment;

import com.bupt.tarecruitment.job.JobBrowseFilter;
import com.bupt.tarecruitment.job.JobActivityType;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobStatus;

import java.util.List;
import java.util.Map;

/**
 * 覆盖 US07 场景的冒烟测试。
 */
public final class US07SmokeTest {
    private US07SmokeTest() {
    }

    public static void main(String[] args) {
        List<JobPosting> jobs = List.of(
            job("job001", "mo001", "TA for Software Engineering", "EBU6304", "Java lab support", List.of("Java", "Teamwork"), List.of("MON-10:00-12:00"), JobStatus.OPEN),
            job("job007", "mo003", "TA for Probability and Statistics", "STAT210", JobActivityType.TUTORIAL, "Statistics support sessions", List.of("Statistics", "Python", "Communication"), List.of("THU-10:00-12:00"), JobStatus.OPEN),
            job("job012", "mo006", "TA for Mobile App Development", "COMP330", JobActivityType.PROJECT_DEVELOPMENT, "Android practices", List.of("Java", "Android", "UI Design"), List.of("FRI-14:00-16:00"), JobStatus.OPEN),
            job("job099", "mo099", "Closed TA Role", "COMP999", "Should never appear", List.of("Testing"), List.of("MON-08:00-09:00"), JobStatus.CLOSED)
        );

        Map<String, String> organiserNames = Map.of(
            "mo001", "Alice Module Organiser",
            "mo003", "Naomi Williams",
            "mo006", "Rui He"
        );

        List<JobPosting> allOpenNewest = JobBrowseFilter.filterAndSortOpenJobs(
            jobs,
            "",
            "All skills",
            "All organisers",
            "All modules",
            "All activity types",
            "Any time",
            true,
            organiserId -> organiserNames.getOrDefault(organiserId, "")
        );
        assertEquals(3, allOpenNewest.size(), "Only OPEN jobs should appear in browse results.");
        assertEquals("job012", allOpenNewest.getFirst().jobId(), "Newest job ID should appear first.");

        List<JobPosting> keywordFiltered = JobBrowseFilter.filterAndSortOpenJobs(
            jobs, "statistics", "All skills", "All organisers", "All modules", "All activity types", "Any time", true,
            organiserId -> organiserNames.getOrDefault(organiserId, "")
        );
        assertEquals(List.of("job007"), extractIds(keywordFiltered), "Keyword filter should match title/module/description text.");

        List<JobPosting> skillFiltered = JobBrowseFilter.filterAndSortOpenJobs(
            jobs, "", "Java", "All organisers", "All modules", "All activity types", "Any time", true,
            organiserId -> organiserNames.getOrDefault(organiserId, "")
        );
        assertEquals(List.of("job012", "job001"), extractIds(skillFiltered), "Skill filter should keep only matching jobs.");

        List<JobPosting> organiserFiltered = JobBrowseFilter.filterAndSortOpenJobs(
            jobs, "", "All skills", "mo003", "All modules", "All activity types", "Any time", true,
            organiserId -> organiserNames.getOrDefault(organiserId, "")
        );
        assertEquals(List.of("job007"), extractIds(organiserFiltered), "Organiser filter should keep only the selected organiser's jobs.");

        List<JobPosting> combinedFiltered = JobBrowseFilter.filterAndSortOpenJobs(
            jobs, "android", "Java", "mo006", "All modules", "All activity types", "Any time", true,
            organiserId -> organiserNames.getOrDefault(organiserId, "")
        );
        assertEquals(List.of("job012"), extractIds(combinedFiltered), "Combined filters should narrow results correctly.");

        List<JobPosting> oldestFirst = JobBrowseFilter.filterAndSortOpenJobs(
            jobs, "", "All skills", "All organisers", "All modules", "All activity types", "Any time", false,
            organiserId -> organiserNames.getOrDefault(organiserId, "")
        );
        assertEquals("job001", oldestFirst.getFirst().jobId(), "Oldest job ID should appear first when sorting oldest-first.");

        List<JobPosting> moduleFiltered = JobBrowseFilter.filterAndSortOpenJobs(
            jobs, "", "All skills", "All organisers", "STAT210", "All activity types", "Any time", true,
            organiserId -> organiserNames.getOrDefault(organiserId, "")
        );
        assertEquals(List.of("job007"), extractIds(moduleFiltered), "Module filter should keep only the selected module.");

        List<JobPosting> activityFiltered = JobBrowseFilter.filterAndSortOpenJobs(
            jobs, "", "All skills", "All organisers", "All modules", "Tutorial", "Any time", true,
            organiserId -> organiserNames.getOrDefault(organiserId, "")
        );
        assertEquals(List.of("job007"), extractIds(activityFiltered), "Activity filter should match tutorial-style jobs.");

        List<JobPosting> timeFiltered = JobBrowseFilter.filterAndSortOpenJobs(
            jobs, "", "All skills", "All organisers", "All modules", "All activity types", "Afternoon", true,
            organiserId -> organiserNames.getOrDefault(organiserId, "")
        );
        assertEquals(List.of("job012"), extractIds(timeFiltered), "Time-slot filter should match jobs by start period.");

        List<JobPosting> organiserNameFiltered = JobBrowseFilter.filterAndSortOpenJobs(
            jobs, "naomi", "All skills", "All organisers", "All modules", "All activity types", "Any time", true,
            organiserId -> organiserNames.getOrDefault(organiserId, "")
        );
        assertEquals(List.of("job007"), extractIds(organiserNameFiltered), "Keyword search should also match MO display names.");

        System.out.println("US07 smoke test passed.");
    }

    private static JobPosting job(
        String jobId,
        String organiserId,
        String title,
        String moduleOrActivity,
        String activityType,
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
            activityType,
            description,
            requiredSkills,
            3,
            scheduleSlots,
            status
        );
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
        return job(
            jobId,
            organiserId,
            title,
            moduleOrActivity,
            JobActivityType.infer(title, description),
            description,
            requiredSkills,
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
