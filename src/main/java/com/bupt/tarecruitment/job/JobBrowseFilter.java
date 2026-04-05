package com.bupt.tarecruitment.job;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class JobBrowseFilter {
    private JobBrowseFilter() {
    }

    public static List<JobPosting> filterAndSortOpenJobs(
        List<JobPosting> jobs,
        String keyword,
        String selectedSkill,
        String selectedOrganiser,
        boolean newestFirst
    ) {
        Comparator<JobPosting> comparator = Comparator.comparingInt(JobBrowseFilter::extractJobSequenceNumber)
            .thenComparing(JobPosting::jobId);
        if (newestFirst) {
            comparator = comparator.reversed();
        }

        return jobs.stream()
            .filter(job -> job.status() == JobStatus.OPEN)
            .filter(job -> matchesKeyword(job, keyword))
            .filter(job -> matchesSkill(job, selectedSkill))
            .filter(job -> matchesOrganiser(job, selectedOrganiser))
            .sorted(comparator)
            .toList();
    }

    public static List<String> collectUniqueSkills(List<JobPosting> jobs) {
        return jobs.stream()
            .flatMap(job -> job.requiredSkills().stream())
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    public static List<String> collectUniqueOrganisers(List<JobPosting> jobs) {
        return jobs.stream()
            .map(JobPosting::organiserId)
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .distinct()
            .sorted()
            .toList();
    }

    private static boolean matchesKeyword(JobPosting job, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }

        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        return contains(job.jobId(), normalizedKeyword)
            || contains(job.title(), normalizedKeyword)
            || contains(job.moduleOrActivity(), normalizedKeyword)
            || contains(job.description(), normalizedKeyword)
            || contains(job.organiserId(), normalizedKeyword)
            || job.requiredSkills().stream().anyMatch(skill -> contains(skill, normalizedKeyword))
            || job.scheduleSlots().stream().anyMatch(slot -> contains(slot, normalizedKeyword));
    }

    private static boolean matchesSkill(JobPosting job, String selectedSkill) {
        if (selectedSkill == null || selectedSkill.equals("All skills")) {
            return true;
        }
        return job.requiredSkills().stream()
            .anyMatch(skill -> skill.equalsIgnoreCase(selectedSkill));
    }

    private static boolean matchesOrganiser(JobPosting job, String selectedOrganiser) {
        if (selectedOrganiser == null || selectedOrganiser.equals("All organisers")) {
            return true;
        }
        return job.organiserId().equals(selectedOrganiser);
    }

    private static boolean contains(String rawValue, String keyword) {
        return rawValue != null && rawValue.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private static int extractJobSequenceNumber(JobPosting job) {
        String jobId = job.jobId();
        String numeric = jobId.replaceAll("\\D+", "");
        if (numeric.isEmpty()) {
            return Integer.MIN_VALUE;
        }
        try {
            return Integer.parseInt(numeric);
        } catch (NumberFormatException ignored) {
            return Integer.MIN_VALUE;
        }
    }
}
