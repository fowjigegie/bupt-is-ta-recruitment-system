package com.bupt.tarecruitment.applicant;

import java.util.List;
import java.util.Optional;

public interface ApplicantProfileRepository {
    Optional<ApplicantProfile> findByUserId(String userId);

    List<ApplicantProfile> findAll();

    void save(ApplicantProfile profile);
}
