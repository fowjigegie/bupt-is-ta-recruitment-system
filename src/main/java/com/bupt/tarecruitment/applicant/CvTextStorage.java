package com.bupt.tarecruitment.applicant;

public interface CvTextStorage {
    String saveApplicationCv(String applicantUserId, String applicationId, String cvContent);

    String loadCv(String relativePath);
}
