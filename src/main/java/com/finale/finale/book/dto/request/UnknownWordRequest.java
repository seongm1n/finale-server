package com.finale.finale.book.dto.request;

public record UnknownWordRequest(
        String word,
        String wordMeaning,
        String sentence,
        String sentenceMeaning,
        int location,
        int length
) {
}
