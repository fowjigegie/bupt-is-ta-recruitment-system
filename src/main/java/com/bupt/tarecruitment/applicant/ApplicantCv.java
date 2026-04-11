package com.bupt.tarecruitment.applicant;

import java.time.LocalDateTime;
import java.util.Objects;

// US02: 这是一条 CV 元数据记录。
// 它不直接保存整份简历正文，而是保存“这份 CV 属于谁、标题是什么、正文存在哪个 txt 文件里”。
public record ApplicantCv(
    String cvId,
    String ownerUserId,
    String title,
    String fileName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public ApplicantCv {
        Objects.requireNonNull(cvId);
        Objects.requireNonNull(ownerUserId);
        Objects.requireNonNull(title);
        Objects.requireNonNull(fileName);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(updatedAt);
    }
}
