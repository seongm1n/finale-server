package com.finale.finale.book.dto.request;

public record QuizRequest(
        Long quizId,
        Boolean userAnswer
) {
}
