package com.bupt.tarecruitment.job;

import java.util.List;
import java.util.Locale;

/**
 * Canonical activity-type values used for job posting and browsing filters.
 */
public final class JobActivityType {
    public static final String LAB_SESSION = "Lab session";
    public static final String TUTORIAL = "Tutorial";
    public static final String ASSIGNMENT_MARKING = "Assignment / marking";
    public static final String PROJECT_DEVELOPMENT = "Project / development";
    public static final String INVIGILATION = "Invigilation";
    public static final String OTHER = "Other";

    private static final List<String> VALUES = List.of(
        LAB_SESSION,
        TUTORIAL,
        ASSIGNMENT_MARKING,
        PROJECT_DEVELOPMENT,
        INVIGILATION,
        OTHER
    );

    private JobActivityType() {
    }

    public static List<String> values() {
        return VALUES;
    }

    public static String normalize(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return OTHER;
        }
        for (String value : VALUES) {
            if (value.equalsIgnoreCase(rawValue.trim())) {
                return value;
            }
        }
        return OTHER;
    }

    public static String infer(String title, String description) {
        String searchBlob = ((title == null ? "" : title) + " " + (description == null ? "" : description))
            .toLowerCase(Locale.ROOT);
        if (searchBlob.contains("lab") || searchBlob.contains("practical")) {
            return LAB_SESSION;
        }
        if (searchBlob.contains("tutorial")) {
            return TUTORIAL;
        }
        if (searchBlob.contains("assignment") || searchBlob.contains("marking") || searchBlob.contains("grading")) {
            return ASSIGNMENT_MARKING;
        }
        if (searchBlob.contains("project") || searchBlob.contains("development") || searchBlob.contains("studio")) {
            return PROJECT_DEVELOPMENT;
        }
        if (searchBlob.contains("invigilation") || searchBlob.contains("exam")) {
            return INVIGILATION;
        }
        return OTHER;
    }
}
