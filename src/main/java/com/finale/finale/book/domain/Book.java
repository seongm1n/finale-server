package com.finale.finale.book.domain;

import com.finale.finale.auth.domain.User;
import com.finale.finale.exception.CustomException;
import com.finale.finale.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "books")
@Getter
@NoArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private BookCategory category;

    @Column(name = "ability_score", nullable = false)
    private Integer abilityScore;

    @Column(name = "total_word_count", nullable = false)
    private Integer totalWordCount;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @Column(name = "is_provision", nullable = false)
    private Boolean isProvision = false;

    @Column(name = "is_bookmarked", nullable = false)
    private Boolean isBookmarked = false;

    @ManyToMany
    @JoinTable(
            name = "book_review_words",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "unknown_word_id")
    )
    private List<UnknownWord> reviewWords = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "book_review_phrases",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "unknown_phrase_id")
    )
    private List<UnknownPhrase> reviewPhrases = new ArrayList<>();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Book(User user, String title, BookCategory category, Integer abilityScore, Integer totalWordCount) {
        this.user = user;
        this.title = title;
        this.category = category;
        this.abilityScore = abilityScore;
        this.totalWordCount = totalWordCount;
    }

    public void markAsCompleted() {
        this.isCompleted = true;
    }

    public void markAsProvision() {
        this.isProvision = true;
    }

    public void addReviewWord(List<UnknownWord> unknownWords) {
        this.reviewWords.addAll(unknownWords);
    }

    public void addReviewPhrase(List<UnknownPhrase> unknownPhrases) {
        this.reviewPhrases.addAll(unknownPhrases);
    }

    public void toggleIsBookmarked() {
        this.isBookmarked = !this.isBookmarked;
    }

    public void validateOwner(User user) {
        if (!this.user.equals(user)) {
            throw new CustomException(ErrorCode.BOOK_USER_MISMATCH);
        }
    }

    public void validateCompleted() {
        if (!isCompleted) {
            throw new CustomException(ErrorCode.BOOK_NOT_COMPLETED);
        }
    }

    public void validateNotCompleted() {
        if (isCompleted) {
            throw new CustomException(ErrorCode.BOOK_ALREADY_COMPLETED);
        }
    }

    public void setCompletedAt() {
        this.completedAt = LocalDateTime.now();
    }
}
