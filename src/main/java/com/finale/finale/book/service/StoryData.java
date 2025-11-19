package com.finale.finale.book.service;

import com.finale.finale.book.domain.Phrase;
import com.finale.finale.book.domain.Sentence;
import com.finale.finale.book.domain.UnknownPhrase;
import com.finale.finale.book.domain.UnknownWord;
import com.finale.finale.book.domain.Word;
import com.finale.finale.book.domain.Quiz;

import java.util.List;
import java.util.Map;

public record StoryData(
        List<Sentence> sentences,
        Map<Long, List<Word>> wordsBySentence,
        Map<Long, List<Phrase>> phrasesBySentence,
        List<Quiz> quizzes,
        List<UnknownWord> unknownWords,
        List<UnknownPhrase> unknownPhrases
) {
}

