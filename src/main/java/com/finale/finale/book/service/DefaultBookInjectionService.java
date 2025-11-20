package com.finale.finale.book.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finale.finale.auth.domain.User;
import com.finale.finale.book.domain.*;
import com.finale.finale.book.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultBookInjectionService {

    private final BookRepository bookRepository;
    private final SentenceRepository sentenceRepository;
    private final QuizRepository quizRepository;
    private final WordRepository wordRepository;
    private final PhraseRepository phraseRepository;
    private final ObjectMapper objectMapper;

    private static final String DEFAULT_BOOK_PATH = "default-book.json";

    @Transactional
    public void injectDefaultBook(User user) {
        try {
            Map<String, Object> bookData = loadDefaultBookData();

            Book book = createBook(user, bookData);
            bookRepository.save(book);
            log.info("Default book created for user {}: {}", user.getId(), book.getTitle());

            List<Map<String, Object>> sentencesData = getList(bookData, "sentences");
            for (Map<String, Object> sentenceData : sentencesData) {
                Sentence sentence = createSentence(book, sentenceData);
                sentenceRepository.save(sentence);

                List<Map<String, Object>> wordsData = getList(sentenceData, "words");
                for (Map<String, Object> wordData : wordsData) {
                    Word word = createWord(sentence, wordData);
                    wordRepository.save(word);
                }

                List<Map<String, Object>> phrasesData = getList(sentenceData, "phrases");
                for (Map<String, Object> phraseData : phrasesData) {
                    Phrase phrase = createPhrase(sentence, phraseData);

                    List<Map<String, Object>> expressionData = getList(phraseData, "expression");
                    for (Map<String, Object> phraseWordData : expressionData) {
                        PhraseWord phraseWord = new PhraseWord(
                            phrase,
                            getString(phraseWordData, "word"),
                            getInt(phraseWordData, "location")
                        );
                        phrase.addPhraseWord(phraseWord);
                    }

                    phraseRepository.save(phrase);
                }
            }

            List<Map<String, Object>> quizzesData = getList(bookData, "quizzes");
            for (Map<String, Object> quizData : quizzesData) {
                Quiz quiz = createQuiz(book, quizData);
                quizRepository.save(quiz);
            }

            log.info("Default book injection completed for user {}", user.getId());
        } catch (IOException e) {
            log.error("Failed to inject default book for user {}: {}", user.getId(), e.getMessage());
            throw new RuntimeException("Failed to load default book data", e);
        }
    }

    private Map<String, Object> loadDefaultBookData() throws IOException {
        ClassPathResource resource = new ClassPathResource(DEFAULT_BOOK_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {});
        }
    }

    private Book createBook(User user, Map<String, Object> data) {
        String categoryStr = getString(data, "category").toUpperCase();
        BookCategory category = BookCategory.valueOf(categoryStr);

        return new Book(
            user,
            getString(data, "title"),
            category,
            getInt(data, "abilityScore"),
            getInt(data, "totalWordCount")
        );
    }

    private Sentence createSentence(Book book, Map<String, Object> data) {
        return new Sentence(
            book,
            getInt(data, "paragraphNumber"),
            getInt(data, "sentenceOrder"),
            getString(data, "englishText"),
            getString(data, "koreanText")
        );
    }

    private Word createWord(Sentence sentence, Map<String, Object> data) {
        return new Word(
            sentence,
            getString(data, "word"),
            getString(data, "meaning"),
            getInt(data, "location")
        );
    }

    private Phrase createPhrase(Sentence sentence, Map<String, Object> data) {
        return new Phrase(
            sentence,
            getString(data, "meaning")
        );
    }

    private Quiz createQuiz(Book book, Map<String, Object> data) {
        return new Quiz(
            book,
            getString(data, "question"),
            getBoolean(data, "correctAnswer")
        );
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return List.of();
        }
        return (List<Map<String, Object>>) value;
    }

    private String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : "";
    }

    private int getInt(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private boolean getBoolean(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return false;
    }
}
