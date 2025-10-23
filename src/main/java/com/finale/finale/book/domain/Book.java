package com.finale.finale.book.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "books")
@Getter
@NoArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 50)
    private String category;

    @Column(name = "ability_score", nullable = false)
    private Integer abilityScore;

    @Column(name = "total_word_count", nullable = false)
    private Integer totalWordCount;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Book(Long userId, String title, String category, Integer abilityScore, Integer totalWordCount) {
        this.userId = userId;
        this.title = title;
        this.category = category;
        this.abilityScore = abilityScore;
        this.totalWordCount = totalWordCount;
    }

    public void markAsCompleted() {
        this.isCompleted = true;
    }
}
