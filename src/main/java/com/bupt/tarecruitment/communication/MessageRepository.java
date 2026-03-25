package com.bupt.tarecruitment.communication;

import java.util.List;

public interface MessageRepository {
    List<InquiryMessage> findByJobId(String jobId);

    void save(InquiryMessage message);
}
