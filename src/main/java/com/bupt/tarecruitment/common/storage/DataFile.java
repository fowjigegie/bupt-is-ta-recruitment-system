package com.bupt.tarecruitment.common.storage;

import java.util.List;

public enum DataFile {
    USERS(
        "users.txt",
        List.of(
            "# Format: userId|passwordHash|role|displayName|status",
            "admin001|demo-admin-password|ADMIN|System Admin|ACTIVE",
            "mo001|demo-mo-password|MO|Demo Module Organiser|ACTIVE",
            "mo002|demo-mo2-password|MO|Database Module Organiser|ACTIVE",
            "mo003|demo-mo3-password|MO|Math Module Organiser|ACTIVE",
            "mo004|demo-mo4-password|MO|Web Module Organiser|ACTIVE",
            "mo005|demo-mo5-password|MO|Network Module Organiser|ACTIVE",
            "mo006|demo-mo6-password|MO|Mobile Module Organiser|ACTIVE",
            "ta001|demo-ta-password|APPLICANT|Demo Applicant|ACTIVE",
            "ta002|demo-ta2-password|APPLICANT|Apply Demo Applicant|ACTIVE"
        )
    ),
    PROFILES(
        "profiles.txt",
        List.of(
            "# Format: profileId|userId|studentId|fullName|programme|yearOfStudy|educationLevel|skills(;)|availabilitySlots(;)|desiredPositions(;)",
            "profile001|ta001|231225700|Demo Applicant|Software Engineering|3|Not Graduated|Java;Communication|MON-09:00-11:00;WED-14:00-16:00|Teaching Assistant;Invigilation",
            "profile002|ta002|231225701|Apply Demo Applicant|Computer Science|2|Not Graduated|Python;Communication|TUE-14:00-16:00;FRI-10:00-12:00|Teaching Assistant;Lab Support"
        )
    ),
    CVS(
        "cvs.txt",
        List.of(
            "# Format: cvId|ownerUserId|title|fileName|createdAt|updatedAt",
            "cv001|ta001|Software Engineering Focus CV|cvs/ta001/cv001.txt|2026-03-25T13:55:00|2026-03-25T13:55:00",
            "cv002|ta001|Computer Science Focus CV|cvs/ta001/cv002.txt|2026-03-25T15:20:00|2026-03-25T15:20:00",
            "cv004|ta002|General TA CV|cvs/ta002/cv004.txt|2026-03-26T09:00:00|2026-03-26T09:00:00"
        )
    ),
    JOBS(
        "jobs.txt",
        List.of(
            "# Format: jobId|organiserId|title|moduleOrActivity|description|requiredSkills(;)|weeklyHours|scheduleSlots(;)|status",
            "job001|mo001|TA for Software Engineering|EBU6304|Support lab sessions and assignment marking|Java;Teamwork|4|MON-10:00-12:00;THU-14:00-16:00|OPEN",
            "job002|mo001|TA for Computer Science|COMP101|Support tutorials and coursework feedback|Programming;Communication|3|TUE-09:00-12:00|OPEN",
            "job003|mo001|TA for Data Structures|COMP202|Support weekly lab sessions and office hours|Java;Data Structures;Communication|4|WED-10:00-12:00;FRI-14:00-16:00|OPEN",
            "job004|mo002|TA for Database Systems|COMP303|Assist SQL lab practices and grading|SQL;Database Design;Communication|3|MON-13:00-16:00|OPEN",
            "job005|mo002|TA for Operating Systems|COMP305|Guide kernel concept tutorials and labs|C;Linux;Problem Solving|5|TUE-13:00-16:00;FRI-10:00-12:00|OPEN",
            "job006|mo003|TA for Linear Algebra|MATH201|Support exercise classes and quiz marking|Mathematics;Patience|2|WED-16:00-18:00|OPEN",
            "job007|mo003|TA for Probability and Statistics|STAT210|Help with practical sessions and assignments|Statistics;Python;Communication|2|THU-10:00-12:00|OPEN",
            "job008|mo004|TA for Web Development|COMP220|Support frontend labs and project reviews|HTML;CSS;JavaScript|4|MON-08:00-10:00;WED-14:00-16:00|OPEN",
            "job009|mo004|TA for Software Testing|COMP340|Assist test design workshops and grading|Testing;JUnit;Detail Orientation|2|TUE-08:00-10:00|OPEN",
            "job010|mo005|TA for Computer Networks|COMP260|Assist network configuration labs|Networking;Wireshark;Teamwork|5|WED-08:00-11:00;FRI-08:00-10:00|OPEN",
            "job011|mo005|TA for Information Security|COMP360|Support cryptography tutorials and lab guidance|Security;Python;Analytical Thinking|2|THU-14:00-16:00|OPEN",
            "job012|mo006|TA for Mobile App Development|COMP330|Mentor Android studio practices and demos|Java;Android;UI Design|2|FRI-14:00-16:00|OPEN"
        )
    ),
    APPLICATIONS(
        "applications.txt",
        List.of(
            "# Format: applicationId|jobId|applicantUserId|cvId|status|submittedAt|reviewerNote",
            "application001|job001|ta001|cv001|SUBMITTED|2026-03-25T14:00:00|Initial demo application",
            "application002|job002|ta001|cv002|SUBMITTED|2026-03-25T15:30:00|Second demo application for a different role"
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
