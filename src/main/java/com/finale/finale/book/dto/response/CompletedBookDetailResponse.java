package com.finale.finale.book.dto.response;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CompletedBookDetailResponse(
        Long bookId,
        String title,
        String category,
        Integer abilityScore,
        LocalDateTime completedAt,
        List<SentenceResponse> sentences,
        List<QuizResponse> quizzes,
        List<UnknownWordResponse> unknownWords
) {

    @Builder
    public record SentenceResponse(
            Long sentenceId,
            int paragraphNumber,
            int sentenceOrder,
            String englishText,
            String koreanText
    ) {}

    @Builder
    public record QuizResponse(
            Long id,
            String question,
            Boolean correctAnswer,
            Boolean userAnswer,
            Boolean isCorrect
    ) {}

    @Builder
    public record UnknownWordResponse(
            Long id,
            String word,
            String wordMeaning,
            Long sentenceId,
            String sentence,
            String sentenceMeaning,
            int location,
            int length,
            LocalDate nextReviewDate,
            LocalDateTime createdAt
    ) {}
}
