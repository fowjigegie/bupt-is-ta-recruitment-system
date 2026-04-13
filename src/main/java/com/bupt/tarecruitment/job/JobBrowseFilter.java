package com.bupt.tarecruitment.job;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * 为岗位浏览页提供筛选和聚合辅助逻辑。
 */
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
        return filterAndSortOpenJobs(
            jobs,
            keyword,
            selectedSkill,
            selectedOrganiser,
            "All modules",
            "All activity types",
            "Any time",
            newestFirst,
            organiserId -> ""
        );
    }

    public static List<JobPosting> filterAndSortOpenJobs(
        List<JobPosting> jobs,
        String keyword,
        String selectedSkill,
        String selectedOrganiser,
        String selectedModule,
        String selectedActivityType,
        String selectedTimeSlot,
        boolean newestFirst,
        Function<String, String> organiserDisplayNameResolver
    ) {
        Comparator<JobPosting> comparator = Comparator.comparingInt(JobBrowseFilter::extractJobSequenceNumber)
            .thenComparing(JobPosting::jobId);
        if (newestFirst) {
            comparator = comparator.reversed();
        }

        return jobs.stream()
            .filter(job -> job.status() == JobStatus.OPEN)
            .filter(job -> matchesKeyword(job, keyword, organiserDisplayNameResolver))
            .filter(job -> matchesSkill(job, selectedSkill))
            .filter(job -> matchesOrganiser(job, selectedOrganiser))
            .filter(job -> matchesModule(job, selectedModule))
            .filter(job -> matchesActivityType(job, selectedActivityType))
            .filter(job -> matchesTimeSlot(job, selectedTimeSlot))
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

    public static List<String> collectUniqueModules(List<JobPosting> jobs) {
        return jobs.stream()
            .map(JobPosting::moduleOrActivity)
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

    private static boolean matchesKeyword(JobPosting job, String keyword, Function<String, String> organiserDisplayNameResolver) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }

        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        String organiserDisplayName = organiserDisplayNameResolver == null
            ? ""
            : organiserDisplayNameResolver.apply(job.organiserId());
        return contains(job.jobId(), normalizedKeyword)
            || contains(job.title(), normalizedKeyword)
            || contains(job.moduleOrActivity(), normalizedKeyword)
            || contains(job.description(), normalizedKeyword)
            || contains(job.organiserId(), normalizedKeyword)
            || contains(organiserDisplayName, normalizedKeyword)
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

    private static boolean matchesModule(JobPosting job, String selectedModule) {
        if (selectedModule == null || selectedModule.equals("All modules")) {
            return true;
        }
        return job.moduleOrActivity().equalsIgnoreCase(selectedModule);
    }

    private static boolean matchesActivityType(JobPosting job, String selectedActivityType) {
        if (selectedActivityType == null || selectedActivityType.equals("All activity types")) {
            return true;
        }

        String searchBlob = (job.title() + " " + job.description()).toLowerCase(Locale.ROOT);
        return switch (selectedActivityType) {
            case "Lab session" -> searchBlob.contains("lab");
            case "Tutorial" -> searchBlob.contains("tutorial");
            case "Assignment / marking" ->
                searchBlob.contains("assignment") || searchBlob.contains("marking") || searchBlob.contains("grading");
            case "Project / development" ->
                searchBlob.contains("project") || searchBlob.contains("development") || searchBlob.contains("studio");
            default -> true;
        };
    }

    private static boolean matchesTimeSlot(JobPosting job, String selectedTimeSlot) {
        if (selectedTimeSlot == null || selectedTimeSlot.equals("Any time")) {
            return true;
        }

        if (List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").contains(selectedTimeSlot)) {
            String prefix = selectedTimeSlot + "-";
            return job.scheduleSlots().stream().anyMatch(slot -> contains(slot, prefix.toLowerCase(Locale.ROOT)));
        }
        if (selectedTimeSlot.equals("Morning")) {
            return job.scheduleSlots().stream().anyMatch(JobBrowseFilter::slotStartsInMorning);
        }
        if (selectedTimeSlot.equals("Afternoon")) {
            return job.scheduleSlots().stream().anyMatch(JobBrowseFilter::slotStartsInAfternoon);
        }
        if (selectedTimeSlot.equals("Evening")) {
            return job.scheduleSlots().stream().anyMatch(JobBrowseFilter::slotStartsInEvening);
        }
        return true;
    }

    private static boolean contains(String rawValue, String keyword) {
        return rawValue != null && rawValue.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private static int slotStartHour(String slot) {
        if (slot == null || slot.isBlank()) {
            return -1;
        }

        int firstDash = slot.indexOf('-');
        if (firstDash < 0 || firstDash + 1 >= slot.length()) {
            return -1;
        }
        String timeRange = slot.substring(firstDash + 1);
        int secondDash = timeRange.indexOf('-');
        if (secondDash < 0) {
            return -1;
        }
        String startTime = timeRange.substring(0, secondDash);
        String[] parts = startTime.split(":");
        if (parts.length == 0) {
            return -1;
        }

        try {
            return Integer.parseInt(parts[0].trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static boolean slotStartsInMorning(String slot) {
        int startHour = slotStartHour(slot);
        return startHour >= 5 && startHour < 12;
    }

    private static boolean slotStartsInAfternoon(String slot) {
        int startHour = slotStartHour(slot);
        return startHour >= 12 && startHour < 17;
    }

    private static boolean slotStartsInEvening(String slot) {
        int startHour = slotStartHour(slot);
        return startHour >= 17 && startHour <= 23;
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
