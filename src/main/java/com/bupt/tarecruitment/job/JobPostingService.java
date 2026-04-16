package com.bupt.tarecruitment.job;

import com.bupt.tarecruitment.auth.UserAccessPolicy;
import com.bupt.tarecruitment.auth.UserRepository;
import com.bupt.tarecruitment.auth.UserRole;
import com.bupt.tarecruitment.common.schedule.ScheduleSlot;

import java.util.List;
import java.util.Objects;

/**
 * 处理岗位发布、更新和状态变更。
 */
public final class JobPostingService {
    private final JobRepository jobRepository;
    private final JobIdGenerator jobIdGenerator;
    private final UserAccessPolicy userAccessPolicy;

    public JobPostingService(JobRepository jobRepository, JobIdGenerator jobIdGenerator) {
        this(jobRepository, jobIdGenerator, UserAccessPolicy.noOp());
    }

    public JobPostingService(JobRepository jobRepository, JobIdGenerator jobIdGenerator, UserRepository userRepository) {
        this(jobRepository, jobIdGenerator, new UserAccessPolicy(userRepository));
    }

    private JobPostingService(JobRepository jobRepository, JobIdGenerator jobIdGenerator, UserAccessPolicy userAccessPolicy) {
        this.jobRepository = Objects.requireNonNull(jobRepository);
        this.jobIdGenerator = Objects.requireNonNull(jobIdGenerator);
        this.userAccessPolicy = Objects.requireNonNull(userAccessPolicy);
    }

    public JobPosting publish(
        String organiserId,
        String title,
        String moduleOrActivity,
        String description,
        List<String> requiredSkills,
        double weeklyHours,
        List<String> scheduleSlots
    ) {
        requireNonBlank(organiserId, "organiserId");
        requireNonBlank(title, "title");
        requireNonBlank(moduleOrActivity, "moduleOrActivity");
        requireNonBlank(description, "description");

        if (weeklyHours <= 0) {
            throw new IllegalArgumentException("weeklyHours must be greater than 0.");
        }

        userAccessPolicy.requireActiveUserWithRole(organiserId, UserRole.MO);

        List<String> normalizedScheduleSlots = normalizeScheduleSlots(scheduleSlots);

        JobPosting posting = new JobPosting(
            jobIdGenerator.nextJobId(),
            organiserId.trim(),
            title.trim(),
            moduleOrActivity.trim(),
            description.trim(),
            normalizeList(requiredSkills),
            weeklyHours,
            normalizedScheduleSlots,
            JobStatus.OPEN
        );
        jobRepository.save(posting);
        return posting;
    }

    public JobPosting publish(JobPosting posting) {
        Objects.requireNonNull(posting);

        requireNonBlank(posting.organiserId(), "organiserId");
        requireNonBlank(posting.title(), "title");
        requireNonBlank(posting.moduleOrActivity(), "moduleOrActivity");
        requireNonBlank(posting.description(), "description");
        if (posting.weeklyHours() <= 0) {
            throw new IllegalArgumentException("weeklyHours must be greater than 0.");
        }

        userAccessPolicy.requireActiveUserWithRole(posting.organiserId(), UserRole.MO);

        List<String> normalizedScheduleSlots = normalizeScheduleSlots(
            posting.scheduleSlots(),
            posting.status() != JobStatus.CLOSED
        );

        JobPosting normalizedPosting = new JobPosting(
            posting.jobId(),
            posting.organiserId().trim(),
            posting.title().trim(),
            posting.moduleOrActivity().trim(),
            posting.description().trim(),
            normalizeList(posting.requiredSkills()),
            posting.weeklyHours(),
            normalizedScheduleSlots,
            posting.status()
        );
        jobRepository.save(normalizedPosting);
        return normalizedPosting;
    }

    private List<String> normalizeList(List<String> rawValues) {
        if (rawValues == null) {
            return List.of();
        }

        return rawValues.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList();
    }

    private List<String> normalizeScheduleSlots(List<String> rawValues) {
        return normalizeScheduleSlots(rawValues, true);
    }

    private List<String> normalizeScheduleSlots(List<String> rawValues, boolean required) {
        List<String> normalizedScheduleSlots = normalizeList(rawValues);
        if (required && normalizedScheduleSlots.isEmpty()) {
            throw new IllegalArgumentException("At least one schedule slot is required.");
        }
        for (String scheduleSlot : normalizedScheduleSlots) {
            ScheduleSlot.parse(scheduleSlot);
        }
        return normalizedScheduleSlots;
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }
}
