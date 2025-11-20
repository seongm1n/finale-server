package com.finale.finale.book.repository;

import com.finale.finale.book.domain.Book;
import com.finale.finale.book.domain.UnknownPhrase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface UnknownPhraseRepository extends JpaRepository<UnknownPhrase, Long> {

    List<UnknownPhrase> findTop5ByUser_IdAndNextReviewDateLessThanEqualOrderByNextReviewDateAsc(Long userId, LocalDate today);

    List<UnknownPhrase> findAllByBookIdIn(List<Long> bookIds);

    List<UnknownPhrase> findAllByBook(Book book);

    void deleteAllByUserId(Long userId);

    void deleteAllByBook(Book book);
}
