package com.finale.finale.book.repository;

import com.finale.finale.book.domain.UnknownWord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnknownWordRepository extends JpaRepository<UnknownWord, Integer> {
}
