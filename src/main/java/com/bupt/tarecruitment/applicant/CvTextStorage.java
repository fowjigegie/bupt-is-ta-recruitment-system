package com.bupt.tarecruitment.applicant;

public interface CvTextStorage {
    String saveCv(String applicantUserId, String cvId, String cvContent);

    String loadCv(String relativePath);
}
