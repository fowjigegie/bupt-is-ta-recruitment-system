package com.bupt.tarecruitment.applicant;

import java.util.Optional;

/**
 * Stores optional binary attachments (currently PDF) that belong to a CV.
 */
public interface CvAttachmentStorage {
    String savePdf(String applicantUserId, String cvId, byte[] pdfBytes);

    Optional<String> findPdfPath(String applicantUserId, String cvId);

    byte[] loadPdf(String relativePath);

    boolean deletePdf(String relativePath);
}
