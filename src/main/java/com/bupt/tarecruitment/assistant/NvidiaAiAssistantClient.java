package com.bupt.tarecruitment.assistant;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal NVIDIA NIM chat-completions client.
 */
public final class NvidiaAiAssistantClient {
    private static final URI CHAT_COMPLETIONS_URI =
        URI.create("https://integrate.api.nvidia.com/v1/chat/completions");
    private static final String DEFAULT_MODEL = "z-ai/glm4.7";
    private static final Pattern CONTENT_PATTERN = Pattern.compile(
        "\"message\"\\s*:\\s*\\{.*?\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
        Pattern.DOTALL
    );

    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;

    public NvidiaAiAssistantClient(String apiKey) {
        this(apiKey, DEFAULT_MODEL);
    }

    public NvidiaAiAssistantClient(String apiKey, String model) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .version(HttpClient.Version.HTTP_1_1)
            .build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
    }

    public static Optional<NvidiaAiAssistantClient> fromEnvironment() {
        String apiKey = System.getenv("NVIDIA_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        String model = System.getenv("NVIDIA_AI_MODEL");
        return Optional.of(new NvidiaAiAssistantClient(apiKey, model));
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public String model() {
        return model;
    }

    public String chat(String systemPrompt, String userPrompt) {
        if (!isConfigured()) {
            throw new IllegalStateException("NVIDIA_API_KEY is not configured.");
        }

        String requestJson = """
            {
              "model": "%s",
              "messages": [
                { "role": "system", "content": "%s" },
                { "role": "user", "content": "%s" }
              ],
              "temperature": 0.3,
              "max_tokens": 360,
              "stream": false,
              "chat_template_kwargs": {
                "enable_thinking": false
              }
            }
            """.formatted(
                jsonEscape(model),
                jsonEscape(systemPrompt),
                jsonEscape(userPrompt)
            );

        HttpRequest request = HttpRequest.newBuilder(CHAT_COMPLETIONS_URI)
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
        throw lastFailure == null ? new IllegalStateException("NVIDIA API request failed.") : lastFailure;
    }

    private String sendOnce(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                    "NVIDIA API request failed with HTTP " + response.statusCode() + ": " + response.body()
                );
            }
            return extractContent(response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to call NVIDIA API.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("NVIDIA API call was interrupted.", exception);
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
        Matcher matcher = CONTENT_PATTERN.matcher(responseJson == null ? "" : responseJson);
        if (!matcher.find()) {
            throw new IllegalStateException("NVIDIA API response did not contain a chat message.");
        }
        return jsonUnescape(matcher.group(1)).trim();
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
