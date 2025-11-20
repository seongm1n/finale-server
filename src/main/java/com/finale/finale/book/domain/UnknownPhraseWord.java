package com.finale.finale.book.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "unknown_phrase_words")
@Getter
@NoArgsConstructor
public class UnknownPhraseWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unknown_phrase_id", nullable = false)
    private UnknownPhrase unknownPhrase;

    @Column(nullable = false)
    private String word;

    @Column(nullable = false)
    private Integer location;

    @Column(nullable = false)
    private Integer length;

    public UnknownPhraseWord(String word, Integer location, Integer length) {
        this.word = word;
        this.location = location;
        this.length = length;
    }

    public void setUnknownPhrase(UnknownPhrase unknownPhrase) {
        this.unknownPhrase = unknownPhrase;
    }
}
