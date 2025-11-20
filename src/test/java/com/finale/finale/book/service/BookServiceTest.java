package com.finale.finale.book.service;

import com.finale.finale.auth.domain.User;
import com.finale.finale.auth.repository.UserRepository;
import com.finale.finale.book.domain.*;
import com.finale.finale.book.dto.response.CompletedBookDetailResponse;
import com.finale.finale.book.dto.response.CompletedBooksResponse;
import com.finale.finale.book.dto.response.StoryGenerationResponse;
import com.finale.finale.book.repository.BookRepository;
import com.finale.finale.book.repository.QuizRepository;
import com.finale.finale.book.repository.SentenceRepository;
import com.finale.finale.book.repository.UnknownPhraseRepository;
import com.finale.finale.book.repository.UnknownWordRepository;
import com.finale.finale.exception.CustomException;
import com.finale.finale.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService 테스트")
class BookServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private SentenceRepository sentenceRepository;

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private com.finale.finale.book.repository.WordRepository wordRepository;

    @Mock
    private com.finale.finale.book.repository.PhraseRepository phraseRepository;

    @Mock
    private UnknownWordRepository unknownWordRepository;

    @Mock
    private UnknownPhraseRepository unknownPhraseRepository;

    @Spy
    private StoryResponseAssembler storyResponseAssembler;

    @Spy
    private CompletedBooksAssembler completedBooksAssembler;

    @Spy
    private CompletedBookDetailAssembler completedBookDetailAssembler;

    @InjectMocks
    private BookService bookService;

    @Test
    @DisplayName("getNewStory 성공 - 미할당 책을 제공하고 isProvision을 true로 변경")
    void getNewStorySuccess() {
        // Given
        Long userId = 1L;
        User user = new User("test@example.com");
        Book book = new Book(user, "Test Book", BookCategory.ADVENTURE, 800, 1000);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(bookRepository.findFirstByUserAndIsProvisionFalseWithReviewWords(user))
                .willReturn(Optional.of(book));
        given(sentenceRepository.findAllByBook(book)).willReturn(List.of());
        given(quizRepository.findAllByBook(book)).willReturn(List.of());
        given(wordRepository.findAllBySentenceIn(List.of())).willReturn(List.of());
        given(phraseRepository.findAllBySentenceIn(List.of())).willReturn(List.of());

        // When
        StoryGenerationResponse response = bookService.getNewStory(userId);

        // Then
        assertThat(response.bookId()).isEqualTo(book.getId());
        assertThat(book.getIsProvision()).isTrue();
    }

    @Test
    @DisplayName("getNewStory 실패 - 미할당 책이 없으면 BOOK_NOT_READY")
    void getNewStoryFailBookNotReady() {
        // Given
        Long userId = 1L;
        User user = new User("test@example.com");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(bookRepository.findFirstByUserAndIsProvisionFalseWithReviewWords(user))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookService.getNewStory(userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOK_NOT_READY);
    }

    @Test
    @DisplayName("getNewStory 성공 - Word와 Phrase가 포함된 Response 반환")
    void getNewStoryWithWordAndPhrase() {
        // Given
        Long userId = 1L;
        User user = new User("test@example.com");
        Book book = new Book(user, "Test Book", BookCategory.ADVENTURE, 800, 1000);
        Sentence sentence = new Sentence(book, 1, 1, "She looked up.", "그녀는 올려다봤다.");
        ReflectionTestUtils.setField(sentence, "id", 1L);

        Word word1 = new Word(sentence, "She", "그녀", 0);
        Word word2 = new Word(sentence, "looked", "보다", 4);

        Phrase phrase = new Phrase(sentence, "올려다보다");
        PhraseWord phraseWord1 = new PhraseWord(phrase, "looked", 4);
        PhraseWord phraseWord2 = new PhraseWord(phrase, "up", 11);
        phrase.addPhraseWord(phraseWord1);
        phrase.addPhraseWord(phraseWord2);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(bookRepository.findFirstByUserAndIsProvisionFalseWithReviewWords(user))
                .willReturn(Optional.of(book));
        given(sentenceRepository.findAllByBook(book)).willReturn(List.of(sentence));
        given(quizRepository.findAllByBook(book)).willReturn(List.of());
        given(wordRepository.findAllBySentenceIn(List.of(sentence))).willReturn(List.of(word1, word2));
        given(phraseRepository.findAllBySentenceIn(List.of(sentence))).willReturn(List.of(phrase));

        // When
        StoryGenerationResponse response = bookService.getNewStory(userId);

        // Then
        assertThat(response.sentences()).hasSize(1);
        assertThat(response.sentences().get(0).words()).hasSize(2);
        assertThat(response.sentences().get(0).phrases()).hasSize(1);
        assertThat(response.sentences().get(0).phrases().get(0).expression()).hasSize(2);
    }

    @Test
    @DisplayName("getNewStory 성공 - 조회 후 Word와 Phrase 삭제")
    void getNewStoryDeleteWordAndPhrase() {
        // Given
        Long userId = 1L;
        User user = new User("test@example.com");
        Book book = new Book(user, "Test Book", BookCategory.ADVENTURE, 800, 1000);
        Sentence sentence = new Sentence(book, 1, 1, "Test sentence.", "테스트 문장.");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(bookRepository.findFirstByUserAndIsProvisionFalseWithReviewWords(user))
                .willReturn(Optional.of(book));
        given(sentenceRepository.findAllByBook(book)).willReturn(List.of(sentence));
        given(quizRepository.findAllByBook(book)).willReturn(List.of());
        given(wordRepository.findAllBySentenceIn(List.of(sentence))).willReturn(List.of());
        given(phraseRepository.findAllBySentenceIn(List.of(sentence))).willReturn(List.of());

        // When
        bookService.getNewStory(userId);

        // Then
        verify(wordRepository).deleteAllBySentence(sentence);
        verify(phraseRepository).deleteAllBySentence(sentence);
    }

    @Test
    @DisplayName("getCompletedBooks 성공 - 기본 조회 (최신순, 전체)")
    void getCompletedBooksSuccess() {
        // Given
        Long userId = 1L;
        User user = new User("test@example.com");
        ReflectionTestUtils.setField(user, "id", userId);

        Book book1 = new Book(user, "Book 1", BookCategory.ADVENTURE, 800, 1000);
        ReflectionTestUtils.setField(book1, "id", 1L);
        ReflectionTestUtils.setField(book1, "createdAt", LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(book1, "isCompleted", true);

        Book book2 = new Book(user, "Book 2", BookCategory.COMEDY, 700, 900);
        ReflectionTestUtils.setField(book2, "id", 2L);
        ReflectionTestUtils.setField(book2, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(book2, "isCompleted", true);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Book> bookPage = new PageImpl<>(List.of(book2, book1), pageable, 2);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(bookRepository.findCompletedBooks(eq(user), eq(null), eq(null), any(Pageable.class)))
                .willReturn(bookPage);
        given(unknownWordRepository.findAllByBookIdIn(List.of(2L, 1L))).willReturn(Collections.emptyList());
        given(unknownPhraseRepository.findAllByBookIdIn(List.of(2L, 1L))).willReturn(Collections.emptyList());

        // When
        CompletedBooksResponse response = bookService.getCompletedBooks(
                userId, 0, 10, "latest", null, null
        );

        // Then
        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).id()).isEqualTo(2L);
        assertThat(response.content().get(1).id()).isEqualTo(1L);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("getCompletedBooks 성공 - 카테고리 필터링")
    void getCompletedBooksWithCategoryFilter() {
        // Given
        Long userId = 1L;
        User user = new User("test@example.com");
        ReflectionTestUtils.setField(user, "id", userId);

        Book book = new Book(user, "Adventure Book", BookCategory.ADVENTURE, 800, 1000);
        ReflectionTestUtils.setField(book, "id", 1L);
        ReflectionTestUtils.setField(book, "isCompleted", true);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Book> bookPage = new PageImpl<>(List.of(book), pageable, 1);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(bookRepository.findCompletedBooks(eq(user), eq(BookCategory.ADVENTURE), eq(null), any(Pageable.class)))
                .willReturn(bookPage);
        given(unknownWordRepository.findAllByBookIdIn(List.of(1L))).willReturn(Collections.emptyList());
        given(unknownPhraseRepository.findAllByBookIdIn(List.of(1L))).willReturn(Collections.emptyList());

        // When
        CompletedBooksResponse response = bookService.getCompletedBooks(
                userId, 0, 10, "latest", "adventure", null
        );

        // Then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).category()).isEqualTo("adventure");
    }

    @Test
    @DisplayName("getCompletedBooks 성공 - 북마크 필터링")
    void getCompletedBooksWithBookmarkFilter() {
        // Given
        Long userId = 1L;
        User user = new User("test@example.com");
        ReflectionTestUtils.setField(user, "id", userId);

        Book book = new Book(user, "Bookmarked Book", BookCategory.COMEDY, 700, 900);
        ReflectionTestUtils.setField(book, "id", 1L);
        ReflectionTestUtils.setField(book, "isCompleted", true);
        ReflectionTestUtils.setField(book, "isBookmarked", true);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Book> bookPage = new PageImpl<>(List.of(book), pageable, 1);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(bookRepository.findCompletedBooks(eq(user), eq(null), eq(true), any(Pageable.class)))
                .willReturn(bookPage);
        given(unknownWordRepository.findAllByBookIdIn(List.of(1L))).willReturn(Collections.emptyList());
        given(unknownPhraseRepository.findAllByBookIdIn(List.of(1L))).willReturn(Collections.emptyList());

        // When
        CompletedBooksResponse response = bookService.getCompletedBooks(
                userId, 0, 10, "latest", null, true
        );

        // Then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).isBookmarked()).isTrue();
    }

    @Test
    @DisplayName("getCompletedBooks 성공 - 정렬 (오래된순)")
    void getCompletedBooksWithOldestSort() {
        // Given
        Long userId = 1L;
        User user = new User("test@example.com");
        ReflectionTestUtils.setField(user, "id", userId);

        Book book1 = new Book(user, "Old Book", BookCategory.ADVENTURE, 800, 1000);
        ReflectionTestUtils.setField(book1, "id", 1L);
        ReflectionTestUtils.setField(book1, "createdAt", LocalDateTime.now().minusDays(5));
        ReflectionTestUtils.setField(book1, "isCompleted", true);

        Book book2 = new Book(user, "New Book", BookCategory.COMEDY, 700, 900);
        ReflectionTestUtils.setField(book2, "id", 2L);
        ReflectionTestUtils.setField(book2, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(book2, "isCompleted", true);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<Book> bookPage = new PageImpl<>(List.of(book1, book2), pageable, 2);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(bookRepository.findCompletedBooks(eq(user), eq(null), eq(null), any(Pageable.class)))
                .willReturn(bookPage);
        given(unknownWordRepository.findAllByBookIdIn(List.of(1L, 2L))).willReturn(Collections.emptyList());
        given(unknownPhraseRepository.findAllByBookIdIn(List.of(1L, 2L))).willReturn(Collections.emptyList());

        // When
        CompletedBooksResponse response = bookService.getCompletedBooks(
                userId, 0, 10, "oldest", null, null
        );

        // Then
        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).id()).isEqualTo(1L);
        assertThat(response.content().get(1).id()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getCompletedBooks 성공 - UnknownWord 포함")
    void getCompletedBooksWithUnknownWords() {
        // Given
        Long userId = 1L;
        User user = new User("test@example.com");
        ReflectionTestUtils.setField(user, "id", userId);

        Book book = new Book(user, "Book with words", BookCategory.ADVENTURE, 800, 1000);
        ReflectionTestUtils.setField(book, "id", 1L);
        ReflectionTestUtils.setField(book, "isCompleted", true);

        UnknownWord word1 = new UnknownWord(
                user, book, "example", "예시",
                "This is an example.", "이것은 예시입니다.",
                1L, 11, 7, LocalDate.now()
        );
        ReflectionTestUtils.setField(word1, "id", 1L);
        ReflectionTestUtils.setField(word1, "createdAt", LocalDateTime.now());

        UnknownWord word2 = new UnknownWord(
                user, book, "test", "테스트",
                "This is a test.", "이것은 테스트입니다.",
                2L, 10, 4, LocalDate.now()
        );
        ReflectionTestUtils.setField(word2, "id", 2L);
        ReflectionTestUtils.setField(word2, "createdAt", LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Book> bookPage = new PageImpl<>(List.of(book), pageable, 1);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(bookRepository.findCompletedBooks(eq(user), eq(null), eq(null), any(Pageable.class)))
                .willReturn(bookPage);
        given(unknownWordRepository.findAllByBookIdIn(List.of(1L))).willReturn(List.of(word1, word2));
        given(unknownPhraseRepository.findAllByBookIdIn(List.of(1L))).willReturn(Collections.emptyList());

        // When
        CompletedBooksResponse response = bookService.getCompletedBooks(
                userId, 0, 10, "latest", null, null
        );

        // Then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).unknownWords()).hasSize(2);
        assertThat(response.content().get(0).unknownWords().get(0).word()).isEqualTo("example");
        assertThat(response.content().get(0).unknownWords().get(1).word()).isEqualTo("test");
    }

    @Test
    @DisplayName("getCompletedBooks 성공 - 빈 목록 (완료한 책 없음)")
    void getCompletedBooksEmpty() {
        // Given
        Long userId = 1L;
        User user = new User("test@example.com");
        ReflectionTestUtils.setField(user, "id", userId);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Book> bookPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(bookRepository.findCompletedBooks(eq(user), eq(null), eq(null), any(Pageable.class)))
                .willReturn(bookPage);

        // When
        CompletedBooksResponse response = bookService.getCompletedBooks(
                userId, 0, 10, "latest", null, null
        );

        // Then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0);
        assertThat(response.totalPages()).isEqualTo(0);
    }

    @Test
    @DisplayName("getCompletedBooks 성공 - 페이징 (2페이지)")
    void getCompletedBooksWithPagination() {
        // Given
        Long userId = 1L;
        User user = new User("test@example.com");
        ReflectionTestUtils.setField(user, "id", userId);

        List<Book> books = List.of(
                createCompletedBook(user, 11L, "Book 11"),
                createCompletedBook(user, 12L, "Book 12"),
                createCompletedBook(user, 13L, "Book 13"),
                createCompletedBook(user, 14L, "Book 14"),
                createCompletedBook(user, 15L, "Book 15")
        );

        Pageable pageable = PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Book> bookPage = new PageImpl<>(books, pageable, 15L);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(bookRepository.findCompletedBooks(eq(user), eq(null), eq(null), any(Pageable.class)))
                .willReturn(bookPage);
        given(unknownWordRepository.findAllByBookIdIn(anyList())).willReturn(Collections.emptyList());
        given(unknownPhraseRepository.findAllByBookIdIn(anyList())).willReturn(Collections.emptyList());

        // When
        CompletedBooksResponse response = bookService.getCompletedBooks(
                userId, 1, 10, "latest", null, null
        );

        // Then
        assertThat(response.currentPage()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(15);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.hasPrevious()).isTrue();
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("getCompletedBooks 실패 - 사용자 없음")
    void getCompletedBooksUserNotFound() {
        // Given
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookService.getCompletedBooks(
                userId, 0, 10, "latest", null, null
        ))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("getCompletedBooks 성공 - N+1 방지 확인 (일괄 조회)")
    void getCompletedBooksNoNPlusOne() {
        // Given
        Long userId = 1L;
        User user = new User("test@example.com");
        ReflectionTestUtils.setField(user, "id", userId);

        Book book1 = new Book(user, "Book 1", BookCategory.ADVENTURE, 800, 1000);
        ReflectionTestUtils.setField(book1, "id", 1L);
        ReflectionTestUtils.setField(book1, "isCompleted", true);

        Book book2 = new Book(user, "Book 2", BookCategory.COMEDY, 700, 900);
        ReflectionTestUtils.setField(book2, "id", 2L);
        ReflectionTestUtils.setField(book2, "isCompleted", true);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Book> bookPage = new PageImpl<>(List.of(book1, book2), pageable, 2);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(bookRepository.findCompletedBooks(eq(user), eq(null), eq(null), any(Pageable.class)))
                .willReturn(bookPage);
        given(unknownWordRepository.findAllByBookIdIn(List.of(1L, 2L))).willReturn(Collections.emptyList());
        given(unknownPhraseRepository.findAllByBookIdIn(List.of(1L, 2L))).willReturn(Collections.emptyList());

        // When
        bookService.getCompletedBooks(userId, 0, 10, "latest", null, null);

        // Then
        verify(unknownWordRepository).findAllByBookIdIn(List.of(1L, 2L));
        verify(unknownPhraseRepository).findAllByBookIdIn(List.of(1L, 2L));
    }

    private Book createCompletedBook(User user, Long id, String title) {
        Book book = new Book(user, title, BookCategory.ADVENTURE, 800, 1000);
        ReflectionTestUtils.setField(book, "id", id);
        ReflectionTestUtils.setField(book, "isCompleted", true);
        return book;
    }

    @Test
    @DisplayName("getCompletedBookDetail 성공 - 완료된 책 상세 조회")
    void getCompletedBookDetailSuccess() {
        // Given
        Long userId = 1L;
        Long bookId = 1L;
        User user = new User("test@example.com");
        ReflectionTestUtils.setField(user, "id", userId);

        Book book = new Book(user, "Test Book", BookCategory.ADVENTURE, 800, 1000);
        ReflectionTestUtils.setField(book, "id", bookId);
        ReflectionTestUtils.setField(book, "isCompleted", true);
        ReflectionTestUtils.setField(book, "createdAt", LocalDateTime.now());

        Sentence sentence1 = new Sentence(book, 1, 1, "Hello world.", "안녕 세상.");
        ReflectionTestUtils.setField(sentence1, "id", 1L);
        Sentence sentence2 = new Sentence(book, 1, 2, "This is a test.", "이것은 테스트입니다.");
        ReflectionTestUtils.setField(sentence2, "id", 2L);

        Quiz quiz1 = new Quiz(book, "What is this?", true);
        ReflectionTestUtils.setField(quiz1, "id", 1L);
        ReflectionTestUtils.setField(quiz1, "userAnswer", true);

        Quiz quiz2 = new Quiz(book, "Is this correct?", false);
        ReflectionTestUtils.setField(quiz2, "id", 2L);
        ReflectionTestUtils.setField(quiz2, "userAnswer", true);

        UnknownWord word1 = new UnknownWord(
                user, book, "hello", "안녕",
                "Hello world.", "안녕 세상.",
                1L, 0, 5, LocalDate.now()
        );
        ReflectionTestUtils.setField(word1, "id", 1L);
        ReflectionTestUtils.setField(word1, "createdAt", LocalDateTime.now());

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
        given(sentenceRepository.findAllByBook(book)).willReturn(List.of(sentence1, sentence2));
        given(quizRepository.findAllByBook(book)).willReturn(List.of(quiz1, quiz2));
        given(unknownWordRepository.findAllByBook(book)).willReturn(List.of(word1));
        given(unknownPhraseRepository.findAllByBook(book)).willReturn(Collections.emptyList());

        // When
        CompletedBookDetailResponse response = bookService.getCompletedBookDetail(userId, bookId);

        // Then
        assertThat(response.bookId()).isEqualTo(bookId);
        assertThat(response.title()).isEqualTo("Test Book");
        assertThat(response.category()).isEqualTo("adventure");
        assertThat(response.abilityScore()).isEqualTo(800);
        assertThat(response.sentences()).hasSize(2);
        assertThat(response.sentences().get(0).sentenceId()).isEqualTo(1L);
        assertThat(response.sentences().get(0).englishText()).isEqualTo("Hello world.");
        assertThat(response.quizzes()).hasSize(2);
        assertThat(response.quizzes().get(0).isCorrect()).isTrue();
        assertThat(response.quizzes().get(1).isCorrect()).isFalse();
        assertThat(response.unknownWords()).hasSize(1);
        assertThat(response.unknownWords().get(0).word()).isEqualTo("hello");
    }

    @Test
    @DisplayName("getCompletedBookDetail 실패 - 사용자 없음")
    void getCompletedBookDetailUserNotFound() {
        // Given
        Long userId = 999L;
        Long bookId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookService.getCompletedBookDetail(userId, bookId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("getCompletedBookDetail 실패 - 책 없음")
    void getCompletedBookDetailBookNotFound() {
        // Given
        Long userId = 1L;
        Long bookId = 999L;
        User user = new User("test@example.com");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(bookRepository.findById(bookId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookService.getCompletedBookDetail(userId, bookId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOK_NOT_FOUND);
    }

    @Test
    @DisplayName("getCompletedBookDetail 실패 - 다른 사용자의 책")
    void getCompletedBookDetailUserMismatch() {
        // Given
        Long userId = 1L;
        Long bookId = 1L;
        User owner = new User("owner@example.com");
        ReflectionTestUtils.setField(owner, "id", 2L);
        User other = new User("other@example.com");
        ReflectionTestUtils.setField(other, "id", userId);

        Book book = new Book(owner, "Owner's Book", BookCategory.ADVENTURE, 800, 1000);
        ReflectionTestUtils.setField(book, "id", bookId);
        ReflectionTestUtils.setField(book, "isCompleted", true);

        given(userRepository.findById(userId)).willReturn(Optional.of(other));
        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));

        // When & Then
        assertThatThrownBy(() -> bookService.getCompletedBookDetail(userId, bookId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOK_USER_MISMATCH);
    }

    @Test
    @DisplayName("getCompletedBookDetail 실패 - 완료되지 않은 책")
    void getCompletedBookDetailNotCompleted() {
        // Given
        Long userId = 1L;
        Long bookId = 1L;
        User user = new User("test@example.com");
        ReflectionTestUtils.setField(user, "id", userId);

        Book book = new Book(user, "Not Completed Book", BookCategory.ADVENTURE, 800, 1000);
        ReflectionTestUtils.setField(book, "id", bookId);
        ReflectionTestUtils.setField(book, "isCompleted", false);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));

        // When & Then
        assertThatThrownBy(() -> bookService.getCompletedBookDetail(userId, bookId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOK_NOT_COMPLETED);
    }

    @Test
    @DisplayName("deleteBook 성공 - 책과 모든 연관 데이터 삭제")
    void deleteBookSuccess() {
        // Given
        Long userId = 1L;
        Long bookId = 1L;
        User user = new User("test@example.com");
        ReflectionTestUtils.setField(user, "id", userId);

        Book book = new Book(user, "Test Book", BookCategory.ADVENTURE, 800, 1000);
        ReflectionTestUtils.setField(book, "id", bookId);

        Sentence sentence1 = new Sentence(book, 1, 1, "Test sentence 1.", "테스트 문장 1.");
        Sentence sentence2 = new Sentence(book, 1, 2, "Test sentence 2.", "테스트 문장 2.");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
        given(sentenceRepository.findAllByBook(book)).willReturn(List.of(sentence1, sentence2));

        // When
        bookService.deleteBook(userId, bookId);

        // Then
        verify(wordRepository).deleteAllBySentence(sentence1);
        verify(wordRepository).deleteAllBySentence(sentence2);
        verify(phraseRepository).deleteAllBySentence(sentence1);
        verify(phraseRepository).deleteAllBySentence(sentence2);
        verify(unknownWordRepository).deleteAllByBook(book);
        verify(unknownPhraseRepository).deleteAllByBook(book);
        verify(sentenceRepository).deleteAllByBook(book);
        verify(quizRepository).deleteAllByBook(book);
        verify(bookRepository).delete(book);
    }

    @Test
    @DisplayName("deleteBook 실패 - 사용자 없음")
    void deleteBookUserNotFound() {
        // Given
        Long userId = 999L;
        Long bookId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookService.deleteBook(userId, bookId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("deleteBook 실패 - 책 없음")
    void deleteBookNotFound() {
        // Given
        Long userId = 1L;
        Long bookId = 999L;
        User user = new User("test@example.com");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(bookRepository.findById(bookId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookService.deleteBook(userId, bookId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOK_NOT_FOUND);
    }

    @Test
    @DisplayName("deleteBook 실패 - 다른 사용자의 책")
    void deleteBookUserMismatch() {
        // Given
        Long userId = 1L;
        Long bookId = 1L;
        User owner = new User("owner@example.com");
        ReflectionTestUtils.setField(owner, "id", 2L);
        User other = new User("other@example.com");
        ReflectionTestUtils.setField(other, "id", userId);

        Book book = new Book(owner, "Owner's Book", BookCategory.ADVENTURE, 800, 1000);
        ReflectionTestUtils.setField(book, "id", bookId);

        given(userRepository.findById(userId)).willReturn(Optional.of(other));
        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));

        // When & Then
        assertThatThrownBy(() -> bookService.deleteBook(userId, bookId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOK_USER_MISMATCH);
    }

    @Test
    @DisplayName("deleteBook 성공 - 완료되지 않은 책도 삭제 가능")
    void deleteBookNotCompletedAllowed() {
        // Given
        Long userId = 1L;
        Long bookId = 1L;
        User user = new User("test@example.com");
        ReflectionTestUtils.setField(user, "id", userId);

        Book book = new Book(user, "Not Completed Book", BookCategory.ADVENTURE, 800, 1000);
        ReflectionTestUtils.setField(book, "id", bookId);
        ReflectionTestUtils.setField(book, "isCompleted", false);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
        given(sentenceRepository.findAllByBook(book)).willReturn(List.of());

        // When
        bookService.deleteBook(userId, bookId);

        // Then
        verify(bookRepository).delete(book);
    }

    @Test
    @DisplayName("deleteBook 성공 - 연관 데이터 삭제 순서 검증")
    void deleteBookVerifyDeletionOrder() {
        // Given
        Long userId = 1L;
        Long bookId = 1L;
        User user = new User("test@example.com");
        ReflectionTestUtils.setField(user, "id", userId);

        Book book = new Book(user, "Test Book", BookCategory.ADVENTURE, 800, 1000);
        ReflectionTestUtils.setField(book, "id", bookId);

        Sentence sentence = new Sentence(book, 1, 1, "Test.", "테스트.");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(bookRepository.findById(bookId)).willReturn(Optional.of(book));
        given(sentenceRepository.findAllByBook(book)).willReturn(List.of(sentence));

        // When
        bookService.deleteBook(userId, bookId);

        // Then - 순서대로 검증
        var inOrder = inOrder(
                wordRepository, phraseRepository,
                unknownWordRepository, unknownPhraseRepository, sentenceRepository, quizRepository,
                bookRepository
        );

        // 1. Word, Phrase 먼저 삭제
        inOrder.verify(wordRepository).deleteAllBySentence(sentence);
        inOrder.verify(phraseRepository).deleteAllBySentence(sentence);

        // 2. Book의 자식 엔티티들 삭제
        inOrder.verify(unknownWordRepository).deleteAllByBook(book);
        inOrder.verify(unknownPhraseRepository).deleteAllByBook(book);
        inOrder.verify(sentenceRepository).deleteAllByBook(book);
        inOrder.verify(quizRepository).deleteAllByBook(book);

        // 3. Book 마지막 삭제
        inOrder.verify(bookRepository).delete(book);
    }
}
