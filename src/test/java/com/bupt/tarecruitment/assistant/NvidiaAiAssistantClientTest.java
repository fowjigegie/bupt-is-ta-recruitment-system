package com.bupt.tarecruitment.assistant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NvidiaAiAssistantClientTest {
    @Test
    void extractsAssistantContentFromNvidiaResponse() {
        String responseJson = """
            {
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "Hello\\nChinese reply: \\\"你好\\\""
                  },
                  "finish_reason": "stop"
                }
              ]
            }
            """;

        assertEquals(
            "Hello\nChinese reply: \"你好\"",
            NvidiaAiAssistantClient.extractContent(responseJson)
        );
    }

    @Test
    void escapesPromptTextForJsonRequest() {
        assertEquals(
            "line 1\\nquote: \\\"ok\\\"",
            NvidiaAiAssistantClient.jsonEscape("line 1\nquote: \"ok\"")
        );
    }
}
