package com.finale.finale.book.service;

import com.finale.finale.auth.domain.User;
import com.finale.finale.auth.repository.UserRepository;
import com.finale.finale.book.domain.Book;
import com.finale.finale.book.dto.response.QuizResponse;
import com.finale.finale.book.dto.response.SentenceResponse;
import com.finale.finale.book.dto.response.StoryGenerationResponse;
import com.finale.finale.book.dto.response.UnknownWordResponse;
import com.finale.finale.book.repository.BookRepository;
import com.finale.finale.book.repository.QuizRepository;
import com.finale.finale.book.repository.SentenceRepository;
import com.finale.finale.exception.CustomException;
import com.finale.finale.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final SentenceRepository sentenceRepository;
    private final QuizRepository quizRepository;

    @Transactional
    public StoryGenerationResponse getNewStory(Long userId) {
        // TODO : 비관적 락 구현 고려
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Book book = bookRepository.findFirstByUserAndIsProvisionFalseWithReviewWords(user)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_READY));

        List<SentenceResponse> sentences = sentenceRepository.findAllByBook(book).stream()
                .map(sentence -> new SentenceResponse(
                        sentence.getId(),
                        sentence.getParagraphNumber(),
                        sentence.getSentenceOrder(),
                        sentence.getEnglishText(),
                        sentence.getKoreanText()
                ))
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
}
