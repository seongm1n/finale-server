package com.finale.finale.book.domain;

import com.finale.finale.auth.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "unknown_words")
@Getter
@NoArgsConstructor
public class UnknownWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private String word;

    @Column(name = "word_meaning", nullable = false)
    private String wordMeaning;

    @Column(nullable = false)
    private String sentence;

    @Column(name = "sentence_meaning", nullable = false)
    private String sentenceMeaning;

    @Column(nullable = false)
    private Integer location;

    @Column(nullable = false)
    private Integer length;

    @Column(name = "next_review_date", nullable = false)
    private LocalDate nextReviewDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public UnknownWord(User user, Book book, String word, String wordMeaning, String sentence, String sentenceMeaning, int location, int length, LocalDate nextReviewDate) {
        this.user = user;
        this.book = book;
        this.word = word;
        this.wordMeaning = wordMeaning;
        this.sentence = sentence;
        this.sentenceMeaning = sentenceMeaning;
        this.location = location;
        this.length = length;
        this.nextReviewDate = nextReviewDate;
    }
}
