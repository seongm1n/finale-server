package com.finale.finale.book.repository;

import com.finale.finale.auth.domain.User;
import com.finale.finale.book.domain.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findFirstByUserAndIsProvisionFalseOrderByCreatedAtAsc(User user);

    int countByUserAndIsProvisionFalse(User user);
}
