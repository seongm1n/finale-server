package com.finale.finale.book.dto;

import java.util.List;

public record BookRequest(
        int abilityScore,
        String category,
        List<String> recommendedWords
) {
}
