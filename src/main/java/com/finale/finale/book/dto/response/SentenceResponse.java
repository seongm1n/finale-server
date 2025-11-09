package com.finale.finale.book.dto.response;

public record SentenceResponse(
        Long sentenceId,
        int paragraphNumber,
        int sentenceOrder,
        String englishText,
        String koreanText
) {
}
