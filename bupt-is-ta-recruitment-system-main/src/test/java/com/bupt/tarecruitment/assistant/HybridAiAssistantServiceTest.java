package com.bupt.tarecruitment.assistant;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HybridAiAssistantServiceTest {
    @Test
    void fallsBackToLocalAssistantWhenNvidiaIsNotConfigured() {
        AiAssistantService fallback = new AiAssistantService() {
            @Override
            public String answer(String rawQuestion, String applicantUserId, String selectedJobId) {
                return "local answer";
            }
        };

        HybridAiAssistantService service = new HybridAiAssistantService(fallback, Optional.empty());

        assertFalse(service.isRealApiEnabled());
        assertEquals("local answer", service.answer("question", "ta001", "job001"));
    }
}
