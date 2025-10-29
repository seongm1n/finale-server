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

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;

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

    public void nextReviewSetting() {
        int[] intervals = {1, 3, 7, 14, 30, 75, 188, 469, 1173, 2933};
        java.util.Random random = new java.util.Random();

        int index = Math.max(0, reviewCount);
        int safeIndex = Math.min(index, intervals.length - 1);

        int base = intervals[safeIndex];
        int range = (int) Math.round(base * 0.1);
        int offset = range > 0 ? random.nextInt(2 * range + 1) - range : 0;

        int days = Math.max(base + offset, 0);

        this.nextReviewDate = LocalDate.now().plusDays(days);
        this.reviewCount = reviewCount + 1;
    }
}
