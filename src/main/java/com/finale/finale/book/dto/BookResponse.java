package com.finale.finale.book.dto;

import java.time.LocalDateTime;
import java.util.List;

public record BookResponse(
        Long bookId,
        String title,
        String category,
        int abilityScore,
        List<SentenceResponse> sentences,
        LocalDateTime createdAt
) {
}
