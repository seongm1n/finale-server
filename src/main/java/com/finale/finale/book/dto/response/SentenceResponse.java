package com.finale.finale.book.dto.response;

public record SentenceResponse(
        int paragraphNumber,
        int sentenceOrder,
        String englishText,
        String koreanText
) {
}
