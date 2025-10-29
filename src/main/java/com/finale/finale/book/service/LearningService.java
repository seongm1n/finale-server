package com.finale.finale.book.service;

import com.finale.finale.auth.domain.User;
import com.finale.finale.auth.repository.UserRepository;
import com.finale.finale.book.domain.Book;
import com.finale.finale.book.domain.Quiz;
import com.finale.finale.book.domain.UnknownWord;
import com.finale.finale.book.dto.request.CompleteRequest;
import com.finale.finale.book.dto.response.CompleteResponse;
import com.finale.finale.book.repository.BookRepository;
import com.finale.finale.book.repository.QuizRepository;
import com.finale.finale.book.repository.SentenceRepository;
import com.finale.finale.book.repository.UnknownWordRepository;
import com.finale.finale.exception.CustomException;
import com.finale.finale.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LearningService {
    private static final int UNKNOWN_WORD_FIRST_REVIEW_DAYS = 0;

    private final UnknownWordRepository unknownWordRepository;
    private final QuizRepository quizRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final SentenceRepository sentenceRepository;

    public CompleteResponse complete(Long userId, Long bookId, CompleteRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_FOUND));

        book.validateComplete(user);

        List<CompleteRequest.QuizAnswer> quizRequestList = request.quizAnswers();
        Map<Long, Quiz> quizMap = loadQuizMap(quizRequestList);
        int ansCount = processQuizzes(quizRequestList, quizMap);

        for (Quiz quiz : quizMap.values()) {
            quiz.validateMatch(book);
        }

        List<CompleteRequest.UnknownWord> unknownWordRequestList = request.unknownWords();
        int unknownWordCount = unknownWordRequestList.size();
        saveUnknownWord(user, book, unknownWordRequestList);

        int beforeAbilityScore = user.getAbilityScore();
        int beforeTodaySentencesRead = user.getTotalSentencesRead();
        int beforeContinuousLearning = user.getContinuosLearning();
        changeUserInformation(user, book, ansCount, unknownWordCount);
        book.markAsCompleted();
        bookRepository.save(book);

        return new CompleteResponse(
                bookId,
                book.getTitle(),
                LocalDateTime.now(),
                new CompleteResponse.QuizResult(
                        quizRequestList.size(),
                        ansCount
                ),
                new CompleteResponse.StatsChange(
                        new CompleteResponse.StatChange(
                                beforeAbilityScore,
                                user.getAbilityScore(),
                                user.getAbilityScore() - beforeAbilityScore
                        ),
                        new CompleteResponse.StatChange(
                                user.getTodayBooksReadCount() - 1,
                                user.getTodayBooksReadCount(),
                                1
                        ),
                        new CompleteResponse.StatChange(
                                beforeTodaySentencesRead,
                                user.getTodaySentencesReadCount(),
                                user.getTodaySentencesReadCount() - beforeTodaySentencesRead
                        ),
                        new CompleteResponse.StatChange(
                                beforeContinuousLearning,
                                user.getContinuosLearning(),
                                user.getContinuosLearning() - beforeContinuousLearning
                        )
                )
        );
    }

    private int processQuizzes(List<CompleteRequest.QuizAnswer> quizRequestList, Map<Long, Quiz> quizMap) {
        int ansCount = checkQuizAnswer(quizRequestList, quizMap);
        saveQuiz(quizRequestList, quizMap);
        return ansCount;
    }

    private Map<Long, Quiz> loadQuizMap(List<CompleteRequest.QuizAnswer> quizRequestList) {
        List<Long> quizIds = quizRequestList.stream()
                .map(CompleteRequest.QuizAnswer::quizId)
                .toList();
        List<Quiz> quizzes = quizRepository.findAllById(quizIds);
        return quizzes.stream()
                .collect(Collectors.toMap(Quiz::getId, quiz -> quiz));
    }

    private void changeUserInformation(User user, Book book, int ansCount, int unknownWordCount) {
        user.inclusionScore(ansCount, unknownWordCount, book.getTotalWordCount());
        int count = sentenceRepository.countByBookId(book.getId());
        user.learningStatusToday(count);
        user.increaseBookReadCount();
        user.addTotalSentences(count);
        user.addUnknownWords(unknownWordCount);
        userRepository.save(user);
    }

    private int checkQuizAnswer(List<CompleteRequest.QuizAnswer> quizRequestList, Map<Long, Quiz> quizMap) {
        return (int) quizRequestList.stream()
                .filter(quizRequest -> {
                    Quiz quiz = quizMap.get(quizRequest.quizId());
                    if (quiz == null) {
                        throw new CustomException(ErrorCode.QUIZ_NOT_FOUND);
                    }
                    return quiz.getCorrectAnswer() == quizRequest.userAnswer();
                })
                .count();
    }

    private void saveQuiz(List<CompleteRequest.QuizAnswer> quizRequestList, Map<Long, Quiz> quizMap) {
        for (CompleteRequest.QuizAnswer quizRequest : quizRequestList) {
            Quiz quiz = quizMap.get(quizRequest.quizId());
            if (quiz == null) {
                throw new CustomException(ErrorCode.QUIZ_NOT_FOUND);
            }
            quiz.answerQuiz(quizRequest.userAnswer());
            quizRepository.save(quiz);
        }
    }

    private void saveUnknownWord(User user, Book book, List<CompleteRequest.UnknownWord> unknownWordRequestList) {
        for (CompleteRequest.UnknownWord unknownWordRequest : unknownWordRequestList) {
            UnknownWord unknownWord = new UnknownWord(
                    user,
                    book,
                    unknownWordRequest.word(),
                    unknownWordRequest.wordMeaning(),
                    unknownWordRequest.sentence(),
                    unknownWordRequest.sentenceMeaning(),
                    unknownWordRequest.location(),
                    unknownWordRequest.length(),
                    LocalDate.now().plusDays(UNKNOWN_WORD_FIRST_REVIEW_DAYS)
                    );
            unknownWordRepository.save(unknownWord);
        }
    }
}
