package com.finale.finale.book.service;

import com.finale.finale.book.domain.Book;
import com.finale.finale.book.domain.UnknownPhrase;
import com.finale.finale.book.domain.UnknownWord;
import com.finale.finale.book.dto.response.CompletedBooksResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CompletedBooksAssembler {

    public List<CompletedBooksResponse.CompletedBook> toCompletedBooks(
            List<Book> books,
            Map<Long, List<UnknownWord>> unknownWordsByBook,
            Map<Long, List<UnknownPhrase>> unknownPhrasesByBook
    ) {
        return books.stream()
                .map(book -> toCompletedBook(
                        book,
                        unknownWordsByBook.getOrDefault(book.getId(), List.of()),
                        unknownPhrasesByBook.getOrDefault(book.getId(), List.of())
                ))
                .toList();
    }

    private CompletedBooksResponse.CompletedBook toCompletedBook(
            Book book,
            List<UnknownWord> unknownWords,
            List<UnknownPhrase> unknownPhrases
    ) {
        List<CompletedBooksResponse.UnknownWordResponse> unknownWordResponses = unknownWords.stream()
                .map(word -> CompletedBooksResponse.UnknownWordResponse.builder()
                        .id(word.getId())
                        .word(word.getWord())
                        .wordMeaning(word.getWordMeaning())
                        .sentenceId(word.getSentenceId())
                        .sentence(word.getSentence())
                        .sentenceMeaning(word.getSentenceMeaning())
                        .location(word.getLocation())
                        .length(word.getLength())
                        .nextReviewDate(word.getNextReviewDate())
                        .createdAt(word.getCreatedAt())
                        .build())
                .toList();

        List<CompletedBooksResponse.UnknownPhraseResponse> unknownPhraseResponses = unknownPhrases.stream()
                .map(phrase ->
                        CompletedBooksResponse.UnknownPhraseResponse.builder()
                                .id(phrase.getId())
                                .phrase(phrase.getPhrase())
                                .phraseMeaning(phrase.getPhraseMeaning())
                                .sentenceId(phrase.getSentenceId())
                                .sentence(phrase.getSentence())
                                .sentenceMeaning(phrase.getSentenceMeaning())
                                .words(phrase.getWords().stream()
                                        .map(w -> new CompletedBooksResponse.PhraseWordResponse(
                                                w.getWord(),
                                                w.getLocation(),
                                                w.getLength()
                                        ))
                                        .toList())
                                .nextReviewDate(phrase.getNextReviewDate())
                                .createdAt(phrase.getCreatedAt())
                                .build())
                .toList();

        return CompletedBooksResponse.CompletedBook.builder()
                .id(book.getId())
                .title(book.getTitle())
                .category(book.getCategory().getValue())
                .abilityScore(book.getAbilityScore())
                .isBookmarked(book.getIsBookmarked())
                .createdAt(book.getCreatedAt())
                .unknownWords(unknownWordResponses)
                .unknownPhrases(unknownPhraseResponses)
                .build();
    }
}

