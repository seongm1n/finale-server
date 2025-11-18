package com.finale.finale.book.service;

import com.finale.finale.auth.domain.User;
import com.finale.finale.auth.repository.UserRepository;
import com.finale.finale.book.domain.*;
import com.finale.finale.book.dto.response.BookmarkResponse;
import com.finale.finale.book.dto.response.CompletedBookDetailResponse;
import com.finale.finale.book.dto.response.CompletedBooksResponse;
import com.finale.finale.book.dto.response.StoryGenerationResponse;
import com.finale.finale.book.dto.response.StoryGenerationResponse.QuizResponse;
import com.finale.finale.book.dto.response.StoryGenerationResponse.SentenceResponse;
import com.finale.finale.book.dto.response.StoryGenerationResponse.UnknownWordResponse;
import com.finale.finale.book.repository.*;
import com.finale.finale.exception.CustomException;
import com.finale.finale.exception.ErrorCode;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final SentenceRepository sentenceRepository;
    private final QuizRepository quizRepository;
    private final WordRepository wordRepository;
    private final PhraseRepository phraseRepository;
    private final UnknownWordRepository unknownWordRepository;
    private final UnknownPhraseRepository unknownPhraseRepository;

    @Transactional
    public StoryGenerationResponse getNewStory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Book book = bookRepository.findFirstByUserAndIsProvisionFalseWithReviewWords(user)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_READY));

        List<Sentence> sentenceEntities = sentenceRepository.findAllByBook(book);

        List<Word> allWords = wordRepository.findAllBySentenceIn(sentenceEntities);
        List<Phrase> allPhrases = phraseRepository.findAllBySentenceIn(sentenceEntities);

        Map<Long, List<Word>> wordsBySentence = allWords.stream()
                .collect(Collectors.groupingBy(w -> w.getSentence().getId()));
        Map<Long, List<Phrase>> phrasesBySentence = allPhrases.stream()
                .collect(Collectors.groupingBy(p -> p.getSentence().getId()));

        List<SentenceResponse> sentences = sentenceEntities.stream()
                .map(sentence -> {
                    List<StoryGenerationResponse.WordResponse> words =
                            wordsBySentence.getOrDefault(sentence.getId(), Collections.emptyList())
                                    .stream()
                                    .map(word -> new StoryGenerationResponse.WordResponse(
                                            word.getWord(),
                                            word.getMeaning(),
                                            word.getLocation(),
                                            word.getLength()
                                    ))
                                    .toList();

                    List<StoryGenerationResponse.PhraseResponse> phrases =
                            phrasesBySentence.getOrDefault(sentence.getId(), Collections.emptyList())
                                    .stream()
                                    .map(phrase -> {
                                        List<StoryGenerationResponse.ExpressionResponse> expressions = phrase.getExpression().stream()
                                                .map(phraseWord -> new StoryGenerationResponse.ExpressionResponse(
                                                        phraseWord.getWord(),
                                                        phraseWord.getLocation(),
                                                        phraseWord.getLength()
                                                ))
                                                .toList();

                                        return new StoryGenerationResponse.PhraseResponse(
                                                phrase.getMeaning(),
                                                expressions
                                        );
                                    })
                                    .toList();

                    return new SentenceResponse(
                            sentence.getId(),
                            sentence.getParagraphNumber(),
                            sentence.getSentenceOrder(),
                            sentence.getEnglishText(),
                            sentence.getKoreanText(),
                            words,
                            phrases
                    );
                })
                .toList();

        List<QuizResponse> quizzes = quizRepository.findAllByBook(book).stream()
                .map(quiz -> new QuizResponse(
                        quiz.getId(),
                        quiz.getQuestion(),
                        quiz.getCorrectAnswer()
                ))
                .toList();

        List<UnknownWordResponse> unknownWords = book.getReviewWords().stream()
                .map(unknownWord -> new UnknownWordResponse(
                        unknownWord.getWord(),
                        unknownWord.getWordMeaning(),
                        unknownWord.getSentence(),
                        unknownWord.getSentenceMeaning(),
                        unknownWord.getReviewCount()
                ))
                .toList();

        List<StoryGenerationResponse.UnknownPhraseResponse> unknownPhrases =
                book.getReviewPhrases().stream()
                        .map(phrase -> new StoryGenerationResponse.UnknownPhraseResponse(
                                phrase.getPhrase(),
                                phrase.getPhraseMeaning(),
                                phrase.getSentence(),
                                phrase.getSentenceMeaning(),
                                phrase.getWords().stream()
                                        .map(w -> new
                                                StoryGenerationResponse.PhraseWordResponse(
                                                w.getWord(),
                                                w.getLocation(),
                                                w.getLength()
                                        ))
                                        .toList(),
                                phrase.getReviewCount()
                        ))
                        .toList();

        book.markAsProvision();

        sentenceEntities.forEach(sentence -> {
            wordRepository.deleteAllBySentence(sentence);
            phraseRepository.deleteAllBySentence(sentence);
        });

        return new StoryGenerationResponse(
                book.getId(),
                book.getTitle(),
                book.getCategory().getValue(),
                book.getAbilityScore(),
                book.getTotalWordCount(),
                sentences,
                quizzes,
                unknownWords,
                unknownPhrases,
                book.getCreatedAt()
        );
    }

    @Transactional
    public BookmarkResponse toggleBookmark(Long userId, Long bookId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_FOUND));

        book.validateOwner(user);
        book.validateCompleted();
        book.toggleIsBookmarked();

        return new BookmarkResponse(book.getId(), book.getIsBookmarked());
    }

    @Transactional(readOnly = true)
    public CompletedBooksResponse getCompletedBooks(Long userId, int page, int size, String sort, String category, Boolean bookmarked) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Sort.Direction direction = sort.equals("latest") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"));

        BookCategory bookCategory = category != null
                ? BookCategory.valueOf(category.toUpperCase())
                : null;

        Page<Book> bookPage = bookRepository.findCompletedBooks(user, bookCategory, bookmarked, pageable);

        List<Long> bookIds = bookPage.getContent().stream()
                .map(Book::getId)
                .toList();

        List<UnknownWord> unknownWords = bookIds.isEmpty()
                ? Collections.emptyList()
                : unknownWordRepository.findAllByBookIdIn(bookIds);

        List<UnknownPhrase> unknownPhrases = bookIds.isEmpty()
                ? Collections.emptyList()
                : unknownPhraseRepository.findAllByBookIdIn(bookIds);

        Map<Long, List<UnknownWord>> unknownWordsByBook = unknownWords.stream()
                .collect(Collectors.groupingBy(uw -> uw.getBook().getId()));

        Map<Long, List<UnknownPhrase>> unknownPhrasesByBook =
                unknownPhrases.stream()
                        .collect(Collectors.groupingBy(up -> up.getBook().getId()));

        List<CompletedBooksResponse.CompletedBook> content = bookPage.getContent().stream()
                .map(book -> toCompletedBook(
                        book,
                        unknownWordsByBook.getOrDefault(book.getId(), Collections.emptyList()),
                        unknownPhrasesByBook.getOrDefault(book.getId(), Collections.emptyList())
                ))
                .toList();

        return new CompletedBooksResponse(
                content,
                (int) bookPage.getTotalElements(),
                bookPage.getTotalPages(),
                bookPage.getNumber(),
                bookPage.getSize(),
                bookPage.hasNext(),
                bookPage.hasPrevious()
        );
    }

    @Transactional(readOnly = true)
    public CompletedBookDetailResponse getCompletedBookDetail(Long userId, Long bookId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_FOUND));

        book.validateOwner(user);
        book.validateCompleted();

        List<Sentence> sentences = sentenceRepository.findAllByBook(book);
        List<Quiz> quizzes = quizRepository.findAllByBook(book);
        List<UnknownWord> unknownWords = unknownWordRepository.findAllByBook(book);
        List<UnknownPhrase> unknownPhrases = unknownPhraseRepository.findAllByBook(book);

        return new CompletedBookDetailResponse(
                bookId,
                book.getTitle(),
                book.getCategory().getValue(),
                book.getAbilityScore(),
                book.getCreatedAt(), // TODO: completedAt 필드로 수정 필요
                sentences.stream()
                        .map(sentence -> new CompletedBookDetailResponse.SentenceResponse(
                                sentence.getId(),
                                sentence.getParagraphNumber(),
                                sentence.getSentenceOrder(),
                                sentence.getEnglishText(),
                                sentence.getKoreanText()
                        ))
                        .toList(),
                quizzes.stream()
                        .map(quiz -> new CompletedBookDetailResponse.QuizResponse(
                                quiz.getId(),
                                quiz.getQuestion(),
                                quiz.getCorrectAnswer(),
                                quiz.getUserAnswer(),
                                Objects.equals(quiz.getCorrectAnswer(), quiz.getUserAnswer())
                        ))
                        .toList(),
                unknownWords.stream()
                        .map(word -> new CompletedBookDetailResponse.UnknownWordResponse(
                                word.getId(),
                                word.getWord(),
                                word.getWordMeaning(),
                                word.getSentenceId(),
                                word.getSentence(),
                                word.getSentenceMeaning(),
                                word.getLocation(),
                                word.getLength(),
                                word.getNextReviewDate(),
                                word.getCreatedAt()
                        ))
                        .toList(),
                unknownPhrases.stream()
                        .map(phrase -> new CompletedBookDetailResponse.UnknownPhraseResponse(
                                phrase.getId(),
                                phrase.getPhrase(),
                                phrase.getPhraseMeaning(),
                                phrase.getSentenceId(),
                                phrase.getSentence(),
                                phrase.getSentenceMeaning(),
                                phrase.getWords().stream()
                                        .map(w -> new CompletedBookDetailResponse.PhraseWordResponse(
                                                w.getWord(),
                                                w.getLocation(),
                                                w.getLength()
                                        ))
                                        .toList(),
                                phrase.getNextReviewDate(),
                                phrase.getCreatedAt()
                        ))
                        .toList()
        );
    }

    @Transactional
    public void deleteBook(Long userId, Long bookId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_FOUND));

        book.validateOwner(user);

        List<Sentence> sentences = sentenceRepository.findAllByBook(book);

        sentences.forEach(sentence -> {
            wordRepository.deleteAllBySentence(sentence);
            phraseRepository.deleteAllBySentence(sentence);
        });

        unknownWordRepository.deleteAllByBook(book);
        unknownPhraseRepository.deleteAllByBook(book);
        sentenceRepository.deleteAllByBook(book);
        quizRepository.deleteAllByBook(book);
        bookRepository.delete(book);
    }

    private CompletedBooksResponse.CompletedBook toCompletedBook(Book book, List<UnknownWord> unknownWords, List<UnknownPhrase> unknownPhrases) {
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
                                        .map(w -> new
                                                CompletedBooksResponse.PhraseWordResponse(
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
