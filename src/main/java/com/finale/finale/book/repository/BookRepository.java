package com.finale.finale.book.repository;

import com.finale.finale.auth.domain.User;
import com.finale.finale.book.domain.Book;
import com.finale.finale.book.domain.BookCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("SELECT b FROM Book b LEFT JOIN FETCH b.reviewWords WHERE b.user = :user AND b.isProvision = false ORDER BY b.createdAt ASC LIMIT 1")
    Optional<Book> findFirstByUserAndIsProvisionFalseWithReviewWords(@Param("user") User user);

    int countByUserAndIsProvisionFalse(User user);

    List<Book> findAllByUser(User user);

    @Query("SELECT b FROM Book b " +
    "WHERE b.user = :user " +
    "AND b.isCompleted = true " +
    "AND (:category IS NULL OR b.category = :category) " +
    "AND (:bookmarked IS NULL OR b.isBookmarked = :bookmarked)")
    Page<Book> findCompletedBooks(
            @Param("user") User user,
            @Param("category") BookCategory category,
            @Param("bookmarked") Boolean bookmarked,
            Pageable pageable
    );
}
