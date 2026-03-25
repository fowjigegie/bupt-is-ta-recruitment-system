package com.bupt.tarecruitment.applicant;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class ApplicantCvLibraryService {
    public static final int MAX_CVS_PER_APPLICANT = 10;

    private final ApplicantProfileRepository profileRepository;
    private final ApplicantCvRepository cvRepository;
    private final ApplicantCvIdGenerator cvIdGenerator;
    private final CvTextStorage cvStorage;

    public ApplicantCvLibraryService(
        ApplicantProfileRepository profileRepository,
        ApplicantCvRepository cvRepository,
        ApplicantCvIdGenerator cvIdGenerator,
        CvTextStorage cvStorage
    ) {
        this.profileRepository = Objects.requireNonNull(profileRepository);
        this.cvRepository = Objects.requireNonNull(cvRepository);
        this.cvIdGenerator = Objects.requireNonNull(cvIdGenerator);
        this.cvStorage = Objects.requireNonNull(cvStorage);
    }

    public ApplicantCv createCv(String ownerUserId, String title, String cvContent) {
        requireExistingProfile(ownerUserId);
        requireNonBlank(title, "title");
        requireNonBlank(cvContent, "cvContent");
        requireWithinCvLimit(ownerUserId);

        String cvId = cvIdGenerator.nextCvId();
        String fileName = cvStorage.saveCv(ownerUserId, cvId, cvContent);
        LocalDateTime now = LocalDateTime.now();

        ApplicantCv applicantCv = new ApplicantCv(
            cvId,
            ownerUserId,
            title.trim(),
            fileName,
            now,
            now
        );
        cvRepository.save(applicantCv);
        return applicantCv;
    }

    public ApplicantCv updateCvContent(String cvId, String cvContent) {
        requireNonBlank(cvId, "cvId");
        requireNonBlank(cvContent, "cvContent");

        ApplicantCv existingCv = cvRepository.findByCvId(cvId)
            .orElseThrow(() -> new IllegalArgumentException("No CV exists for cvId: " + cvId));
        String fileName = cvStorage.saveCv(existingCv.ownerUserId(), existingCv.cvId(), cvContent);
        ApplicantCv updatedCv = new ApplicantCv(
            existingCv.cvId(),
            existingCv.ownerUserId(),
            existingCv.title(),
            fileName,
            existingCv.createdAt(),
            LocalDateTime.now()
        );
        cvRepository.save(updatedCv);
        return updatedCv;
    }

    public ApplicantCv getCvById(String cvId) {
        requireNonBlank(cvId, "cvId");
        return cvRepository.findByCvId(cvId)
            .orElseThrow(() -> new IllegalArgumentException("No CV exists for cvId: " + cvId));
    }

    public String loadCvContentByCvId(String cvId) {
        ApplicantCv applicantCv = getCvById(cvId);
        return cvStorage.loadCv(applicantCv.fileName());
    }

    public List<ApplicantCv> listCvsByUserId(String ownerUserId) {
        requireNonBlank(ownerUserId, "ownerUserId");
        return cvRepository.findByOwnerUserId(ownerUserId);
    }

    private ApplicantProfile requireExistingProfile(String userId) {
        return profileRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("No applicant profile exists for userId: " + userId));
    }

    private void requireWithinCvLimit(String ownerUserId) {
        int existingCvCount = cvRepository.findByOwnerUserId(ownerUserId).size();
        if (existingCvCount >= MAX_CVS_PER_APPLICANT) {
            throw new IllegalArgumentException(
                "Each applicant can keep at most %d CVs.".formatted(MAX_CVS_PER_APPLICANT)
            );
        }
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }
}
