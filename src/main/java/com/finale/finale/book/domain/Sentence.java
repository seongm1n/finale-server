package com.finale.finale.book.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sentences")
@Getter
@NoArgsConstructor
public class Sentence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "paragraph_number", nullable = false)
    private Integer paragraphNumber;

    @Column(name = "sentence_order", nullable = false)
    private Integer sentenceOrder;

    @Column(name = "english_text", nullable = false)
    private String englishText;

    @Column(name = "korean_text", nullable = false)
    private String koreanText;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Sentence(Book book, int paragraphNumber, int sentenceOrder, String englishText, String koreanText) {
        this.book = book;
        this.paragraphNumber = paragraphNumber;
        this.sentenceOrder = sentenceOrder;
        this.englishText = englishText;
        this.koreanText = koreanText;
    }
}
