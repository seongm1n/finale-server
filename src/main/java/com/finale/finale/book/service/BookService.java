package com.finale.finale.book.service;

import com.finale.finale.auth.domain.User;
import com.finale.finale.auth.repository.UserRepository;
import com.finale.finale.book.domain.*;
import com.finale.finale.book.dto.response.BookmarkResponse;
import com.finale.finale.book.dto.response.CompletedBookDetailResponse;
import com.finale.finale.book.dto.response.CompletedBooksResponse;
import com.finale.finale.book.dto.response.StoryGenerationResponse;
import com.finale.finale.book.repository.*;
import com.finale.finale.exception.CustomException;
import com.finale.finale.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    private final StoryResponseAssembler storyResponseAssembler;
    private final CompletedBooksAssembler completedBooksAssembler;
    private final CompletedBookDetailAssembler completedBookDetailAssembler;

    @Transactional
    public StoryGenerationResponse getNewStory(Long userId) {
        User user = findUser(userId);
        Book book = findReadyBook(user);

        List<Sentence> sentenceEntities = sentenceRepository.findAllByBook(book);

        StoryData storyData = loadStoryData(book, sentenceEntities);
        StoryGenerationResponse response = storyResponseAssembler.toResponse(book, storyData);

        finalizeProvision(book, sentenceEntities);

        return response;
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

        List<CompletedBooksResponse.CompletedBook> content = completedBooksAssembler.toCompletedBooks(
                bookPage.getContent(),
                unknownWordsByBook,
                unknownPhrasesByBook
        );

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

        return completedBookDetailAssembler.toResponse(book, sentences, quizzes, unknownWords, unknownPhrases);
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

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private Book findReadyBook(User user) {
        return bookRepository.findFirstByUserAndIsProvisionFalseWithReviewWords(user)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_READY));
    }

    private StoryData loadStoryData(Book book, List<Sentence> sentences) {
        List<Word> allWords = wordRepository.findAllBySentenceIn(sentences);
        List<Phrase> allPhrases = phraseRepository.findAllBySentenceIn(sentences);

        Map<Long, List<Word>> wordsBySentence = allWords.stream()
                .collect(Collectors.groupingBy(w -> w.getSentence().getId()));
        Map<Long, List<Phrase>> phrasesBySentence = allPhrases.stream()
                .collect(Collectors.groupingBy(p -> p.getSentence().getId()));

        List<Quiz> quizzes = quizRepository.findAllByBook(book);
        List<UnknownWord> unknownWords = book.getReviewWords().stream().toList();
        List<UnknownPhrase> unknownPhrases = book.getReviewPhrases().stream().toList();

        return new StoryData(sentences, wordsBySentence, phrasesBySentence, quizzes, unknownWords, unknownPhrases);
    }

    private void finalizeProvision(Book book, List<Sentence> sentences) {
        book.markAsProvision();

        sentences.forEach(sentence -> {
            wordRepository.deleteAllBySentence(sentence);
            phraseRepository.deleteAllBySentence(sentence);
        });
    }

}
