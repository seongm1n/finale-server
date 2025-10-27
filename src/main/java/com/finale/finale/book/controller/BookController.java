package com.finale.finale.book.controller;

import com.finale.finale.book.dto.StoryGenerationRequest;
import com.finale.finale.book.dto.StoryGenerationResponse;
import com.finale.finale.book.service.StoryGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final StoryGenerationService storyGenerationService;

    @PostMapping("/generate")
    public ResponseEntity<StoryGenerationResponse> generate(
            @AuthenticationPrincipal Long userId,
            @RequestBody StoryGenerationRequest request
    ) {
        StoryGenerationResponse response = storyGenerationService.generate(request, userId);
        return ResponseEntity.ok(response);
    }
}
