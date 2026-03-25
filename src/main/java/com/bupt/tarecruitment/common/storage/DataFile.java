package com.bupt.tarecruitment.common.storage;

import java.util.List;

public enum DataFile {
    USERS(
        "users.txt",
        List.of(
            "# Format: userId|passwordHash|role|displayName|status",
            "admin001|demo-admin-password|ADMIN|System Admin|ACTIVE",
            "mo001|demo-mo-password|MO|Demo Module Organiser|ACTIVE",
            "ta001|demo-ta-password|APPLICANT|Demo Applicant|ACTIVE"
        )
    ),
    PROFILES(
        "profiles.txt",
        List.of(
            "# Format: profileId|userId|studentId|fullName|programme|yearOfStudy|educationLevel|skills(;)|availabilitySlots(;)|desiredPositions(;)|cvFileName",
            "profile001|ta001|231225700|Demo Applicant|Software Engineering|3|Undergraduate|Java;Communication|MON-09:00-11:00;WED-14:00-16:00|Teaching Assistant;Invigilation|demo_cv.txt"
        )
    ),
    JOBS(
        "jobs.txt",
        List.of(
            "# Format: jobId|organiserId|title|moduleOrActivity|description|requiredSkills(;)|weeklyHours|scheduleSlots(;)|status",
            "job001|mo001|TA for Software Engineering|EBU6304|Support lab sessions and assignment marking|Java;Teamwork|4|MON-10:00-12:00;THU-14:00-16:00|OPEN"
        )
    ),
    APPLICATIONS(
        "applications.txt",
        List.of(
            "# Format: applicationId|jobId|applicantUserId|status|submittedAt|reviewerNote",
            "application001|job001|ta001|SUBMITTED|2026-03-25T14:00:00|Initial demo application"
        )
    ),
    MESSAGES(
        "messages.txt",
        List.of(
            "# Format: messageId|jobId|senderUserId|receiverUserId|sentAt|content|readStatus",
            "message001|job001|ta001|mo001|2026-03-25T14:10:00|Could you clarify the marking workload?|UNREAD"
        )
    );

    private final String fileName;
    private final List<String> initialLines;

    DataFile(String fileName, List<String> initialLines) {
        this.fileName = fileName;
        this.initialLines = List.copyOf(initialLines);
    }

    public String fileName() {
        return fileName;
    }

    public List<String> initialLines() {
        return initialLines;
    }
}
