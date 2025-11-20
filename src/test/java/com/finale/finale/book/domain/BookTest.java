package com.finale.finale.book.domain;

import com.finale.finale.auth.domain.User;
import com.finale.finale.exception.CustomException;
import com.finale.finale.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Book 엔티티 테스트")
public class BookTest {

    @Test
    @DisplayName("책 완료 처리")
    void markAsCompleted() {
        // Given
        User user = new User("test@example.com");
        Book book = new Book(user, "Test Book", BookCategory.ADVENTURE, 500, 600);

        // When
        book.markAsCompleted();

        // Then
        assertThat(book.getIsCompleted()).isTrue();
    }

    @Test
    @DisplayName("소유자 검증 - 성공")
    void validateOwnerSuccess() {
        // Given
        User user = new User("test@example.com");
        Book book = new Book(user, "Test Book", BookCategory.ADVENTURE, 500, 600);

        // When & Then
        assertThatNoException().isThrownBy(() -> book.validateOwner(user));
    }

    @Test
    @DisplayName("소유자 검증 - 다른 사용자")
    void validateOwnerUserMismatch() {
        // Given
        User owner = new User("owner@example.com");
        User other = new User("other@example.com");
        Book book = new Book(owner, "Test Book", BookCategory.ADVENTURE, 500, 600);

        // When & Then
        assertThatThrownBy(() -> book.validateOwner(other))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOK_USER_MISMATCH);
    }

    @Test
    @DisplayName("완료 검증 - 성공")
    void validateCompletedSuccess() {
        // Given
        User user = new User("test@example.com");
        Book book = new Book(user, "Test Book", BookCategory.ADVENTURE, 500, 600);
        book.markAsCompleted();

        // When & Then
        assertThatNoException().isThrownBy(() -> book.validateCompleted());
    }

    @Test
    @DisplayName("완료 검증 - 완료되지 않은 책")
    void validateCompletedNotCompleted() {
        // Given
        User user = new User("test@example.com");
        Book book = new Book(user, "Test Book", BookCategory.ADVENTURE, 500, 600);

        // When & Then
        assertThatThrownBy(() -> book.validateCompleted())
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOK_NOT_COMPLETED);
    }

    @Test
    @DisplayName("미완료 검증 - 성공")
    void validateNotCompletedSuccess() {
        // Given
        User user = new User("test@example.com");
        Book book = new Book(user, "Test Book", BookCategory.ADVENTURE, 500, 600);

        // When & Then
        assertThatNoException().isThrownBy(() -> book.validateNotCompleted());
    }

    @Test
    @DisplayName("미완료 검증 - 이미 완료된 책")
    void validateNotCompletedAlreadyCompleted() {
        // Given
        User user = new User("test@example.com");
        Book book = new Book(user, "Test Book", BookCategory.ADVENTURE, 500, 600);
        book.markAsCompleted();

        // When & Then
        assertThatThrownBy(() -> book.validateNotCompleted())
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOK_ALREADY_COMPLETED);
    }
}
