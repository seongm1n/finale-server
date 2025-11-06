package com.finale.finale.book.repository;

import com.finale.finale.book.domain.Book;
import com.finale.finale.book.domain.UnknownWord;
import com.finale.finale.book.dto.response.UnknownWordResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface UnknownWordRepository extends JpaRepository<UnknownWord, Long> {

    @Query("SELECT uw FROM UnknownWord uw WHERE uw.user.id = :userId AND uw.nextReviewDate <= :today ORDER BY uw.nextReviewDate ASC LIMIT 10")
    List<UnknownWord> findTop10ByUserIdAndNextReviewDateBeforeOrEqual(@Param("userId") Long userId, @Param("today") LocalDate today);

    List<UnknownWord> findAllByBook(Book book);
}
