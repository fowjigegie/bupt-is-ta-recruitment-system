package com.bupt.tarecruitment.communication;

import java.util.List;

public interface MessageRepository {
    // 读取全部消息，消息页会在内存中按 job + peer 重新组 conversation thread。
    List<InquiryMessage> findAll();

    // 按 jobId 取消息，供某个岗位的单条会话使用。
    List<InquiryMessage> findByJobId(String jobId);

    // 统一的新增/覆盖保存入口。
    void save(InquiryMessage message);
}
