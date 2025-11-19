package com.finale.finale.book.service;

import com.finale.finale.book.domain.Book;
import com.finale.finale.book.domain.Quiz;
import com.finale.finale.book.domain.Sentence;
import com.finale.finale.book.domain.UnknownPhrase;
import com.finale.finale.book.domain.UnknownWord;
import com.finale.finale.book.dto.response.CompletedBookDetailResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class CompletedBookDetailAssembler {

    public CompletedBookDetailResponse toResponse(
            Book book,
            List<Sentence> sentences,
            List<Quiz> quizzes,
            List<UnknownWord> unknownWords,
            List<UnknownPhrase> unknownPhrases
    ) {
        return new CompletedBookDetailResponse(
                book.getId(),
                book.getTitle(),
                book.getCategory().getValue(),
                book.getAbilityScore(),
                book.getCreatedAt(), // TODO: completedAt 필드로 수정 필요
                buildSentenceResponses(sentences),
                buildQuizResponses(quizzes),
                buildUnknownWordResponses(unknownWords),
                buildUnknownPhraseResponses(unknownPhrases)
        );
    }

    private List<CompletedBookDetailResponse.SentenceResponse> buildSentenceResponses(List<Sentence> sentences) {
        return sentences.stream()
                .map(sentence -> new CompletedBookDetailResponse.SentenceResponse(
                        sentence.getId(),
                        sentence.getParagraphNumber(),
                        sentence.getSentenceOrder(),
                        sentence.getEnglishText(),
                        sentence.getKoreanText()
                ))
                .toList();
    }

    private List<CompletedBookDetailResponse.QuizResponse> buildQuizResponses(List<Quiz> quizzes) {
        return quizzes.stream()
                .map(quiz -> new CompletedBookDetailResponse.QuizResponse(
                        quiz.getId(),
                        quiz.getQuestion(),
                        quiz.getCorrectAnswer(),
                        quiz.getUserAnswer(),
                        Objects.equals(quiz.getCorrectAnswer(), quiz.getUserAnswer())
                ))
                .toList();
    }

    private List<CompletedBookDetailResponse.UnknownWordResponse> buildUnknownWordResponses(List<UnknownWord> unknownWords) {
        return unknownWords.stream()
                .map(word -> new CompletedBookDetailResponse.UnknownWordResponse(
                        word.getId(),
                        word.getWord(),
                        word.getWordMeaning(),
                        word.getSentenceId(),
                        word.getSentence(),
                        word.getSentenceMeaning(),
                        word.getLocation(),
                        word.getLength(),
                        word.getNextReviewDate(),
                        word.getCreatedAt()
                ))
                .toList();
    }

    private List<CompletedBookDetailResponse.UnknownPhraseResponse> buildUnknownPhraseResponses(List<UnknownPhrase> unknownPhrases) {
        return unknownPhrases.stream()
                .map(phrase -> new CompletedBookDetailResponse.UnknownPhraseResponse(
                        phrase.getId(),
                        phrase.getPhrase(),
                        phrase.getPhraseMeaning(),
                        phrase.getSentenceId(),
                        phrase.getSentence(),
                        phrase.getSentenceMeaning(),
                        phrase.getWords().stream()
                                .map(w -> new CompletedBookDetailResponse.PhraseWordResponse(
                                        w.getWord(),
                                        w.getLocation(),
                                        w.getLength()
                                ))
                                .toList(),
                        phrase.getNextReviewDate(),
                        phrase.getCreatedAt()
                ))
                .toList();
    }
}

