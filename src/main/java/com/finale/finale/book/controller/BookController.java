package com.finale.finale.book.controller;

import com.finale.finale.book.dto.request.CompleteRequest;
import com.finale.finale.book.dto.response.CompleteResponse;
import com.finale.finale.book.dto.response.StoryGenerationResponse;
import com.finale.finale.book.service.BookService;
import com.finale.finale.book.service.LearningService;
import com.finale.finale.book.service.StoryGenerationService;
import com.finale.finale.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final StoryGenerationService storyGenerationService;
    private final BookService bookService;
    private final LearningService learningService;

    @GetMapping("/generate")
    public ResponseEntity<String> generate(
            @AuthenticationPrincipal Long userId
    ) {
        storyGenerationService.generate(userId);
        return ResponseEntity.ok("okay");
    }

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
}
