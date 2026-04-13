package com.bupt.tarecruitment.applicant;

/**
 * 定义简历原始文本内容的存储接口。
 */
public interface CvTextStorage {
    // 把 CV 正文保存到真正的文本文件里，并返回可写回 metadata 的相对路径。
    String saveCv(String applicantUserId, String cvId, String cvContent);

    // 根据 metadata 里保存的相对路径，把完整 CV 正文重新读出来。
    String loadCv(String relativePath);
}
