package com.bupt.tarecruitment.job;

import java.util.List;
import java.util.Optional;

public interface JobRepository {
    Optional<JobPosting> findByJobId(String jobId);

    List<JobPosting> findAll();

    void save(JobPosting jobPosting);
}
