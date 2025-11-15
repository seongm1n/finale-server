package com.finale.finale.book.repository;

import com.finale.finale.book.domain.UnknownWord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface UnknownWordRepository extends JpaRepository<UnknownWord, Long> {

    List<UnknownWord> findTop10ByUser_IdAndNextReviewDateLessThanEqualOrderByNextReviewDateAsc(Long userId, LocalDate today);

    List<UnknownWord> findAllByBookIdIn(List<Long> bookIds);

    void deleteAllByUserId(Long userId);
}
