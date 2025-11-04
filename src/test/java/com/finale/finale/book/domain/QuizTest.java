package com.finale.finale.book.domain;

import com.finale.finale.auth.domain.User;
import com.finale.finale.exception.CustomException;
import com.finale.finale.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Quiz 엔티티 테스트")
public class QuizTest {

    @Test
    @DisplayName("퀴즈 답변 처리")
    void answerQuiz() {
        // Given
        User user = new User("test@example.com");
        Book book = new Book(user, "Test Book", "adventure", 500, 600);
        Quiz quiz = new Quiz(book, "Is this true?", true);

        // When
        quiz.answerQuiz(true);

        // Then
        assertThat(quiz.getUserAnswer()).isTrue();
        assertThat(quiz.getIsSolved()).isTrue();
    }

    @Test
    @DisplayName("퀴즈 검증 - 성공")
    void validateMatchSuccess() {
        // Given
        User user = new User("test@example.com");
        Book book = new Book(user, "Test Book", "adventure", 500, 600);
        Quiz quiz = new Quiz(book, "Is this true?", true);

        // When & Then
        assertThatNoException().isThrownBy(() -> quiz.validateMatch(book));
    }

    @Test
    @DisplayName("퀴즈 검증 - 다른 책")
    void validateMatchBookMismatch() {
        // Given
        User user = new User("test@example.com");
        Book book1 = new Book(user, "Test Book 1", "adventure", 500, 600);
        Book book2 = new Book(user, "Test Book 2", "adventure", 500, 600);
        Quiz quiz = new Quiz(book1, "Is this true?", true);

        // When & Then
        assertThatThrownBy(() -> quiz.validateMatch(book2))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_BOOK_MISMATCH);
    }
}
