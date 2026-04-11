package com.bupt.tarecruitment.applicant;

import com.bupt.tarecruitment.application.ApplicationRepository;
import com.bupt.tarecruitment.application.JobApplication;

import java.util.Objects;
import java.util.Optional;

public final class ApplicantCvService {
    private final ApplicationRepository applicationRepository;
    private final ApplicantCvRepository cvRepository;
    private final CvTextStorage cvStorage;

    public ApplicantCvService(
        ApplicationRepository applicationRepository,
        ApplicantCvRepository cvRepository,
        CvTextStorage cvStorage
    ) {
        this.applicationRepository = Objects.requireNonNull(applicationRepository);
        this.cvRepository = Objects.requireNonNull(cvRepository);
        this.cvStorage = Objects.requireNonNull(cvStorage);
    }

    // 这个服务更多是“CV 与 application 的关联”。
    // 虽然主要用于提交申请后的引用，但它和 US02 的 CV library 紧密相关，
    // 因为 application 最终保存的只是 cvId，而不是整份 CV 文本。
    public JobApplication attachCvToApplication(String applicationId, String cvId) {
        requireNonBlank(applicationId, "applicationId");
        requireNonBlank(cvId, "cvId");

        JobApplication existingApplication = requireExistingApplication(applicationId);
        ApplicantCv applicantCv = requireExistingCv(cvId);
        // 只允许 applicant 把“自己的 CV”挂到“自己的申请”上，避免串用别人简历。
        if (!applicantCv.ownerUserId().equals(existingApplication.applicantUserId())) {
            throw new IllegalArgumentException("The selected CV does not belong to applicantUserId: " + existingApplication.applicantUserId());
        }

        JobApplication updatedApplication = new JobApplication(
            existingApplication.applicationId(),
            existingApplication.jobId(),
            existingApplication.applicantUserId(),
            applicantCv.cvId(),
            existingApplication.status(),
            existingApplication.submittedAt(),
            existingApplication.reviewerNote()
        );

        applicationRepository.save(updatedApplication);
        return updatedApplication;
    }

    // 查询某条申请当前绑定了哪一份 CV。
    public Optional<String> getAssignedCvId(String applicationId) {
        requireNonBlank(applicationId, "applicationId");

        return applicationRepository.findByApplicationId(applicationId)
            .map(JobApplication::cvId)
            .filter(cvId -> !cvId.isBlank());
    }

    // 先根据 application 拿到 cvId，再回查 CV 元数据。
    public ApplicantCv getAssignedCv(String applicationId) {
        requireNonBlank(applicationId, "applicationId");

        JobApplication application = requireExistingApplication(applicationId);
        if (application.cvId().isBlank()) {
            throw new IllegalArgumentException("No CV has been submitted for applicationId: " + applicationId);
        }

        return requireExistingCv(application.cvId());
    }

    // 供 MO 查看或详情页展示时读取完整 CV 正文。
    public String loadCvContentByApplicationId(String applicationId) {
        ApplicantCv applicantCv = getAssignedCv(applicationId);
        return cvStorage.loadCv(applicantCv.fileName());
    }

    private JobApplication requireExistingApplication(String applicationId) {
        return applicationRepository.findByApplicationId(applicationId)
            .orElseThrow(() -> new IllegalArgumentException("No application exists for applicationId: " + applicationId));
    }

    private ApplicantCv requireExistingCv(String cvId) {
        return cvRepository.findByCvId(cvId)
            .orElseThrow(() -> new IllegalArgumentException("No CV exists for cvId: " + cvId));
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }
}
