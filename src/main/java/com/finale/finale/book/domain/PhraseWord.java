package com.finale.finale.book.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "phrase_words")
@Getter
@NoArgsConstructor
public class PhraseWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "phrase_id")
    private Phrase phrase;

    @Column(nullable = false)
    private String word;

    @Column(nullable = false)
    private Integer location;

    @Column(nullable = false)
    private Integer length;

    public PhraseWord(Phrase phrase, String word, Integer location) {
        this.phrase = phrase;
        this.word = word;
        this.location = location;
        this.length = word.length();
    }
}
