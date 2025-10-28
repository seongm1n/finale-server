package com.finale.finale.book.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record StoryGenerationResponse(
        Long bookId,
        String title,
        String category,
        int abilityScore,
        int totalWordCount,
        List<SentenceResponse> sentences,
        List<QuizResponse> quizzes,
        LocalDateTime createdAt
) {
}
