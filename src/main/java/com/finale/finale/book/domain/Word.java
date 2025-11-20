package com.finale.finale.book.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Table(name = "words")
@Entity
@NoArgsConstructor
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sentence_id", nullable = false)
    private Sentence sentence;

    @Column(name = "word", nullable = false)
    private String word;

    @Column(name = "meaning", nullable = false)
    private String meaning;

    @Column(name = "location", nullable = false)
    private Integer location;

    @Column(name = "length", nullable = false)
    private Integer length;

    public Word(Sentence sentence, String word, String meaning, Integer location) {
        this.sentence = sentence;
        this.word = word;
        this.meaning = meaning;
        this.location = location;
        this.length = word.length();
    }
}
