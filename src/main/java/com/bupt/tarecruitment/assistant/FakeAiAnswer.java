package com.bupt.tarecruitment.assistant;

import java.util.List;
import java.util.Objects;

/**
 * 表示一条本地固定问答，支持多个等价问法映射到同一个答案。
 */
public record FakeAiAnswer(
    List<String> questions,
    String answer
) {
    public FakeAiAnswer {
        Objects.requireNonNull(questions);
        Objects.requireNonNull(answer);
        questions = List.copyOf(questions);
    }
}
