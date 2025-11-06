package com.finale.finale.book.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BookCategory 테스트")
public class BookCategoryTest {

    @Test
    @DisplayName("random()은 null을 반환하지 않는다.")
    void randomReturnsNotNull() {
        // When
        BookCategory category = BookCategory.random();

        // Then
        assertThat(category).isNotNull();
    }

    @Test
    @DisplayName("random()을 여러 번 호출하면 다양한 카테고리가 나온다")
    void randomReturnsVariousCategories() {
        // Given
        Set<BookCategory> categories = new HashSet<>();

        // When
        for (int i = 0; i < 100; i++) {
            categories.add(BookCategory.random());
        }

        // Then
        assertThat(categories).hasSizeGreaterThanOrEqualTo(3);
    }
}
