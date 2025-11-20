package com.finale.finale.book.domain;

import com.finale.finale.auth.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "unknown_phrases")
@Getter
@NoArgsConstructor
public class UnknownPhrase extends ReviewableItem {

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
    private String phrase;

    @Column(name = "phrase_meaning", nullable = false)
    private String phraseMeaning;

    @Column(nullable = false)
    private String sentence;

    @Column(name = "sentence_meaning", nullable = false)
    private String sentenceMeaning;

    @Column(name = "sentence_id", nullable = false)
    private Long sentenceId;

    @OneToMany(mappedBy = "unknownPhrase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UnknownPhraseWord> words = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public UnknownPhrase(User user, Book book, String phrase, String phraseMeaning, String sentence, String sentenceMeaning, Long sentenceId, LocalDate nextReviewDate) {
        this.user = user;
        this.book = book;
        this.phrase = phrase;
        this.phraseMeaning = phraseMeaning;
        this.sentence = sentence;
        this.sentenceMeaning = sentenceMeaning;
        this.sentenceId = sentenceId;
        setNextReviewDate(nextReviewDate);
    }

    public void addWords(List<UnknownPhraseWord> words) {
        this.words = words;
        words.forEach(w -> w.setUnknownPhrase(this));
    }
}
