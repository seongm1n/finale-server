package com.finale.finale.book.domain;

import com.finale.finale.auth.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UnknownWord 엔티티 테스트")
public class UnknownWordTest {

    @Test
    @DisplayName("복습 설정 - 첫 복습")
    void nextReviewSettingFirstReview() {
        // Given
        User user = new User("test@example.com");
        Book book = new Book(user, "Test Book", BookCategory.ADVENTURE, 500, 600);
        UnknownWord unknownWord = new UnknownWord(
                user, book, "example", "예시",
                "This is an example.", "이것은 예시입니다.",
                1L, 8, 7, LocalDate.now()
        );

        // When
        unknownWord.nextReviewSetting();

        // Then
        assertThat(unknownWord.getReviewCount()).isEqualTo(1);
        assertThat(unknownWord.getNextReviewDate()).isAfter(LocalDate.now());
    }

    @Test
    @DisplayName("복습 설정 - 여러 번 복습")
    void nextReviewSettingMultipleTimes() {
        // Given
        User user = new User("test@example.com");
        Book book = new Book(user, "Test Book", BookCategory.ADVENTURE, 500, 600);
        UnknownWord word = new UnknownWord(
                user, book, "example", "예시",
                "This is an example.", "이것은 예시입니다.",
                1L, 8, 7, LocalDate.now()
        );

        // When
        IntStream.range(0, 5).forEach(i -> word.nextReviewSetting());

        // Then
        assertThat(word.getReviewCount()).isEqualTo(5);
        assertThat(word.getNextReviewDate()).isAfter(LocalDate.now());
    }
}
