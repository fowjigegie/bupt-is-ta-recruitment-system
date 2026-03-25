package com.bupt.tarecruitment.applicant;

public interface CvTextStorage {
    String saveCv(String userId, String cvContent);

    String loadCv(String relativePath);
}
