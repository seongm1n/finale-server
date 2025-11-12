package com.finale.finale.book.service;

import com.finale.finale.auth.domain.User;
import com.finale.finale.auth.repository.UserRepository;
import com.finale.finale.book.domain.Book;
import com.finale.finale.book.domain.Phrase;
import com.finale.finale.book.domain.Sentence;
import com.finale.finale.book.domain.Word;
import com.finale.finale.book.dto.response.BookmarkResponse;
import com.finale.finale.book.dto.response.StoryGenerationResponse;
import com.finale.finale.book.dto.response.StoryGenerationResponse.QuizResponse;
import com.finale.finale.book.dto.response.StoryGenerationResponse.SentenceResponse;
import com.finale.finale.book.dto.response.StoryGenerationResponse.UnknownWordResponse;
import com.finale.finale.book.repository.*;
import com.finale.finale.exception.CustomException;
import com.finale.finale.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final SentenceRepository sentenceRepository;
    private final QuizRepository quizRepository;
    private final WordRepository wordRepository;
    private final PhraseRepository phraseRepository;
    private final PhraseWordRepository phraseWordRepository;

    @Transactional
    public StoryGenerationResponse getNewStory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Book book = bookRepository.findFirstByUserAndIsProvisionFalseWithReviewWords(user)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_READY));

        List<Sentence> sentenceEntities = sentenceRepository.findAllByBook(book);

        List<Word> allWords = wordRepository.findAllBySentenceIn(sentenceEntities);
        List<Phrase> allPhrases = phraseRepository.findAllBySentenceIn(sentenceEntities);

        Map<Long, List<Word>> wordsBySentence = allWords.stream()
                .collect(Collectors.groupingBy(w -> w.getSentence().getId()));
        Map<Long, List<Phrase>> phrasesBySentence = allPhrases.stream()
                .collect(Collectors.groupingBy(p -> p.getSentence().getId()));

        List<SentenceResponse> sentences = sentenceEntities.stream()
                .map(sentence -> {
                    List<StoryGenerationResponse.WordResponse> words =
                            wordsBySentence.getOrDefault(sentence.getId(), Collections.emptyList())
                                    .stream()
                                    .map(word -> new StoryGenerationResponse.WordResponse(
                                            word.getWord(),
                                            word.getMeaning(),
                                            word.getLocation(),
                                            word.getLength()
                                    ))
                                    .toList();

                    List<StoryGenerationResponse.PhraseResponse> phrases =
                            phrasesBySentence.getOrDefault(sentence.getId(), Collections.emptyList())
                                    .stream()
                                    .map(phrase -> {
                                        List<StoryGenerationResponse.ExpressionResponse> expressions = phrase.getExpression().stream()
                                                .map(phraseWord -> new StoryGenerationResponse.ExpressionResponse(
                                                        phraseWord.getWord(),
                                                        phraseWord.getLocation(),
                                                        phraseWord.getLength()
                                                ))
                                                .toList();

                                        return new StoryGenerationResponse.PhraseResponse(
                                                phrase.getMeaning(),
                                                expressions
                                        );
                                    })
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

        List<QuizResponse> quizzes = quizRepository.findAllByBook(book).stream()
                .map(quiz -> new QuizResponse(
                        quiz.getId(),
                        quiz.getQuestion(),
                        quiz.getCorrectAnswer()
                ))
                .toList();

        List<UnknownWordResponse> unknownWords = book.getReviewWords().stream()
                .map(unknownWord -> new UnknownWordResponse(
                        unknownWord.getWord(),
                        unknownWord.getWordMeaning(),
                        unknownWord.getSentence(),
                        unknownWord.getSentenceMeaning()
                ))
                .toList();

        book.markAsProvision();

        sentenceEntities.forEach(sentence -> {
            wordRepository.deleteAllBySentence(sentence);
            phraseRepository.deleteAllBySentence(sentence);
        });

        return new StoryGenerationResponse(
                book.getId(),
                book.getTitle(),
                book.getCategory().getValue(),
                book.getAbilityScore(),
                book.getTotalWordCount(),
                sentences,
                quizzes,
                unknownWords,
                book.getCreatedAt()
        );
    }

    @Transactional
    public BookmarkResponse toggleBookmark(Long userId, Long bookId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_FOUND));

        book.validateBookmarked(user);
        book.toggleIsBookmarked();

        return new BookmarkResponse(book.getId(), book.getIsBookmarked());
    }
}
