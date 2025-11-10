package com.finale.finale.book.repository;

import com.finale.finale.book.domain.Phrase;
import com.finale.finale.book.domain.Sentence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PhraseRepository extends JpaRepository<Phrase, Long> {

    List<Phrase> findAllBySentence(Sentence sentence);

    @Query("SELECT p FROM Phrase p LEFT JOIN FETCH p.expression WHERE p.sentence IN :sentences")
    List<Phrase> findAllBySentenceIn(@Param("sentences") List<Sentence> sentences);

    void deleteAllBySentence(Sentence sentence);
}
