package com.finale.finale.book.service;

import com.finale.finale.auth.domain.User;
import com.finale.finale.auth.repository.UserRepository;
import com.finale.finale.book.domain.Book;
import com.finale.finale.book.domain.Quiz;
import com.finale.finale.book.domain.UnknownWord;
import com.finale.finale.book.dto.request.CompleteRequest;
import com.finale.finale.book.dto.request.QuizRequest;
import com.finale.finale.book.dto.request.UnknownWordRequest;
import com.finale.finale.book.dto.response.CompleteResponse;
import com.finale.finale.book.repository.BookRepository;
import com.finale.finale.book.repository.QuizRepository;
import com.finale.finale.book.repository.SentenceRepository;
import com.finale.finale.book.repository.UnknownWordRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LearningService {

    private final UnknownWordRepository unknownWordRepository;
    private final QuizRepository quizRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final SentenceRepository sentenceRepository;

    public CompleteResponse complete(Long userId, Long bookId, CompleteRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        List<QuizRequest> quizRequestList = request.quizAnswers();
        int ansCount = checkQuizAnswer(quizRequestList);
        saveQuiz(quizRequestList);

        List<UnknownWordRequest> unknownWordRequestList = request.unknownWords();
        int unknownWordCount = unknownWordRequestList.size();
        saveUnknownWord(user, book, unknownWordRequestList);

        int beforeAbilityScore = user.getAbilityScore();
        int beforeTotalSentencesRead = user.getTotalSentencesRead();
        int beforeUnknownWordsCount = user.getUnknownWordsCount();
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
                                user.getBookReadCount() - 1,
                                user.getBookReadCount(),
                                1
                        ),
                        new CompleteResponse.StatChange(
                                beforeTotalSentencesRead,
                                user.getTotalSentencesRead(),
                                user.getTotalSentencesRead() - beforeTotalSentencesRead
                        ),
                        new CompleteResponse.StatChange(
                                beforeUnknownWordsCount,
                                user.getUnknownWordsCount(),
                                user.getUnknownWordsCount() - beforeUnknownWordsCount
                        )
                )
        );
    }

    private void changeUserInformation(User user, Book book, int ansCount, int unknownWordCount) {
        user.inclusionScore(ansCount, unknownWordCount);
        int count = sentenceRepository.countByBookId(book.getId());
        user.increaseBookReadCount();
        user.addTotalSentences(count);
        user.addUnknownWords(unknownWordCount);
        userRepository.save(user);
    }

    private int checkQuizAnswer(List<QuizRequest> quizRequestList) {
        return (int) quizRequestList.stream()
                .filter(quizRequest -> {
                    Quiz quiz = quizRepository.findById(quizRequest.quizId())
                            .orElseThrow(() -> new RuntimeException("Quiz not found"));
                    return quiz.getCorrectAnswer() == quizRequest.userAnswer();
                })
                .count();
    }

    private void saveQuiz(List<QuizRequest> quizRequestList) {
        for (QuizRequest quizRequest : quizRequestList) {
            Quiz quiz = quizRepository.findById(quizRequest.quizId())
                    .orElseThrow(() -> new RuntimeException("Quiz not found"));
            quiz.answerQuiz(quizRequest.userAnswer());
            quizRepository.save(quiz);
        }
    }

    private void saveUnknownWord(User user, Book book, List<UnknownWordRequest> unknownWordRequestList) {
        for (UnknownWordRequest unknownWordRequest : unknownWordRequestList) {
            UnknownWord unknownWord = new UnknownWord(
                    user,
                    book,
                    unknownWordRequest.word(),
                    unknownWordRequest.wordMeaning(),
                    unknownWordRequest.sentence(),
                    unknownWordRequest.sentenceMeaning(),
                    unknownWordRequest.location(),
                    unknownWordRequest.length(),
                    LocalDate.now().plusDays(3)
                    );
            unknownWordRepository.save(unknownWord);
        }
    }
}
