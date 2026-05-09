package com.bupt.tarecruitment.assistant;

/**
 * Common assistant contract used by the UI.
 */
public interface AiAssistantService {
    String answer(String rawQuestion, String applicantUserId, String selectedJobId);

    default boolean isRealApiEnabled() {
        return false;
    }

    default String providerLabel() {
        return "Local assistant";
    }
}
