package com.bupt.tarecruitment.assistant;

import java.util.Objects;
import java.util.Optional;

/**
 * Uses NVIDIA when configured and keeps the local assistant as a reliable fallback.
 */
public final class HybridAiAssistantService implements AiAssistantService {
    private static final String SYSTEM_PROMPT = """
        You are the BUPT-TA Recruitment System AI assistant.
        Answer as a concise, practical assistant for a teaching assistant recruitment app.
        Use the local system analysis as the trusted project context.
        Do not invent job data, application status, skills, schedule slots, or user profile facts.
        If the local analysis says the information is missing, say what the user should update in the system.
        Use plain text only. Do not use Markdown syntax, including **bold**, headings, tables, or code fences.
        Use simple numbered lines and short labels instead of Markdown bullets.
        After answering the factual analysis, add a short final note with one practical suggestion or gentle encouragement.
        Keep that final note clearly separate from the factual analysis and do not let it change the system-derived conclusion.
        """;

    private final AiAssistantService fallbackService;
    private final Optional<NvidiaAiAssistantClient> nvidiaClient;

    public HybridAiAssistantService(AiAssistantService fallbackService, Optional<NvidiaAiAssistantClient> nvidiaClient) {
        this.fallbackService = Objects.requireNonNull(fallbackService);
        this.nvidiaClient = Objects.requireNonNull(nvidiaClient);
    }

    @Override
    public String answer(String rawQuestion, String applicantUserId, String selectedJobId) {
        String localAnswer = fallbackService.answer(rawQuestion, applicantUserId, selectedJobId);
        if (nvidiaClient.isEmpty() || !nvidiaClient.get().isConfigured()) {
            return localAnswer;
        }

        try {
            return nvidiaClient.get().chat(
                SYSTEM_PROMPT,
                buildUserPrompt(rawQuestion, applicantUserId, selectedJobId, localAnswer)
            );
        } catch (RuntimeException exception) {
            return localAnswer
                + System.lineSeparator()
                + System.lineSeparator()
                + "Small note: Cloud AI did not respond this time, so this answer came from the built-in helper using local system data. Reason: "
                + summarizeError(exception)
                + ".";
        }
    }

    @Override
    public boolean isRealApiEnabled() {
        return nvidiaClient.isPresent() && nvidiaClient.get().isConfigured();
    }

    @Override
    public String providerLabel() {
        return isRealApiEnabled()
            ? "Cloud AI: " + nvidiaClient.get().model()
            : fallbackService.providerLabel();
    }

    private static String buildUserPrompt(
        String rawQuestion,
        String applicantUserId,
        String selectedJobId,
        String localAnswer
    ) {
        return """
            User question:
            %s

            Applicant user id:
            %s

            Selected job id:
            %s

            Local system analysis:
            %s

            Please answer the user directly. Keep it helpful and compact.
            """.formatted(
                nullToBlank(rawQuestion),
                nullToBlank(applicantUserId),
                nullToBlank(selectedJobId),
                nullToBlank(localAnswer)
            );
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private static String summarizeError(RuntimeException exception) {
        Throwable cause = exception.getCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            return cause.getClass().getSimpleName() + " - " + cause.getMessage();
        }
        if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
            return exception.getMessage();
        }
        return exception.getClass().getSimpleName();
    }
}
