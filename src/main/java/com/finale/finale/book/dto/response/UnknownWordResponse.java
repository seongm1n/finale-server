package com.finale.finale.book.dto.response;

public record UnknownWordResponse (
        String word,
        String wordMeaning,
        String sentence,
        String sentenceMeaning
) {
}
