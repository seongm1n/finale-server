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
        List<UnknownWordResponse> unknownWordList,
        List<UnknownPhraseResponse> unknownPhraseList,
        LocalDateTime createdAt
) {
    public record SentenceResponse(
            Long sentenceId,
            int paragraphNumber,
            int sentenceOrder,
            String englishText,
            String koreanText,
            List<WordResponse> words,
            List<PhraseResponse> phrases
    ) {}

    public record QuizResponse(
            Long id,
            String question,
            Boolean correctAnswer
        ) {}

    public record UnknownWordResponse(
            String word,
            String wordMeaning,
            String sentence,
            String sentenceMeaning,
            Integer reviewCount
    ) {}

    public record UnknownPhraseResponse(
            String phrase,
            String phraseMeaning,
            String sentence,
            String sentenceMeaning,
            List<PhraseWordResponse> words,
            Integer reviewCount
    ) {}

    public record PhraseWordResponse(
            String word,
            Integer location,
            Integer length
    ) {}


    public record WordResponse(
            String word,
            String meaning,
            Integer location,
            Integer length
    ) {}

    public record PhraseResponse(
            String meaning,
            List<ExpressionResponse> expression
    ) {}

    public record ExpressionResponse(
            String word,
            Integer location,
            Integer length
    ) {}
}
