package com.finale.finale.book.domain;

import com.finale.finale.exception.CustomException;
import com.finale.finale.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "quizzes")
@Getter
@NoArgsConstructor
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private String question;

    @Column(name = "correct_answer", nullable = false)
    private Boolean correctAnswer;

    @Column(name = "user_answer")
    private Boolean userAnswer;

    @Column(name = "is_solved")
    private Boolean isSolved = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Quiz(Book book, String question, Boolean correctAnswer) {
        this.book = book;
        this.question = question;
        this.correctAnswer = correctAnswer;
    }

    public void answerQuiz(boolean answer) {
        this.userAnswer = answer;
        this.isSolved = true;
    }

    public void validateMatch(Book book) {
        if (!this.book.equals(book)) {
            throw new CustomException(ErrorCode.QUIZ_BOOK_MISMATCH);
        }
    }
}
