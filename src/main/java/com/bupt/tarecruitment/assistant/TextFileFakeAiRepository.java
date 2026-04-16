package com.bupt.tarecruitment.assistant;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 从本地文本文件读取伪 AI 固定问答库。
 */
public final class TextFileFakeAiRepository {
    private static final String FIELD_SEPARATOR = "\\|";
    private static final String QUESTION_SEPARATOR = ";";

    private static final List<String> INITIAL_LINES = List.of(
        "# Format: question1;question2;question3|answer",
        "what is weakly matched;what does weakly matched mean;什么是 weakly matched|Weakly matched means your profile shows related experience, but not a direct skill-by-skill match yet.",
        "how can i improve my profile;how to improve missing skills;我该怎么提升自己|You can improve your profile by adding stronger evidence for missing skills, updating your CV, and selecting more accurate profile skills.",
        "how does the schedule table work;what is ta workload;课表页面是干什么的|The TA workload page shows only accepted jobs and maps their schedule slots onto the fixed timetable bands.",
        "why can i not apply;why is apply button disabled;为什么我不能申请|If you cannot apply, the most common reasons are schedule conflict, missing profile data, or the job no longer being open.",
        "how are skills matched;how does skill matching work;技能是怎么匹配的|Skill matching is rule-based. The system compares your profile skills with the job's required skills and groups them into matched, weakly matched, and missing.",
        "what should i improve first;what skills should i improve first;我应该先提升什么技能|Start with the top missing skill that is directly required by the job, then strengthen any weakly matched area so it becomes a direct match."
    );

    private final Path answersFilePath;

    public TextFileFakeAiRepository(Path dataDirectory) {
        this.answersFilePath = dataDirectory.resolve("fake_ai_answers.txt");
        ensureFileExists();
    }

    public List<FakeAiAnswer> findAll() {
        try {
            List<FakeAiAnswer> answers = new ArrayList<>();
            for (String line : Files.readAllLines(answersFilePath, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                answers.add(parseLine(line));
            }
            return answers;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read fake AI answers.", exception);
        }
    }

    public Path answersFilePath() {
        return answersFilePath;
    }

    private FakeAiAnswer parseLine(String line) {
        String[] fields = line.split(FIELD_SEPARATOR, 2);
        if (fields.length != 2) {
            throw new IllegalStateException("Invalid fake AI answer record: " + line);
        }

        List<String> questions = List.of(fields[0].split(QUESTION_SEPARATOR)).stream()
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList();

        if (questions.isEmpty() || fields[1].isBlank()) {
            throw new IllegalStateException("Invalid fake AI answer record: " + line);
        }

        return new FakeAiAnswer(questions, fields[1].trim());
    }

    private void ensureFileExists() {
        try {
            Files.createDirectories(answersFilePath.getParent());
            if (Files.notExists(answersFilePath)) {
                Files.write(answersFilePath, INITIAL_LINES, StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to prepare fake AI answer storage.", exception);
        }
    }
}
