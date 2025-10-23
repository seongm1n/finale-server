package com.finale.finale.book.dto;

import java.util.List;

public record StoryGenerationRequest(
        int abilityScore,
        String category,
        List<String> recommendedWords
) {
}
