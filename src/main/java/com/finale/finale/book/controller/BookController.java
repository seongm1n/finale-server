package com.finale.finale.book.controller;

import com.finale.finale.book.domain.BookCategory;
import com.finale.finale.book.dto.request.CompleteRequest;
import com.finale.finale.book.dto.response.BookmarkResponse;
import com.finale.finale.book.dto.response.CompleteResponse;
import com.finale.finale.book.dto.response.CompletedBooksResponse;
import com.finale.finale.book.dto.response.StoryGenerationResponse;
import com.finale.finale.book.service.BookService;
import com.finale.finale.book.service.LearningService;
import com.finale.finale.book.service.StoryGenerationService;
import com.finale.finale.exception.CustomException;
import com.finale.finale.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final StoryGenerationService storyGenerationService;
    private final BookService bookService;
    private final LearningService learningService;

    @PostMapping("/{bookId}/complete")
    public ResponseEntity<CompleteResponse> complete(
            @Valid
            @AuthenticationPrincipal Long userId,
            @PathVariable Long bookId,
            @RequestBody CompleteRequest request
    ) {
        CompleteResponse response = learningService.complete(userId, bookId, request);
        storyGenerationService.generate(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/new")
    public ResponseEntity<StoryGenerationResponse> getNew(@AuthenticationPrincipal Long userId) {
        try {
            StoryGenerationResponse response = bookService.getNewStory(userId);
            return ResponseEntity.ok(response);
        } catch (CustomException e) {
            storyGenerationService.generate(userId);
            throw e;
        }
    }

    @PostMapping("/{bookId}/bookmark")
    public ResponseEntity<BookmarkResponse> toggleBookmark(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long bookId
    ) {
        BookmarkResponse response = bookService.toggleBookmark(userId, bookId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/completed")
    public ResponseEntity<CompletedBooksResponse> getCompletedBooks(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean bookmarked
    ) {
        validateSortParameter(sort);
        validateCategoryParameter(category);

        CompletedBooksResponse response = bookService.getCompletedBooks(
                userId, page, size, sort, category, bookmarked
        );
        return ResponseEntity.ok(response);
    }

    private void validateSortParameter(String sort) {
        if (!sort.equals("latest") && !sort.equals("oldest")) {
            throw new CustomException(ErrorCode.INVALID_SORT_PARAMETER);
        }
    }

    private void validateCategoryParameter(String category) {
        if (category == null) {
            return;
        }
        try {
            BookCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_CATEGORY_PARAMETER);
        }
    }
}
