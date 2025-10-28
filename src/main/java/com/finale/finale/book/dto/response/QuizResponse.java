package com.finale.finale.book.dto.response;

public record QuizResponse(
        Long id,
        String question,
        Boolean correctAnswer
) {
}
