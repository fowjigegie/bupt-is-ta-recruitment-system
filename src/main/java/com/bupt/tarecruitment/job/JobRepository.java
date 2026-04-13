package com.bupt.tarecruitment.job;

import java.util.List;
import java.util.Optional;

/**
 * 定义岗位数据的持久化接口。
 */
public interface JobRepository {
    Optional<JobPosting> findByJobId(String jobId);

    List<JobPosting> findAll();

    void save(JobPosting jobPosting);
}
