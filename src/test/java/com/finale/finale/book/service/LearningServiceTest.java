package com.finale.finale.book.service;

import com.finale.finale.auth.domain.User;
import com.finale.finale.auth.repository.UserRepository;
import com.finale.finale.book.domain.Book;
import com.finale.finale.book.domain.BookCategory;
import com.finale.finale.book.domain.Quiz;
import com.finale.finale.book.dto.request.CompleteRequest;
import com.finale.finale.book.dto.response.CompleteResponse;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LearningService 테스트")
public class LearningServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private SentenceRepository sentenceRepository;

    @Mock
    private UnknownWordRepository unknownWordRepository;

    @InjectMocks
    private LearningService learningService;

    @Test
    @DisplayName("책 완료 처리 - 성공")
    void completeSuccess() {
        // Given
        User user = new User("test@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);

        Book book = new Book(user, "test book", BookCategory.ADVENTURE, 500, 600);
        ReflectionTestUtils.setField(book, "id", 1L);

        Quiz quiz1 = new Quiz(book, "Quesion 1", true);
        ReflectionTestUtils.setField(quiz1, "id", 1L);
        Quiz quiz2 = new Quiz(book, "Quesion 2", false);
        ReflectionTestUtils.setField(quiz2, "id", 2L);

        CompleteRequest request = new CompleteRequest(
                List.of(
                        new CompleteRequest.QuizAnswer(1L, true),
                        new CompleteRequest.QuizAnswer(2L, false)
                ),
                List.of()
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(bookRepository.findById(1L)).willReturn(Optional.of(book));
        given(quizRepository.findAllById(any())).willReturn(List.of(quiz1, quiz2));
        given(sentenceRepository.countByBookId(1L)).willReturn(50);

        // When
        CompleteResponse response = learningService.complete(1L, 1L, request);

        // Then
        assertThat(response.bookId()).isEqualTo(1L);
        assertThat(response.quizResult().correctAnswers()).isEqualTo(2);
        assertThat(book.getIsCompleted()).isTrue();
        verify(bookRepository).save(book);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("책 완료 처리 - 사용자 없음")
    void completeUserNotFound() {
        // Given
        CompleteRequest request = new CompleteRequest(List.of(), List.of());
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> learningService.complete(999L, 1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("책 완료 처리 - 책 없음")
    void completeBookNotFound() {
        // Given
        User user = new User("test@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);

        CompleteRequest request = new CompleteRequest(List.of(), List.of());
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(bookRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> learningService.complete(1L, 999L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOK_NOT_FOUND);
    }

    @Test
    @DisplayName("퀴즈 정답 확인 - 일부 정답")
    void completePartialCorrect() {
        // Given
        User user = new User("test@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);

        Book book = new Book(user, "Test Book", BookCategory.ADVENTURE, 500, 600);
        ReflectionTestUtils.setField(book, "id", 1L);

        Quiz quiz1 = new Quiz(book, "Question 1", true);
        ReflectionTestUtils.setField(quiz1, "id", 1L);
        Quiz quiz2 = new Quiz(book, "Question 2", false);
        ReflectionTestUtils.setField(quiz2, "id", 2L);

        CompleteRequest request = new CompleteRequest(
                List.of(
                        new CompleteRequest.QuizAnswer(1L, true),  // 정답
                        new CompleteRequest.QuizAnswer(2L, true)   // 오답
                ),
                List.of()
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(bookRepository.findById(1L)).willReturn(Optional.of(book));
        given(quizRepository.findAllById(any())).willReturn(List.of(quiz1, quiz2));
        given(sentenceRepository.countByBookId(1L)).willReturn(50);

        // When
        CompleteResponse response = learningService.complete(1L, 1L, request);

        // Then
        assertThat(response.quizResult().totalQuestions()).isEqualTo(2);
        assertThat(response.quizResult().correctAnswers()).isEqualTo(1);
    }

    @Test
    @DisplayName("모르는 단어 저장")
    void completeSaveUnknownWords() {
        // Given
        User user = new User("test@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);

        Book book = new Book(user, "Test Book", BookCategory.ADVENTURE, 500, 600);
        ReflectionTestUtils.setField(book, "id", 1L);

        CompleteRequest request = new CompleteRequest(
                List.of(),
                List.of(
                        new CompleteRequest.UnknownWord(
                                "example", "예시",
                                "This is an example.", "이것은 예시입니다.",
                                8, 7
                        )
                )
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(bookRepository.findById(1L)).willReturn(Optional.of(book));
        given(quizRepository.findAllById(any())).willReturn(List.of());
        given(sentenceRepository.countByBookId(1L)).willReturn(50);

        // When
        learningService.complete(1L, 1L, request);

        // Then
        verify(unknownWordRepository).save(any());
        verify(userRepository).save(user);
    }
}
