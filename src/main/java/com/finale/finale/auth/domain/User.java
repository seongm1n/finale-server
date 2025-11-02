package com.finale.finale.auth.domain;

import com.finale.finale.exception.CustomException;
import com.finale.finale.exception.ErrorCode;
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
    private Integer abilityScore = 500;

    @Column(name = "book_read_count", nullable = false)
    private Integer bookReadCount = 0;

    @Column(name = "total_sentences_read", nullable = false)
    private Integer totalSentencesRead = 0;

    @Column(name = "unknown_words_count", nullable = false)
    private Integer unknownWordsCount = 0;

    @Column(name = "today_books_read_count", nullable = false)
    private Integer todayBooksReadCount = 0;

    @Column(name = "today_sentences_read_count", nullable = false)
    private Integer todaySentencesReadCount = 0;

    @Column(name = "continuos_learning", nullable = false)
    private Integer continuosLearning = 0;

    @Column(name = "last_learn_date", nullable = false)
    private LocalDate lastLearnDate = LocalDate.now().minusDays(1);

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

    public void initAbilityScore(int abilityScore) {
        if (this.abilityScore != 500) {
            throw new CustomException(ErrorCode.ABILITY_ALREADY_INITIALIZED);
        }
        this.abilityScore = abilityScore;
    }

    public void inclusionScore(int incorrectAnswersCount, int unknownWordCount, int totalWordCount) {
        int maxLScore = 1000;
        int minLScore = 0;

        double targetUnknownWordsRate = 0.03;
        double plusNormalizationFactor = 9.0;
        double minusNormalizationFactor = 49.0;
        double minusMaxRate = 0.10;
        double changeStep = 100.0;
        double rate = (totalWordCount > 0) ? ( ((double) unknownWordCount + (double) incorrectAnswersCount * 3) / (double) totalWordCount) : 0.0;
        double adjusted = Math.min(rate, minusMaxRate);
        double deviation = (adjusted - targetUnknownWordsRate) * 100.0;
        double normalizedSq = (deviation >= 0.0)
                ? -(deviation * deviation / minusNormalizationFactor)
                :  (deviation * deviation / plusNormalizationFactor);

        int next = (int) Math.round(this.abilityScore + changeStep * normalizedSq);
        this.abilityScore = Math.min(maxLScore, Math.max(minLScore, next));
    }

    public void addTotalSentences(int count) {
        totalSentencesRead += count;
    }

    public void addUnknownWords(int count) {
        unknownWordsCount += count;
    }

    public void increaseBookReadCount() {
        this.bookReadCount += 1;
    }

    public void learningStatusToday(int sentencesCount) {
        LocalDate today = LocalDate.now();

        if (lastLearnDate.isEqual(today)) {
            todayBooksReadCount++;
            todaySentencesReadCount += sentencesCount;
            return;
        }

        if (lastLearnDate.isEqual(today.minusDays(1))) {
            continuosLearning++;
            todayBooksReadCount = 1;
            todaySentencesReadCount = sentencesCount;
            lastLearnDate = today;
            return;
        }

        continuosLearning = 1;
        todayBooksReadCount = 1;
        todaySentencesReadCount = sentencesCount;
        lastLearnDate = today;
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
