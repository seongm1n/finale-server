package com.finale.finale.book.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "phrases")
@Getter
@NoArgsConstructor
public class Phrase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sentence_id", nullable = false)
    private Sentence sentence;

    @Column(name = "meaning", nullable = false)
    private String meaning;

    @OneToMany(mappedBy = "phrase", cascade = CascadeType.ALL)
    private List<PhraseWord> expression = new ArrayList<>();

    public Phrase(Sentence sentence, String meaning) {
        this.sentence = sentence;
        this.meaning = meaning;
    }

    public void addPhraseWord(PhraseWord phraseWord) {
        expression.add(phraseWord);
    }
}
