package com.bupt.tarecruitment.applicant;

import com.bupt.tarecruitment.auth.UserAccessPolicy;
import com.bupt.tarecruitment.auth.UserRepository;
import com.bupt.tarecruitment.auth.UserRole;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class ApplicantCvLibraryService {
    public static final int MAX_CVS_PER_APPLICANT = 10;

    private final ApplicantProfileRepository profileRepository;
    private final ApplicantCvRepository cvRepository;
    private final ApplicantCvIdGenerator cvIdGenerator;
    private final CvTextStorage cvStorage;
    private final UserAccessPolicy userAccessPolicy;

    public ApplicantCvLibraryService(
        ApplicantProfileRepository profileRepository,
        ApplicantCvRepository cvRepository,
        ApplicantCvIdGenerator cvIdGenerator,
        CvTextStorage cvStorage
    ) {
        this(profileRepository, cvRepository, cvIdGenerator, cvStorage, UserAccessPolicy.noOp());
    }

    public ApplicantCvLibraryService(
        ApplicantProfileRepository profileRepository,
        ApplicantCvRepository cvRepository,
        ApplicantCvIdGenerator cvIdGenerator,
        CvTextStorage cvStorage,
        UserRepository userRepository
    ) {
        this(profileRepository, cvRepository, cvIdGenerator, cvStorage, new UserAccessPolicy(userRepository));
    }

    private ApplicantCvLibraryService(
        ApplicantProfileRepository profileRepository,
        ApplicantCvRepository cvRepository,
        ApplicantCvIdGenerator cvIdGenerator,
        CvTextStorage cvStorage,
        UserAccessPolicy userAccessPolicy
    ) {
        this.profileRepository = Objects.requireNonNull(profileRepository);
        this.cvRepository = Objects.requireNonNull(cvRepository);
        this.cvIdGenerator = Objects.requireNonNull(cvIdGenerator);
        this.cvStorage = Objects.requireNonNull(cvStorage);
        this.userAccessPolicy = Objects.requireNonNull(userAccessPolicy);
    }

    // US02 的核心服务：
    // 负责“创建 CV / 更新 CV 正文 / 列出 applicant 名下所有 CV / 按 id 读取 CV”。
    // 它要求 applicant 先有 profile，再允许维护自己的 CV library。
    public ApplicantCv createCv(String ownerUserId, String title, String cvContent) {
        userAccessPolicy.requireActiveUserWithRole(ownerUserId, UserRole.APPLICANT);
        requireExistingProfile(ownerUserId);
        requireNonBlank(title, "title");
        requireNonBlank(cvContent, "cvContent");
        requireWithinCvLimit(ownerUserId);

        // 先生成新的 cvId，再把真正的简历文本写进 data/cvs/<userId>/<cvId>.txt。
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

    // 更新时只替换“正文文件”和 updatedAt，不会改动 cvId、owner、createdAt。
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

    // 只查 metadata，不会自动把正文文件一并读出。
    public ApplicantCv getCvById(String cvId) {
        requireNonBlank(cvId, "cvId");
        return cvRepository.findByCvId(cvId)
            .orElseThrow(() -> new IllegalArgumentException("No CV exists for cvId: " + cvId));
    }

    // 这是“metadata -> txt 文件正文”的桥接步骤。
    public String loadCvContentByCvId(String cvId) {
        ApplicantCv applicantCv = getCvById(cvId);
        return cvStorage.loadCv(applicantCv.fileName());
    }

    // 页面顶部的 CV 标签栏、申请岗位时的 CV 下拉框，都会走这里列出当前 applicant 的 CV。
    public List<ApplicantCv> listCvsByUserId(String ownerUserId) {
        requireNonBlank(ownerUserId, "ownerUserId");
        userAccessPolicy.requireActiveUserWithRole(ownerUserId, UserRole.APPLICANT);
        return cvRepository.findByOwnerUserId(ownerUserId);
    }

    // US02 约束：没有先创建 applicant profile，就不能开始维护 CV library。
    private ApplicantProfile requireExistingProfile(String userId) {
        return profileRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("No applicant profile exists for userId: " + userId));
    }

    // 当前设计允许一个 applicant 保留多份 CV，但数量上限是 10 份。
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
