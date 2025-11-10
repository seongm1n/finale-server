package com.finale.finale.book.service;

import com.finale.finale.auth.domain.User;
import com.finale.finale.auth.repository.UserRepository;
import com.finale.finale.book.domain.*;
import com.finale.finale.book.dto.response.StoryGenerationResponse;
import com.finale.finale.book.repository.BookRepository;
import com.finale.finale.book.repository.QuizRepository;
import com.finale.finale.book.repository.SentenceRepository;
import com.finale.finale.book.repository.UnknownWordRepository;
import com.finale.finale.exception.CustomException;
import com.finale.finale.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import org.springframework.test.util.ReflectionTestUtils;

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
}
