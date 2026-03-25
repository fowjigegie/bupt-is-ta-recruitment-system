package com.bupt.tarecruitment.applicant;

import java.util.List;
import java.util.Optional;

public interface ApplicantCvRepository {
    Optional<ApplicantCv> findByCvId(String cvId);

    List<ApplicantCv> findByOwnerUserId(String ownerUserId);

    List<ApplicantCv> findAll();

    void save(ApplicantCv applicantCv);
}
