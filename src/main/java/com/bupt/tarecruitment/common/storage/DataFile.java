package com.bupt.tarecruitment.common.storage;

import java.util.List;

/**
 * Central registry for txt-backed storage files and their demo seed data.
 */
public enum DataFile {
    USERS(
        "users.txt",
        List.of(
            "# Format: userId|passwordHash|role|displayName|status",
            "admin001|123456|ADMIN|System Admin|ACTIVE",
            "mo001|123456|MO|Dr Alice Chen|ACTIVE",
            "mo002|123456|MO|Dr Brian Zhao|ACTIVE",
            "mo003|123456|MO|Dr Naomi Wang|ACTIVE",
            "mo004|123456|MO|Dr Victor Liu|ACTIVE",
            "mo005|123456|MO|Dr Helen Xu|ACTIVE",
            "mo006|123456|MO|Dr Rachel Sun|ACTIVE",
            "ta001|123456|APPLICANT|Zhang Yiliang|ACTIVE",
            "ta002|123456|APPLICANT|Chen Yuhan|ACTIVE",
            "ta003|123456|APPLICANT|Liu Qian|ACTIVE",
            "ta004|123456|APPLICANT|Wang Rui|ACTIVE",
            "ta005|123456|APPLICANT|Zhao Min|ACTIVE",
            "ta006|123456|APPLICANT|Sun Ke|ACTIVE",
            "ta007|123456|APPLICANT|Huang Jia|ACTIVE",
            "ta008|123456|APPLICANT|Lin Siyu|ACTIVE",
            "ta009|123456|APPLICANT|Zhou Han|ACTIVE",
            "ta010|123456|APPLICANT|Jiang Yuan|ACTIVE",
            "ta011|123456|APPLICANT|Guo Yichen|ACTIVE",
            "ta012|123456|APPLICANT|Ma Ning|ACTIVE",
            "ta013|123456|APPLICANT|Yang Fan|ACTIVE",
            "ta900|123456|APPLICANT|Li Minghao|ACTIVE"
        )
    ),
    PROFILES(
        "profiles.txt",
        List.of(
            "# Format: profileId|userId|studentId|fullName|programme|yearOfStudy|educationLevel|skills(;)|availabilitySlots(;)|desiredPositions(;)|avatarPath",
            "profile001|ta001|231225700|Zhang Yiliang|Software Engineering|3|Graduated|Java;Communication|FRI-08:00-09:35;MON-08:00-09:35;THU-08:00-09:35;TUE-08:00-09:35;WED-08:00-09:35;FRI-09:50-11:25;MON-09:50-11:25;THU-09:50-11:25;TUE-09:50-11:25;WED-09:50-11:25;FRI-11:40-12:25;MON-11:40-12:25;THU-11:40-12:25;TUE-11:40-12:25;WED-11:40-12:25;FRI-13:00-14:35;MON-13:00-14:35;THU-13:00-14:35;TUE-13:00-14:35;WED-13:00-14:35;FRI-14:45-16:25;MON-14:45-16:25;THU-14:45-16:25;TUE-14:45-16:25;WED-14:45-16:25;FRI-16:35-18:10;MON-16:35-18:10;THU-16:35-18:10;TUE-16:35-18:10;WED-16:35-18:10;FRI-18:30-19:15;MON-18:30-19:15;THU-18:30-19:15;TUE-18:30-19:15;WED-18:30-19:15|Teaching Assistant;Invigilation|",
            "profile002|ta002|231225701|Chen Yuhan|Computer Science|1|Graduated|Python;Communication|TUE-14:45-16:25;FRI-09:50-11:25|Teaching Assistant;Lab Support|",
            "profile004|ta003|231225702|Liu Qian|Computer Science|2|Not Graduated|Java;Data Structures;Algorithms;Communication;Git|WED-09:50-11:25;FRI-14:45-16:25;MON-13:00-14:35;THU-18:30-19:15|Teaching Assistant;Lab Support;Office Hours|",
            "profile005|ta004|231225703|Wang Rui|Software Engineering|3|Not Graduated|SQL;Database Design;Java;Docker;Communication|MON-13:00-14:35;MON-14:45-16:25;WED-13:00-14:35;THU-09:50-11:25;FRI-11:40-12:25|Database Assistant;Project Mentor;Teaching Assistant|",
            "profile006|ta005|231225704|Zhao Min|Computer Science|4|Graduated|C;Linux;Networking;Wireshark;Problem Solving|TUE-13:00-14:35;TUE-14:45-16:25;FRI-09:50-11:25;WED-08:00-09:35;WED-09:50-11:25|Lab Support;Systems Tutor|",
            "profile007|ta006|231225705|Sun Ke|Data Science|2|Not Graduated|Python;Statistics;Mathematics;Data Analysis;Communication|THU-09:50-11:25;WED-16:35-18:10;FRI-14:45-16:25;TUE-11:40-12:25;MON-18:30-19:15|Statistics Lab Support;Teaching Assistant|",
            "profile008|ta007|231225706|Huang Jia|Computer Science|3|Not Graduated|HTML;CSS;JavaScript;UI Design;Android|MON-08:00-09:35;WED-14:45-16:25;FRI-14:45-16:25;THU-13:00-14:35;TUE-18:30-19:15|Frontend Lab Support;Mobile App Mentor|",
            "profile009|ta008|231225707|Lin Siyu|Software Engineering|2|Not Graduated|Testing;JUnit;Java;Detail Orientation;Teamwork|TUE-08:00-09:35;MON-09:50-11:25;WED-13:00-14:35;FRI-16:35-18:10;THU-11:40-12:25|Software Testing Assistant;Code Review Support|",
            "profile011|ta009|231225708|Zhou Han|Information Security|3|Not Graduated|Security;Python;Linux;Analytical Thinking;Cryptography|THU-14:45-16:25;TUE-11:40-12:25;MON-18:30-19:15;WED-16:35-18:10;FRI-08:00-09:35|Security Tutorial Assistant;Linux Lab Support|",
            "profile003|ta010|2023213389|Jiang Yuan|Computer Science|4|Not Graduated|Python;Java|MON-09:50-11:25;WED-14:45-16:25|Python Tutor;Teaching Assistant|",
            "profile012|ta011|231225709|Guo Yichen|Internet of Things|2|Not Graduated|IoT;RFID;Lab Support;Communication;Equipment Setup|MON-11:40-12:25;FRI-16:35-18:10;THU-08:00-09:35;TUE-18:30-19:15;WED-14:45-16:25|IoT Lab Assistant;Equipment Support|",
            "profile013|ta012|231225710|Ma Ning|Data Science|4|Graduated|Big Data;Spark;Cloud Computing;AWS;Python|THU-09:50-11:25;FRI-14:45-16:25;WED-13:00-14:35;MON-14:45-16:25;TUE-14:45-16:25|Cloud Data Assistant;Analytics Mentor|",
            "profile014|ta013|231225711|Yang Fan|Computer Science|1|Not Graduated|Programming;Presentation;Communication;Patience;Teaching|MON-09:50-11:25;TUE-09:50-11:25;TUE-11:40-12:25;WED-16:35-18:10;THU-18:30-19:15|Programming Tutor;First-year Support|",
            "profile900|ta900|231229900|Li Minghao|Software Engineering|3|Not Graduated|Java;Teamwork;Communication;JUnit|MON-08:00-18:00;TUE-08:00-18:00;WED-08:00-18:00;THU-08:00-18:00;FRI-08:00-18:00|Teaching Assistant;Lab Support|"
        )
    ),
    CVS(
        "cvs.txt",
        List.of(
            "# Format: cvId|ownerUserId|title|fileName|createdAt|updatedAt",
            "cv001|ta001|Software Engineering Focus CV|cvs/ta001/cv001.txt|2026-03-25T13:55|2026-03-25T13:55",
            "cv002|ta001|Computer Science Focus CV|cvs/ta001/cv002.txt|2026-03-25T15:20|2026-03-25T15:20",
            "cv004|ta002|Python Tutorial Support CV|cvs/ta002/cv004.txt|2026-03-26T09:00|2026-03-26T09:00",
            "cv005|ta001|Software Engineering Focus CV|cvs/ta001/cv005.txt|2026-04-07T00:07:01.982942900|2026-04-07T00:07:01.982942900",
            "cv006|ta001|Teaching Support CV|cvs/ta001/cv006.txt|2026-04-07T00:07:22.596890400|2026-04-07T00:07:22.596890400",
            "cv007|ta010|Information Security CV|cvs/ta010/cv007.txt|2026-04-10T20:01:35.026517100|2026-04-10T20:01:35.026517100",
            "cv008|ta010|Python Lab Support CV|cvs/ta010/cv008.txt|2026-04-10T20:01:44.217426600|2026-04-10T20:01:44.217426600",
            "cv009|ta010|Software Testing CV|cvs/ta010/cv009.txt|2026-04-10T20:03:07.812357200|2026-04-10T20:03:07.812357200",
            "cv010|ta003|Data Structures Lab CV|cvs/ta003/cv010.txt|2026-04-17T09:00|2026-04-17T09:00",
            "cv011|ta004|Database Systems Support CV|cvs/ta004/cv011.txt|2026-04-17T09:30|2026-04-17T09:30",
            "cv012|ta005|Systems and Networks CV|cvs/ta005/cv012.txt|2026-04-18T10:30|2026-04-18T10:30",
            "cv013|ta006|Statistics and Python Lab CV|cvs/ta006/cv013.txt|2026-04-18T14:00|2026-04-18T14:00",
            "cv014|ta007|Frontend and Mobile CV|cvs/ta007/cv014.txt|2026-04-19T09:15|2026-04-19T09:15",
            "cv015|ta008|Software Testing CV|cvs/ta008/cv015.txt|2026-04-19T10:40|2026-04-19T10:40",
            "cv016|ta009|Security Tutorial CV|cvs/ta009/cv016.txt|2026-04-20T11:20|2026-04-20T11:20",
            "cv017|ta011|IoT Lab Support CV|cvs/ta011/cv017.txt|2026-04-20T13:10|2026-04-20T13:10",
            "cv018|ta012|Cloud Data Processing CV|cvs/ta012/cv018.txt|2026-04-21T15:05|2026-04-21T15:05",
            "cv019|ta013|Programming Tutor CV|cvs/ta013/cv019.txt|2026-04-21T16:25|2026-04-21T16:25",
            "cv900|ta900|Software Engineering Teaching CV|cvs/ta900/cv900.txt|2026-04-16T10:00|2026-04-16T10:00"
        )
    ),
    JOBS(
        "jobs.txt",
        List.of(
            "# Format: jobId|organiserId|title|moduleOrActivity|activityType|description|requiredSkills(;)|weeklyHours|scheduleSlots(;)|status",
            "job001|mo001|TA for Software Engineering|EBU6304|Lab session|Support lab sessions and assignment marking|Java;Teamwork|4|MON-10:00-12:00;THU-14:00-16:00|OPEN",
            "job002|mo001|TA for Computer Science|COMP101|Tutorial|Support tutorials and coursework feedback|Programming;Communication|3|TUE-09:00-12:00|OPEN",
            "job003|mo001|TA for Data Structures|COMP202|Lab session|Support weekly lab sessions and office hours|Java;Data Structures;Communication|4|WED-10:00-12:00;FRI-14:00-16:00|OPEN",
            "job004|mo002|TA for Database Systems|COMP303|Lab session|Assist SQL lab practices and grading|SQL;Database Design;Communication|3|MON-13:00-16:00|OPEN",
            "job005|mo002|TA for Operating Systems|COMP305|Lab session|Guide kernel concept tutorials and labs|C;Linux;Problem Solving|5|TUE-13:00-16:00;FRI-10:00-12:00|OPEN",
            "job006|mo003|TA for Linear Algebra|MATH201|Assignment / marking|Support exercise classes and quiz marking|Mathematics;Patience|2|WED-16:00-18:00|OPEN",
            "job007|mo003|TA for Probability and Statistics|STAT210|Lab session|Help with practical sessions and assignments|Statistics;Python;Communication|2|THU-10:00-12:00|OPEN",
            "job008|mo004|TA for Web Development|COMP220|Lab session|Support frontend labs and project reviews|HTML;CSS;JavaScript|4|MON-08:00-10:00;WED-14:00-16:00|OPEN",
            "job009|mo004|TA for Software Testing|COMP340|Assignment / marking|Assist test design workshops and grading|Testing;JUnit;Detail Orientation|2|TUE-08:00-10:00|OPEN",
            "job010|mo005|TA for Computer Networks|COMP260|Lab session|Assist network configuration labs|Networking;Wireshark;Teamwork|5|WED-08:00-11:00;FRI-08:00-10:00|OPEN",
            "job011|mo005|TA for Information Security|COMP360|Tutorial|Support cryptography tutorials and lab guidance|Security;Python;Analytical Thinking|2|THU-14:00-16:00|OPEN",
            "job012|mo006|TA for Mobile App Development|COMP330|Project / development|Mentor Android studio practices and demos|Java;Android;UI Design|2|FRI-14:00-16:00|OPEN",
            "job013|mo001|Introductory Programming Support|COMP100|Tutorial|Help first-year students practise basic programming exercises and debug weekly lab tasks.|Programming;Communication|1.5|MON-09:50-11:25|OPEN",
            "job014|mo001|IoT Lab Assistant|EBU6011|Lab session|Support RFID and sensor-network lab activities, including equipment setup and student troubleshooting.|Java;IoT;Communication|4|MON-11:40-12:25|OPEN",
            "job900|mo001|Advanced Java Lab Assistant|COMP900|Lab session|Support advanced Java labs and pair-programming exercises for high-fit applicant testing.|Java;Communication;Teamwork;JUnit|2|MON-09:50-11:25|OPEN",
            "job901|mo001|Programming Fundamentals Tutor|COMP901|Tutorial|Run small-group programming tutorials and explain testing concepts to first-year students.|Programming;Presentation;Testing|2|TUE-09:50-11:25|OPEN",
            "job902|mo002|Database Integration Assistant|COMP902|Project / development|Support database integration clinics covering SQL, Java back-end code, and deployment basics.|Java;Programming;SQL;Docker|3|WED-13:00-14:35|OPEN",
            "job903|mo005|Cloud Data Processing Assistant|COMP903|Project / development|Help students understand cloud data-processing pipelines and large-scale analytics workflows.|Big Data;Spark;Cloud Computing;AWS|3|THU-09:50-11:25|OPEN",
            "job904|mo006|Python Statistics Lab Support|COMP904|Lab session|Support applied statistics lab sessions using Python notebooks and data-analysis exercises.|Python;Big Data;Statistics;Communication|2|FRI-14:45-16:25|OPEN",
            "job905|mo001|RFID Lab Assistant|EBU6011|Lab session|Assist with RFID reader setup, tag testing, and lab safety checks during the IoT practical session.|RFID;IoT;Lab Support|2|FRI-16:35-18:10|OPEN",
            "job906|mo001|Software Engineering Mentor|EBU6304|Project / development|Mentor student teams on requirements analysis, version control, and Java project structure.|Java;Software Engineering;Teamwork|1.5|WED-08:00-09:35|OPEN",
            "job907|mo001|Machine Learning Tutorial Assistant|COMP5566|Tutorial|Support machine-learning tutorials on Linux-based tooling, model evaluation, and Python workflows.|Linux;Software Engineering;Machine Learning;Python|1.5|TUE-11:40-12:25|OPEN",
            "job908|mo001|EBU1111|math|Other|1||9|MON-08:00-09:35;MON-09:50-11:25;MON-11:40-12:25;MON-13:00-14:35;MON-14:45-16:25;MON-16:35-18:10|OPEN"
        )
    ),
    APPLICATIONS(
        "applications.txt",
        List.of(
            "# Format: applicationId|jobId|applicantUserId|cvId|status|submittedAt|reviewerNote",
            "application001|job001|ta001|cv001|WITHDRAWN|2026-03-25T14:00|b64:QXBwbGljYXRpb24gd2l0aGRyYXduIGFmdGVyIGNob29zaW5nIGEgYmV0dGVyIG1hdGNoZWQgU29mdHdhcmUgRW5naW5lZXJpbmcgcm9sZS4=",
            "application002|job002|ta001|cv002|SUBMITTED|2026-03-25T15:30|b64:QXBwbGljYW50IGlzIHN1aXRhYmxlIGZvciB0dXRvcmlhbCBzdXBwb3J0OyBwZW5kaW5nIGZpbmFsIHRpbWV0YWJsZSBjaGVjay4=",
            "application003|job012|ta010|cv007|SUBMITTED|2026-04-10T20:26:03.429235600|b64:",
            "application004|job013|ta001|cv005|WITHDRAWN|2026-04-11T16:39:53.556584700|b64:",
            "application005|job001|ta001|cv001|ACCEPTED|2026-04-14T02:19:35.002826900|b64:QWNjZXB0ZWQgYWZ0ZXIgQ1YgcmV2aWV3IGFuZCB0aW1ldGFibGUgY2hlY2su",
            "application900|job900|ta900|cv900|ACCEPTED|2026-04-16T10:10|b64:",
            "application901|job901|ta900|cv900|ACCEPTED|2026-04-16T10:15|b64:",
            "application902|job902|ta900|cv900|ACCEPTED|2026-04-16T10:20|b64:",
            "application903|job904|ta900|cv900|ACCEPTED|2026-04-16T10:25|b64:",
            "application904|job905|ta001|cv001|ACCEPTED|2026-04-16T15:05:29.134391600|b64:QWNjZXB0ZWQgYWZ0ZXIgQ1YgcmV2aWV3IGFuZCB0aW1ldGFibGUgY2hlY2su",
            "application905|job906|ta001|cv001|ACCEPTED|2026-04-16T15:23:13.073556700|b64:QWNjZXB0ZWQgYWZ0ZXIgQ1YgcmV2aWV3IGFuZCB0aW1ldGFibGUgY2hlY2su",
            "application906|job906|ta003|cv010|ACCEPTED|2026-04-17T09:10|b64:",
            "application907|job003|ta003|cv010|SUBMITTED|2026-04-17T09:20|b64:",
            "application908|job902|ta004|cv011|ACCEPTED|2026-04-17T10:05|b64:",
            "application909|job004|ta004|cv011|SHORTLISTED|2026-04-17T10:15|b64:",
            "application910|job005|ta005|cv012|ACCEPTED|2026-04-18T11:00|b64:",
            "application911|job010|ta005|cv012|SHORTLISTED|2026-04-18T11:20|b64:",
            "application912|job904|ta006|cv013|ACCEPTED|2026-04-18T14:25|b64:",
            "application913|job007|ta006|cv013|SUBMITTED|2026-04-18T14:40|b64:",
            "application914|job008|ta007|cv014|ACCEPTED|2026-04-19T09:35|b64:",
            "application915|job012|ta007|cv014|SHORTLISTED|2026-04-19T09:50|b64:",
            "application916|job900|ta008|cv015|ACCEPTED|2026-04-19T11:00|b64:",
            "application917|job009|ta008|cv015|SUBMITTED|2026-04-19T11:15|b64:",
            "application918|job907|ta009|cv016|ACCEPTED|2026-04-20T11:45|b64:",
            "application919|job011|ta009|cv016|SUBMITTED|2026-04-20T12:00|b64:",
            "application920|job905|ta011|cv017|ACCEPTED|2026-04-20T13:40|b64:",
            "application921|job014|ta011|cv017|SHORTLISTED|2026-04-20T13:55|b64:",
            "application922|job903|ta012|cv018|ACCEPTED|2026-04-21T15:25|b64:",
            "application923|job904|ta012|cv018|SUBMITTED|2026-04-21T15:40|b64:",
            "application924|job901|ta013|cv019|ACCEPTED|2026-04-21T16:45|b64:",
            "application925|job013|ta013|cv019|SUBMITTED|2026-04-21T17:00|b64:"
        )
    ),
    MESSAGES(
        "messages.txt",
        List.of(
            "# Format: messageId|jobId|senderUserId|receiverUserId|sentAt|content|readStatus",
            "message001|job001|ta001|mo001|2026-03-25T14:10|Could you clarify the marking workload?|READ",
            "message002|job001|ta001|mo001|2026-03-31T23:58:21.816837900|Could you confirm whether the marking workload is weekly or biweekly?|READ",
            "message003|job002|ta001|mo001|2026-04-01T00:01:03.590383100|Could you share more details about the tutorial format?|READ",
            "message004|job002|mo001|ta001|2026-04-01T18:01:07.623818500|The tutorial is discussion-based, with a short weekly problem-solving section.|READ",
            "message005|job002|ta001|mo001|2026-04-04T20:22:17.085626500|Thanks, I will update my availability before applying.|READ",
            "message006|job003|ta001|mo001|2026-04-04T20:23:54.027866600|Could you tell me whether this role includes office-hour support?|READ",
            "message007|job013|ta001|mo001|2026-04-07T00:09:45.910818700|I am interested in this programming support role and can cover the listed time slot.|READ",
            "message008|job013|ta010|mo001|2026-04-10T19:49:34.272119|Hello, I would like to ask whether Python experience is required.|READ",
            "message009|job013|mo001|ta010|2026-04-10T20:27:37.787622100|Python experience is helpful, but clear communication with first-year students is more important.|READ",
            "message010|job012|ta010|mo006|2026-04-10T20:35:02.605996800|I am interested in the mobile app development role and can attend the Friday session.|READ"
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
