package com.bupt.tarecruitment.applicant;

import java.util.List;
import java.util.Optional;

public interface ApplicantCvRepository {
    // 按 cvId 查单份 CV 元数据，用于加载、编辑、提交申请时选择指定 CV。
    Optional<ApplicantCv> findByCvId(String cvId);

    // 按 applicant userId 查询这个人拥有的全部 CV。
    List<ApplicantCv> findByOwnerUserId(String ownerUserId);

    // 返回全部 CV 元数据，主要给编号生成器或调试场景使用。
    List<ApplicantCv> findAll();

    // 统一的新增/覆盖保存入口。
    // 如果同一个 cvId 已存在，具体实现会把旧记录替换掉。
    void save(ApplicantCv applicantCv);
}
