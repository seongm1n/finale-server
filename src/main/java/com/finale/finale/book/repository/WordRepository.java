package com.finale.finale.book.repository;

import com.finale.finale.book.domain.Sentence;
import com.finale.finale.book.domain.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WordRepository extends JpaRepository<Word, Long> {

    List<Word> findAllBySentence(Sentence sentence);

    @Query("SELECT w FROM Word w WHERE w.sentence IN :sentences")
    List<Word> findAllBySentenceIn(@Param("sentences") List<Sentence> sentences);

    void deleteAllBySentence(Sentence sentence);
}
