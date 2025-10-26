package com.finale.finale.auth.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Setter
    @Column(length = 100)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Role role = Role.USER;

    @Column(name = "ability_score", nullable = false)
    private Integer abilityScore = 0;

    @Column(name = "effort_score", nullable = false)
    private Integer effortScore = 0;

    @Column(name = "book_read_count", nullable = false)
    private Integer bookReadCount = 0;

    @Column(name = "total_sentences_read", nullable = false)
    private Integer totalSentencesRead = 0;

    @Column(name = "unknown_words_count", nullable = false)
    private Integer unknownWordsCount = 0;

    @Column(name = "today_books_created_count", nullable = false)
    private Integer todayBooksCreatedCount = 0;

    @Column(name = "last_book_created_date")
    private LocalDate lastBookCreatedDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public User(String email) {
        this.email = email;
    }

    public boolean needsNickname() {
        return this.nickname == null;
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
