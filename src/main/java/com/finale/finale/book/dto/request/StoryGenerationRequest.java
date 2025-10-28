package com.finale.finale.book.dto.request;

import java.util.List;

public record StoryGenerationRequest(
        int abilityScore,
        String category,
        List<String> recommendedWords
) {
}
