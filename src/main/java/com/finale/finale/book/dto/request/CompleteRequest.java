package com.finale.finale.book.dto.request;

import java.util.List;

public record CompleteRequest(
        List<QuizAnswer> quizAnswers,
        List<UnknownWord> unknownWords
) {
    public record QuizAnswer(
            Long quizId,
            Boolean userAnswer
    ) {}

    public record UnknownWord(
            String word,
            String wordMeaning,
            String sentence,
            String sentenceMeaning,
            int location,
            int length
    ) {}
}
