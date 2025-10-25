package com.finale.finale.book.dto;

public record QuizResponse(
        Long id,
        String question,
        Boolean correctAnswer
) {
}
