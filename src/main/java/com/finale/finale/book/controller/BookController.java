package com.finale.finale.book.controller;

import com.finale.finale.book.dto.StoryGenerationRequest;
import com.finale.finale.book.dto.StoryGenerationResponse;
import com.finale.finale.book.service.StoryGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<StoryGenerationResponse> generate(@RequestBody StoryGenerationRequest request) {
        System.out.println(request);
        StoryGenerationResponse response = storyGenerationService.generate(request);
        return ResponseEntity.ok(response);
    }
}
