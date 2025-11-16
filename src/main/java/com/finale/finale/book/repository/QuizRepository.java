package com.finale.finale.book.repository;

import com.finale.finale.book.domain.Book;
import com.finale.finale.book.domain.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findAllByBook(Book book);

    void deleteAllByBook(Book book);
}
