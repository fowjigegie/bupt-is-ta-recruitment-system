package com.bupt.tarecruitment.job;

import com.bupt.tarecruitment.auth.UserAccessPolicy;
import com.bupt.tarecruitment.auth.UserRepository;
import com.bupt.tarecruitment.auth.UserRole;

import java.util.List;
import java.util.Objects;

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
        int weeklyHours,
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

        JobPosting posting = new JobPosting(
            jobIdGenerator.nextJobId(),
            organiserId.trim(),
            title.trim(),
            moduleOrActivity.trim(),
            description.trim(),
            normalizeList(requiredSkills),
            weeklyHours,
            normalizeList(scheduleSlots),
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
        jobRepository.save(posting);
        return posting;
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

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }
}
