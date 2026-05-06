package com.bupt.tarecruitment.assistant;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class NvidiaAiAssistantClientSmokeTest {
    @Test
    void canCallNvidiaChatCompletionsWhenApiKeyIsConfigured() {
        String apiKey = System.getenv("NVIDIA_API_KEY");
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(), "NVIDIA_API_KEY is not configured.");
        Assumptions.assumeTrue(
            "true".equalsIgnoreCase(System.getenv("RUN_NVIDIA_SMOKE")),
            "RUN_NVIDIA_SMOKE=true is required for live NVIDIA API tests."
        );

        NvidiaAiAssistantClient client = new NvidiaAiAssistantClient(apiKey);
        String answer = client.chat(
            "You are a concise test assistant.",
            "Reply with one short Chinese greeting."
        );

        assertFalse(answer.isBlank());
    }
}
