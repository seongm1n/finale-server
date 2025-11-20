package com.finale.finale.book.repository;

import com.finale.finale.book.domain.Book;
import com.finale.finale.book.domain.Sentence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SentenceRepository extends JpaRepository<Sentence, Long> {

    int countByBookId(Long id);

    List<Sentence> findAllByBook(Book book);

    void deleteAllByBook(Book book);
}
