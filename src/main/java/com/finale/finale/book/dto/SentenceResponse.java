package com.finale.finale.book.dto;

public record SentenceResponse(
        int paragraphNumber,
        int sentenceOrder,
        String englishText,
        String koreanText
) {
}
