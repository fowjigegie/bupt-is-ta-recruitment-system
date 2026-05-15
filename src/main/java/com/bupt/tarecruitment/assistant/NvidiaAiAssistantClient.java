package com.bupt.tarecruitment.assistant;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * Minimal OpenAI-compatible chat-completions client.
 *
 * <p>The class name is kept for compatibility with the existing UI wiring.
 */
public final class NvidiaAiAssistantClient {
    private static final URI NVIDIA_CHAT_COMPLETIONS_URI =
        URI.create("https://integrate.api.nvidia.com/v1/chat/completions");
    private static final String DASHSCOPE_COMPATIBLE_BASE =
        "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private static final String DEFAULT_NVIDIA_MODEL = "z-ai/glm4.7";
    private static final String DEFAULT_DASHSCOPE_MODEL = "qwen-plus";
    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;
    private final URI chatCompletionsUri;
    private final String providerName;
    private final boolean nvidiaProvider;

    public NvidiaAiAssistantClient(String apiKey) {
        this(apiKey, DEFAULT_NVIDIA_MODEL);
    }

    public NvidiaAiAssistantClient(String apiKey, String model) {
        this(apiKey, model, NVIDIA_CHAT_COMPLETIONS_URI, "NVIDIA", true);
    }

    private NvidiaAiAssistantClient(
        String apiKey,
        String model,
        URI chatCompletionsUri,
        String providerName,
        boolean nvidiaProvider
    ) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .version(HttpClient.Version.HTTP_1_1)
            .build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? DEFAULT_NVIDIA_MODEL : model.trim();
        this.chatCompletionsUri = chatCompletionsUri;
        this.providerName = providerName;
        this.nvidiaProvider = nvidiaProvider;
    }

    public static Optional<NvidiaAiAssistantClient> fromEnvironment() {
        String dashScopeApiKey = firstNonBlank(
            System.getenv("DASHSCOPE_API_KEY"),
            System.getenv("ALIYUN_API_KEY")
        );
        if (dashScopeApiKey != null) {
            String baseUrl = firstNonBlank(
                System.getenv("DASHSCOPE_API_BASE"),
                System.getenv("ALIYUN_API_BASE"),
                System.getenv("AI_API_BASE"),
                DASHSCOPE_COMPATIBLE_BASE
            );
            String model = firstNonBlank(
                System.getenv("DASHSCOPE_AI_MODEL"),
                System.getenv("ALIYUN_AI_MODEL"),
                System.getenv("AI_MODEL"),
                DEFAULT_DASHSCOPE_MODEL
            );
            return Optional.of(new NvidiaAiAssistantClient(
                dashScopeApiKey,
                model,
                chatCompletionsUri(baseUrl),
                "Alibaba Cloud DashScope",
                false
            ));
        }

        String genericApiKey = System.getenv("AI_API_KEY");
        if (genericApiKey != null && !genericApiKey.isBlank()) {
            String baseUrl = firstNonBlank(System.getenv("AI_API_BASE"), DASHSCOPE_COMPATIBLE_BASE);
            String model = firstNonBlank(System.getenv("AI_MODEL"), DEFAULT_DASHSCOPE_MODEL);
            String provider = firstNonBlank(System.getenv("AI_PROVIDER_NAME"), "OpenAI-compatible");
            return Optional.of(new NvidiaAiAssistantClient(
                genericApiKey,
                model,
                chatCompletionsUri(baseUrl),
                provider,
                false
            ));
        }

        String nvidiaApiKey = System.getenv("NVIDIA_API_KEY");
        if (nvidiaApiKey == null || nvidiaApiKey.isBlank()) {
            return Optional.empty();
        }
        String model = System.getenv("NVIDIA_AI_MODEL");
        return Optional.of(new NvidiaAiAssistantClient(nvidiaApiKey, model));
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public String model() {
        return providerName + " / " + model;
    }

    public String chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, 360);
    }

    public String chat(String systemPrompt, String userPrompt, int maxTokens) {
        if (!isConfigured()) {
            throw new IllegalStateException("Cloud API key is not configured.");
        }
        int safeMaxTokens = Math.max(120, Math.min(1200, maxTokens));

        String extraFields = nvidiaProvider
            ? """
              ,
              "chat_template_kwargs": {
                "enable_thinking": false
              }
              """
            : "";

        String requestJson = """
            {
              "model": "%s",
              "messages": [
                { "role": "system", "content": "%s" },
                { "role": "user", "content": "%s" }
              ],
              "temperature": 0.3,
              "max_tokens": %d,
              "stream": false%s
            }
            """.formatted(
                jsonEscape(model),
                jsonEscape(systemPrompt),
                jsonEscape(userPrompt),
                safeMaxTokens,
                extraFields
            );

        HttpRequest request = HttpRequest.newBuilder(chatCompletionsUri)
            .timeout(Duration.ofSeconds(120))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
            .build();

        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return sendOnce(request);
            } catch (RuntimeException exception) {
                lastFailure = exception;
                if (attempt == 2 || !isRetryable(exception)) {
                    throw exception;
                }
                sleepBeforeRetry();
            }
        }
        throw lastFailure == null ? new IllegalStateException("Cloud API request failed.") : lastFailure;
    }

    private String sendOnce(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                    providerName + " request failed with HTTP " + response.statusCode() + ": " + response.body()
                );
            }
            return extractContent(response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to call " + providerName + ".", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(providerName + " call was interrupted.", exception);
        }
    }

    private static boolean isRetryable(RuntimeException exception) {
        String message = exception.getMessage();
        if (message != null && (message.contains("HTTP 502") || message.contains("HTTP 503") || message.contains("HTTP 504"))) {
            return true;
        }
        Throwable cause = exception.getCause();
        return cause instanceof java.net.http.HttpTimeoutException;
    }

    private static void sleepBeforeRetry() {
        try {
            Thread.sleep(1200);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    static String extractContent(String responseJson) {
        String json = responseJson == null ? "" : responseJson;
        int messageIndex = json.indexOf("\"message\"");
        int searchStart = Math.max(messageIndex, 0);
        int contentIndex = json.indexOf("\"content\"", searchStart);
        if (contentIndex < 0) {
            throw new IllegalStateException("Cloud API response did not contain a chat message.");
        }

        int colonIndex = json.indexOf(':', contentIndex + "\"content\"".length());
        if (colonIndex < 0) {
            throw new IllegalStateException("Cloud API response did not contain a chat message.");
        }

        int valueStart = -1;
        for (int index = colonIndex + 1; index < json.length(); index++) {
            char character = json.charAt(index);
            if (!Character.isWhitespace(character)) {
                if (character == '"') {
                    valueStart = index + 1;
                }
                break;
            }
        }
        if (valueStart < 0) {
            throw new IllegalStateException("Cloud API response did not contain a chat message.");
        }

        StringBuilder rawContent = new StringBuilder();
        boolean escaped = false;
        for (int index = valueStart; index < json.length(); index++) {
            char character = json.charAt(index);
            if (escaped) {
                rawContent.append('\\').append(character);
                escaped = false;
                continue;
            }
            if (character == '\\') {
                escaped = true;
                continue;
            }
            if (character == '"') {
                return jsonUnescape(rawContent.toString()).trim();
            }
            rawContent.append(character);
        }

        throw new IllegalStateException("Cloud API response did not contain a complete chat message.");
    }

    private static URI chatCompletionsUri(String baseUrl) {
        String normalized = (baseUrl == null || baseUrl.isBlank())
            ? DASHSCOPE_COMPATIBLE_BASE
            : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!normalized.endsWith("/chat/completions")) {
            normalized = normalized + "/chat/completions";
        }
        return URI.create(normalized);
    }

    private static String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate.trim();
            }
        }
        return null;
    }

    static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private static String jsonUnescape(String value) {
        StringBuilder unescaped = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character != '\\' || index + 1 >= value.length()) {
                unescaped.append(character);
                continue;
            }

            char escaped = value.charAt(++index);
            switch (escaped) {
                case '"' -> unescaped.append('"');
                case '\\' -> unescaped.append('\\');
                case '/' -> unescaped.append('/');
                case 'b' -> unescaped.append('\b');
                case 'f' -> unescaped.append('\f');
                case 'n' -> unescaped.append('\n');
                case 'r' -> unescaped.append('\r');
                case 't' -> unescaped.append('\t');
                case 'u' -> {
                    if (index + 4 >= value.length()) {
                        unescaped.append("\\u");
                    } else {
                        String hex = value.substring(index + 1, index + 5);
                        unescaped.append((char) Integer.parseInt(hex, 16));
                        index += 4;
                    }
                }
                default -> unescaped.append(escaped);
            }
        }
        return unescaped.toString();
    }
}
