package com.finale.finale.book.controller;

import com.finale.finale.book.dto.request.CompleteRequest;
import com.finale.finale.book.dto.request.StoryGenerationRequest;
import com.finale.finale.book.dto.response.CompleteResponse;
import com.finale.finale.book.dto.response.StoryGenerationResponse;
import com.finale.finale.book.service.LearningService;
import com.finale.finale.book.service.StoryGenerationService;
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
    private final LearningService learningService;

    @PostMapping("/generate")
    public ResponseEntity<StoryGenerationResponse> generate(
            @Valid
            @AuthenticationPrincipal Long userId,
            @RequestBody StoryGenerationRequest request
    ) {
        StoryGenerationResponse response = storyGenerationService.generate(request, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{bookId}/complete")
    public ResponseEntity<CompleteResponse> complete(
            @Valid
            @AuthenticationPrincipal Long userId,
            @PathVariable Long bookId,
            @RequestBody CompleteRequest request
    ) {
        CompleteResponse response = learningService.complete(userId, bookId, request);
        return ResponseEntity.ok(response);
    }
}
