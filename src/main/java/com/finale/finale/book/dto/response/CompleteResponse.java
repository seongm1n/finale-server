package com.finale.finale.book.dto.response;

import java.time.LocalDateTime;

public record CompleteResponse(
        Long bookId,
        String title,
        LocalDateTime completedAt,
        QuizResult quizResult,
        StatsChange statsChange
) {
    public record QuizResult(
            Integer totalQuestions,
            Integer correctAnswers
    ) {}

    public record StatsChange(
            StatChange abilityScore,
            StatChange bookReadCount,
            StatChange totalSentencesRead,
            StatChange unknownWordsCount
    ) {}

    public record StatChange(
            Integer before,
            Integer after,
            Integer change
    ) {}
}
