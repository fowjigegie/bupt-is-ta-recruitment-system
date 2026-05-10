package com.bupt.tarecruitment.ai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Calls an OpenAI-compatible Chat Completions HTTP API. Configure with environment variables:
 * {@code OPENAI_API_KEY} or {@code AI_API_KEY}, JVM system property {@code openai.api.key},
 * or the first line of {@code data/openai-api-key.txt} (searched upward from the JVM working directory);
 * optional {@code OPENAI_API_BASE} (default {@code https://api.openai.com/v1}),
 * optional {@code OPENAI_MODEL} (default {@code gpt-4o-mini}).
 * <p>VPN / Clash: browsers often use the system proxy, but this client uses {@link HttpClient} only.
 * Set {@code HTTPS_PROXY} or {@code HTTP_PROXY} (e.g. {@code http://127.0.0.1:7890}) to your local <em>HTTP</em> proxy port,
 * or JVM {@code https.proxyHost} / {@code https.proxyPort}. SOCKS-only ports need a local HTTP forwarder.
 */
public final class OpenAiCompatibleClient {
    private static final Duration CONNECT_TIMEOUT_DIRECT = Duration.ofSeconds(25);
    private static final Duration CONNECT_TIMEOUT_PROXY = Duration.ofSeconds(45);
    /** End-to-end ceiling for request + response body (CompletionFuture deadline). */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(90);

    private final HttpClient httpClient = buildHttpClient();
    private final String apiBase;
    private final String apiKey;
    private final String model;

    public OpenAiCompatibleClient() {
        this(resolveApiBase(), resolveApiKey(), resolveModel());
    }

    public OpenAiCompatibleClient(String apiBase, String apiKey, String model) {
        this.apiBase = trimTrailingSlash(Objects.requireNonNull(apiBase));
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "gpt-4o-mini" : model.trim();
    }

    public static boolean isConfigured() {
        return !resolveApiKey().isBlank();
    }

    public boolean hasApiKey() {
        return !apiKey.isBlank();
    }

    /** Model id sent to the chat/completions API (from OPENAI_MODEL or default). */
    public String model() {
        return model;
    }

    public String complete(String systemPrompt, List<ChatTurn> priorTurns, String userMessage)
        throws IOException {
        if (!hasApiKey()) {
            throw new IllegalStateException("Missing API key: set OPENAI_API_KEY or AI_API_KEY.");
        }

        StringBuilder body = new StringBuilder(256);
        body.append("{\"model\":\"").append(jsonEscape(model)).append("\",\"messages\":[");
        body.append("{\"role\":\"system\",\"content\":\"").append(jsonEscape(systemPrompt)).append("\"}");
        for (ChatTurn turn : priorTurns) {
            body.append(',');
            body.append("{\"role\":\"").append(jsonEscape(turn.role()));
            body.append("\",\"content\":\"").append(jsonEscape(turn.content())).append("\"}");
        }
        body.append(',');
        body.append("{\"role\":\"user\",\"content\":\"").append(jsonEscape(userMessage)).append("\"}");
        body.append("],\"stream\":false}");

        URI uri = URI.create(apiBase + "/chat/completions");
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(REQUEST_TIMEOUT)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .header("User-Agent", "BUPT-TA-Recruitment/1.0 (Java HttpClient)")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
            .build();

        HttpResponse<String> response = execute(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("AI HTTP " + response.statusCode() + ": " + truncate(response.body(), 2000));
        }
        return extractAssistantContent(response.body());
    }

    /**
     * Uses async + {@code orTimeout} so the UI thread never waits indefinitely when DNS/TLS/connect hangs
     * (blocking {@link HttpClient#send} can stall on some Windows networks).
     */
    private HttpResponse<String> execute(HttpRequest request) throws IOException {
        try {
            return httpClient
                .sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .orTimeout(REQUEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                .join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof TimeoutException) {
                throw new IOException(timeoutHint(), cause);
            }
            if (cause instanceof IOException) {
                throw appendProxyHintIfConnectFailed((IOException) cause);
            }
            throw new IOException(appendProxyHint(cause != null ? cause.getMessage() : e.getMessage()), cause);
        }
    }

    private static IOException appendProxyHintIfConnectFailed(IOException io) {
        String msg = io.getMessage();
        if (msg != null && (msg.contains("timed out") || msg.contains("Timeout") || msg.contains("connection"))) {
            return new IOException(appendProxyHint(msg), io);
        }
        return io;
    }

    private static String appendProxyHint(String base) {
        return base + proxyEnvHint();
    }

    private static String proxyEnvHint() {
        if (explicitHttpProxyConfigured()) {
            return "";
        }
        return "\n\nTip (VPN/Clash): Java does not use your browser proxy. Set user environment HTTPS_PROXY="
            + "http://127.0.0.1:<HTTP-port> (Clash: often 7890; check your client) or JVM -Dhttps.proxyHost/-Dhttps.proxyPort.";
    }

    private static boolean explicitHttpProxyConfigured() {
        return firstNonBlank(
            System.getenv("HTTPS_PROXY"),
            System.getenv("https_proxy"),
            System.getenv("HTTP_PROXY"),
            System.getenv("http_proxy"),
            System.getProperty("https.proxyHost"),
            System.getProperty("http.proxyHost")
        ) != null;
    }

    private static String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String s : candidates) {
            if (s != null && !s.isBlank()) {
                return s;
            }
        }
        return null;
    }

    private static HttpClient buildHttpClient() {
        Duration connect = explicitHttpProxyConfigured() ? CONNECT_TIMEOUT_PROXY : CONNECT_TIMEOUT_DIRECT;
        HttpClient.Builder b = HttpClient.newBuilder()
            .connectTimeout(connect)
            .version(HttpClient.Version.HTTP_1_1);
        ProxySelector proxy = resolveProxySelector();
        if (proxy != null) {
            b.proxy(proxy);
        }
        return b.build();
    }

    /**
     * Uses {@code HTTPS_PROXY} / {@code HTTP_PROXY} (common on Windows after {@code setx}) or standard JVM proxy properties.
     * Returns {@code null} to keep the JDK default {@link ProxySelector} (rarely routes Java like the browser).
     */
    private static ProxySelector resolveProxySelector() {
        String raw = firstNonBlank(
            System.getenv("HTTPS_PROXY"),
            System.getenv("https_proxy"),
            System.getenv("HTTP_PROXY"),
            System.getenv("http_proxy")
        );
        if (raw != null && !raw.isBlank()) {
            ProxySelector fromEnv = parseHttpProxyUri(raw.trim());
            if (fromEnv != null) {
                return fromEnv;
            }
        }
        String host = firstNonBlank(System.getProperty("https.proxyHost"), System.getProperty("http.proxyHost"));
        if (host != null && !host.isBlank()) {
            String portProp = firstNonBlank(System.getProperty("https.proxyPort"), System.getProperty("http.proxyPort"));
            int port = 443;
            if (portProp != null && !portProp.isBlank()) {
                try {
                    port = Integer.parseInt(portProp.trim());
                } catch (NumberFormatException ignored) {
                    port = 443;
                }
            }
            return ProxySelector.of(new InetSocketAddress(host.trim(), port));
        }
        return null;
    }

    private static ProxySelector parseHttpProxyUri(String raw) {
        try {
            String normalized = raw.contains("://") ? raw : "http://" + raw;
            URI u = URI.create(normalized);
            String scheme = u.getScheme();
            if (scheme != null && scheme.toLowerCase().startsWith("socks")) {
                return null;
            }
            String h = u.getHost();
            int port = u.getPort();
            if (h == null || h.isEmpty()) {
                return null;
            }
            if (port < 0) {
                port = 80;
            }
            return ProxySelector.of(new InetSocketAddress(h, port));
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    private static String timeoutHint() {
        return appendProxyHint(
            "AI request timed out after " + REQUEST_TIMEOUT.toSeconds()
                + "s. Check VPN/proxy; try OPENAI_API_BASE for a reachable gateway; shorten profile/resume/job context."
        );
    }

    /**
     * Reads {@code choices[0].message.content} (OpenAI-compatible chat completions).
     * Anchoring on {@code "message"} avoids matching a stray {@code "content"} substring that can appear
     * earlier under {@code choices} (e.g. provider-specific metadata keys).
     */
    private static String extractAssistantContent(String json) throws IOException {
        int choicesIdx = json.indexOf("\"choices\"");
        if (choicesIdx < 0) {
            throw new IOException("Unexpected AI response (no choices): " + truncate(json, 500));
        }
        int messageIdx = json.indexOf("\"message\"", choicesIdx);
        if (messageIdx < 0) {
            throw new IOException("Unexpected AI response (no message): " + truncate(json, 500));
        }
        int contentKey = json.indexOf("\"content\"", messageIdx);
        if (contentKey < 0) {
            throw new IOException("Unexpected AI response (no content in message): " + truncate(json, 500));
        }
        int colon = json.indexOf(':', contentKey);
        if (colon < 0) {
            throw new IOException("Unexpected AI response (malformed content key).");
        }
        String content = parseJsonStringOrNull(json, colon + 1);
        if (content != null && !content.isEmpty()) {
            return content;
        }
        int refusalKey = json.indexOf("\"refusal\"", messageIdx);
        if (refusalKey > messageIdx) {
            int nextMsg = json.indexOf("\"message\"", messageIdx + 1);
            if (nextMsg < 0 || refusalKey < nextMsg) {
                int rColon = json.indexOf(':', refusalKey);
                if (rColon > 0) {
                    String refusal = parseJsonStringOrNull(json, rColon + 1);
                    if (refusal != null && !refusal.isEmpty()) {
                        return refusal;
                    }
                }
            }
        }
        return content == null ? "" : content;
    }

    /** After {@code :}, parses a JSON string or {@code null}; returns null for null, empty string for empty string. */
    private static String parseJsonStringOrNull(String json, int valueStart) throws IOException {
        int i = valueStart;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        if (i >= json.length()) {
            throw new IOException("Unexpected AI response (truncated).");
        }
        if (json.regionMatches(i, "null", 0, 4)) {
            return null;
        }
        if (json.charAt(i) != '"') {
            throw new IOException("Unexpected AI response (content/refusal not a string or null).");
        }
        return decodeJsonStringContents(json, i + 1);
    }

    private static String decodeJsonStringContents(String json, int afterOpeningQuote) throws IOException {
        int i = afterOpeningQuote;
        StringBuilder sb = new StringBuilder();
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '"') {
                break;
            }
            if (c == '\\' && i + 1 < json.length()) {
                char n = json.charAt(i + 1);
                if (n == 'n') {
                    sb.append('\n');
                } else if (n == 'r') {
                    sb.append('\r');
                } else if (n == 't') {
                    sb.append('\t');
                } else if (n == '"') {
                    sb.append('"');
                } else if (n == '\\') {
                    sb.append('\\');
                } else if (n == '/' ) {
                    sb.append('/');
                } else if (n == 'u' && i + 6 <= json.length()) {
                    String hex = json.substring(i + 2, i + 6);
                    try {
                        sb.append((char) Integer.parseInt(hex, 16));
                    } catch (NumberFormatException ignored) {
                        sb.append('?');
                    }
                    i += 6;
                    continue;
                } else {
                    sb.append(n);
                }
                i += 2;
                continue;
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    static String jsonEscape(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length() + 16);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '"') {
                sb.append("\\\"");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c < 0x20) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String resolveApiBase() {
        String fromEnv = System.getenv("OPENAI_API_BASE");
        if (fromEnv == null || fromEnv.isBlank()) {
            fromEnv = System.getenv("AI_API_BASE");
        }
        if (fromEnv == null || fromEnv.isBlank()) {
            return "https://api.openai.com/v1";
        }
        return fromEnv.trim();
    }

    private static String resolveApiKey() {
        String k = System.getenv("OPENAI_API_KEY");
        if (k == null || k.isBlank()) {
            k = System.getenv("AI_API_KEY");
        }
        if (k == null || k.isBlank()) {
            String prop = System.getProperty("openai.api.key");
            if (prop != null && !prop.isBlank()) {
                k = prop;
            }
        }
        if (k == null || k.isBlank()) {
            k = readApiKeyFromProjectDataFile();
        }
        return k == null ? "" : k.trim();
    }

    /**
     * Reads {@code data/openai-api-key.txt} starting from the JVM working directory and walking up to ancestors,
     * so it still works when the process starts in {@code src} or another subfolder.
     */
    private static String readApiKeyFromProjectDataFile() {
        Path dir = Path.of("").toAbsolutePath().normalize();
        for (int depth = 0; depth < 10 && dir != null; depth++) {
            Path file = dir.resolve("data").resolve("openai-api-key.txt");
            if (Files.isRegularFile(file)) {
                try {
                    for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                        String line = raw == null ? "" : raw.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            return line;
                        }
                    }
                } catch (IOException ignored) {
                    return "";
                }
                return "";
            }
            dir = dir.getParent();
        }
        return "";
    }

    private static String resolveModel() {
        String m = System.getenv("OPENAI_MODEL");
        if (m == null || m.isBlank()) {
            m = System.getenv("AI_MODEL");
        }
        return m == null ? "gpt-4o-mini" : m.trim();
    }

    private static String trimTrailingSlash(String base) {
        String b = base.trim();
        while (b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        return b;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    /**
     * Keeps the most recent user/assistant pairs to avoid oversized requests.
     */
    public static List<ChatTurn> trimHistory(List<ChatTurn> full, int maxPairs) {
        if (full.size() <= maxPairs * 2) {
            return new ArrayList<>(full);
        }
        int from = full.size() - maxPairs * 2;
        return new ArrayList<>(full.subList(from, full.size()));
    }
}
