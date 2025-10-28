package com.finale.finale.book.repository;

import com.finale.finale.book.domain.Sentence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SentenceRepository extends JpaRepository<Sentence, Long> {
    int countByBookId(Long id);
}
