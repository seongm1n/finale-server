package com.finale.finale.book.controller;

import com.finale.finale.book.dto.BookRequest;
import com.finale.finale.book.dto.BookResponse;
import com.finale.finale.book.service.BookService;
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

    private final BookService bookService;

    @PostMapping("/generate")
    public ResponseEntity<BookResponse> generate(@RequestBody BookRequest request) {
        System.out.println(request);
        BookResponse response = bookService.generate(request);
        return ResponseEntity.ok(response);
    }
}
