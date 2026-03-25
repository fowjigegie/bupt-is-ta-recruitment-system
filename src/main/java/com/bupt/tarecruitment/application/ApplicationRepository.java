package com.bupt.tarecruitment.application;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository {
    Optional<JobApplication> findByApplicationId(String applicationId);

    List<JobApplication> findByApplicantUserId(String applicantUserId);

    List<JobApplication> findAll();

    void save(JobApplication application);
}
