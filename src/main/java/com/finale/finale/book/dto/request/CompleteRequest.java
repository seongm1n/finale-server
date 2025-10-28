package com.finale.finale.book.dto.request;

import java.util.List;

public record CompleteRequest(
        List<QuizRequest> quizAnswers,
        List<UnknownWordRequest> unknownWords
) {
}
