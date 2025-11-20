package com.finale.finale.book.service;

import com.finale.finale.book.domain.Phrase;
import com.finale.finale.book.domain.PhraseWord;
import com.finale.finale.book.domain.Sentence;
import com.finale.finale.book.domain.Word;
import com.finale.finale.book.dto.response.StoryGenerationResponse;
import com.finale.finale.book.dto.response.StoryGenerationResponse.PhraseResponse;
import com.finale.finale.book.dto.response.StoryGenerationResponse.SentenceResponse;
import com.finale.finale.book.dto.response.StoryGenerationResponse.WordResponse;
import com.finale.finale.book.dto.response.StoryGenerationResponse.ExpressionResponse;
import com.finale.finale.book.dto.response.StoryGenerationResponse.PhraseWordResponse;
import com.finale.finale.book.dto.response.StoryGenerationResponse.QuizResponse;
import com.finale.finale.book.dto.response.StoryGenerationResponse.UnknownWordResponse;
import com.finale.finale.book.dto.response.StoryGenerationResponse.UnknownPhraseResponse;
import com.finale.finale.book.domain.Book;
import com.finale.finale.book.domain.Quiz;
import com.finale.finale.book.domain.UnknownPhrase;
import com.finale.finale.book.domain.UnknownWord;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StoryResponseAssembler {

    public StoryGenerationResponse toResponse(Book book, StoryData data) {
        List<SentenceResponse> sentences = buildSentenceResponses(data.sentences(), data);
        List<QuizResponse> quizzes = buildQuizResponses(data.quizzes());
        List<UnknownWordResponse> unknownWords = buildUnknownWordResponses(data.unknownWords());
        List<UnknownPhraseResponse> unknownPhrases = buildUnknownPhraseResponses(data.unknownPhrases());

        return new StoryGenerationResponse(
                book.getId(),
                book.getTitle(),
                book.getCategory().getValue(),
                book.getAbilityScore(),
                book.getTotalWordCount(),
                sentences,
                quizzes,
                unknownWords,
                unknownPhrases,
                book.getCreatedAt()
        );
    }

    private List<SentenceResponse> buildSentenceResponses(List<Sentence> sentences, StoryData data) {
        return sentences.stream()
                .map(sentence -> {
                    List<WordResponse> words = data.wordsBySentence()
                            .getOrDefault(sentence.getId(), List.of())
                            .stream()
                            .map(this::toWordResponse)
                            .toList();

                    List<PhraseResponse> phrases = data.phrasesBySentence()
                            .getOrDefault(sentence.getId(), List.of())
                            .stream()
                            .map(this::toPhraseResponse)
                            .toList();

                    return new SentenceResponse(
                            sentence.getId(),
                            sentence.getParagraphNumber(),
                            sentence.getSentenceOrder(),
                            sentence.getEnglishText(),
                            sentence.getKoreanText(),
                            words,
                            phrases
                    );
                })
                .toList();
    }

    private WordResponse toWordResponse(Word word) {
        return new WordResponse(
                word.getWord(),
                word.getMeaning(),
                word.getLocation(),
                word.getLength()
        );
    }

    private PhraseResponse toPhraseResponse(Phrase phrase) {
        List<ExpressionResponse> expressions = phrase.getExpression().stream()
                .map(this::toExpressionResponse)
                .toList();

        return new PhraseResponse(
                phrase.getMeaning(),
                expressions
        );
    }

    private ExpressionResponse toExpressionResponse(PhraseWord phraseWord) {
        return new ExpressionResponse(
                phraseWord.getWord(),
                phraseWord.getLocation(),
                phraseWord.getLength()
        );
    }

    private List<QuizResponse> buildQuizResponses(List<Quiz> quizzes) {
        return quizzes.stream()
                .map(quiz -> new QuizResponse(
                        quiz.getId(),
                        quiz.getQuestion(),
                        quiz.getCorrectAnswer()
                ))
                .toList();
    }

    private List<UnknownWordResponse> buildUnknownWordResponses(List<UnknownWord> unknownWords) {
        return unknownWords.stream()
                .map(unknownWord -> new UnknownWordResponse(
                        unknownWord.getWord(),
                        unknownWord.getWordMeaning(),
                        unknownWord.getSentence(),
                        unknownWord.getSentenceMeaning(),
                        unknownWord.getReviewCount()
                ))
                .toList();
    }

    private List<UnknownPhraseResponse> buildUnknownPhraseResponses(List<UnknownPhrase> unknownPhrases) {
        return unknownPhrases.stream()
                .map(phrase -> new UnknownPhraseResponse(
                        phrase.getPhrase(),
                        phrase.getPhraseMeaning(),
                        phrase.getSentence(),
                        phrase.getSentenceMeaning(),
                        phrase.getWords().stream()
                                .map(word -> new PhraseWordResponse(
                                        word.getWord(),
                                        word.getLocation(),
                                        word.getLength()
                                ))
                                .toList(),
                        phrase.getReviewCount()
                ))
                .toList();
    }
}

