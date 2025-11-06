package com.finale.finale.book.service;

import com.finale.finale.auth.domain.User;
import com.finale.finale.auth.repository.UserRepository;
import com.finale.finale.book.domain.Book;
import com.finale.finale.book.domain.BookCategory;
import com.finale.finale.book.dto.response.StoryGenerationResponse;
import com.finale.finale.book.repository.BookRepository;
import com.finale.finale.book.repository.QuizRepository;
import com.finale.finale.book.repository.SentenceRepository;
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
import static org.mockito.BDDMockito.given;

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
        given(bookRepository.findFirstByUserAndIsProvisionFalseOrderByCreatedAtAsc(user))
                .willReturn(Optional.of(book));
        given(sentenceRepository.findAllByBook(book)).willReturn(List.of());
        given(quizRepository.findAllByBook(book)).willReturn(List.of());

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
        given(bookRepository.findFirstByUserAndIsProvisionFalseOrderByCreatedAtAsc(user))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookService.getNewStory(userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOK_NOT_READY);
    }
}
