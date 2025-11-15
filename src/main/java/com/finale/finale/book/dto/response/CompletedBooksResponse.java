package com.finale.finale.book.dto.response;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CompletedBooksResponse(
        List<CompletedBook> content,
        int totalElements,
        int totalPages,
        int currentPage,
        int size,
        boolean hasNext,
        boolean hasPrevious
) {
    @Builder
    public record CompletedBook(
            Long id,
            String title,
            String category,
            Integer abilityScore,
            Boolean isBookmarked,
            LocalDateTime createdAt,
            List<UnknownWordResponse> unknownWords
    ){}

    @Builder
    public record UnknownWordResponse(
            Long id,
            String word,
            String wordMeaning,
            Long sentenceId,
            String sentence,
            String sentenceMeaning,
            Integer location,
            Integer length,
            LocalDate nextReviewDate,
            LocalDateTime createdAt
    ){}
}
