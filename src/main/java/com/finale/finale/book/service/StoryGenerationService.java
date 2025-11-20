package com.finale.finale.book.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finale.finale.auth.domain.User;
import com.finale.finale.auth.repository.UserRepository;
import com.finale.finale.book.domain.*;
import com.finale.finale.book.repository.*;
import com.finale.finale.exception.CustomException;
import com.finale.finale.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class StoryGenerationService {

    private final ChatClient chatClient;
    private final UnknownWordRepository unknownWordRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final SentenceRepository sentenceRepository;
    private final QuizRepository quizRepository;
    private final WordMeaningService wordMeaningService;
    private final RedisLockService redisLockService;
    private final UnknownPhraseRepository unknownPhraseRepository;

    @Async
    public void generate(Long userId) {
        String lockKey = "book-generation:" + userId;

        if (!redisLockService.tryLock(lockKey, 0, 300)) {
            log.info("Book generation already in progress for user {}", userId);
            return;
        }

        try {
            GenerationData data = loadDataForGeneration(userId);
            if (data == null) {
                return;
            }

            String response = chatClient.prompt()
                    .user(data.prompt())
                    .call()
                    .content();

            saveGeneratedBook(data, response);

        } finally {
            redisLockService.unlock(lockKey);
        }
    }

    @Transactional
    protected GenerationData loadDataForGeneration(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (bookRepository.countByUserAndIsProvisionFalse(user) >= 2) {
            log.info("User {} 유저의 미할당 책이 충분합니다.", userId);
            return null;
        }

        List<UnknownWord> unknownWords = unknownWordRepository
                .findTop5ByUser_IdAndNextReviewDateLessThanEqualOrderByNextReviewDateAsc(userId, LocalDate.now());

        List<UnknownPhrase> unknownPhrases = unknownPhraseRepository
                .findTop5ByUser_IdAndNextReviewDateLessThanEqualOrderByNextReviewDateAsc(userId, LocalDate.now());

        BookCategory category = BookCategory.random();
        String prompt = createPrompt(unknownWords, unknownPhrases, user, category);

        return new GenerationData(
                userId,
                user.getAbilityScore(),
                category,
                prompt,
                unknownWords.stream().map(w -> w.getId()).toList(),
                unknownPhrases.stream().map(p -> p.getId()).toList()
        );
    }

    @Transactional
    protected void saveGeneratedBook(GenerationData data, String response) {
        User user = userRepository.findById(data.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<Sentence> sentences = parseResponse(response);
        int totalWords = calculateTotalWords(sentences);

        Book book = new Book(
                user,
                extractTitle(response),
                data.category(),
                data.abilityScore(),
                totalWords
        );

        List<UnknownWord> reviewWords = unknownWordRepository.findAllById(data.reviewWordIds());
        List<UnknownPhrase> reviewPhrases = unknownPhraseRepository.findAllById(data.reviewPhraseIds());

        reviewWords.forEach(UnknownWord::nextReviewSetting);
        reviewPhrases.forEach(UnknownPhrase::nextReviewSetting);

        book.addReviewWord(reviewWords);
        book.addReviewPhrase(reviewPhrases);
        bookRepository.save(book);

        List<Quiz> quizzes = parseQuizzes(response, book);
        quizRepository.saveAll(quizzes);

        sentences.forEach(sentence -> sentence.updateBook(book));
        sentenceRepository.saveAll(sentences);

        sentences.forEach(wordMeaningService::extractWordMeanings);
    }

    private String createPrompt(List<UnknownWord> unknownWords, List<UnknownPhrase> unknownPhrases, User user, BookCategory category) {
        Random random = new Random();
        boolean quiz1Answer = random.nextBoolean();
        boolean quiz2Answer = random.nextBoolean();

        StringBuilder vocabSection = new StringBuilder();

        if (!unknownWords.isEmpty()) {
            vocabSection.append("REVIEW WORDS (use these words naturally in the story):\n");
            vocabSection.append("IMPORTANT: The example sentences below are for REFERENCE ONLY. ");
            vocabSection.append("You MUST create completely DIFFERENT sentences with creative and varied contexts. ");
            vocabSection.append("DO NOT copy or closely imitate the example sentences.\n\n");
            for (UnknownWord uw : unknownWords) {
                vocabSection.append(String.format("- %s (reference example: \"%s\")\n", uw.getWord(), uw.getSentence()));
            }
        }

        if (!unknownPhrases.isEmpty()) {
            if (!vocabSection.isEmpty()) vocabSection.append("\n");
            vocabSection.append("REVIEW PHRASES (use these phrases naturally in the story):\n");
            vocabSection.append("IMPORTANT: The example sentences below are for REFERENCE ONLY. ");
            vocabSection.append("You MUST create completely DIFFERENT sentences with creative and varied contexts. ");
            vocabSection.append("DO NOT copy or closely imitate the example sentences.\n\n");
            for (UnknownPhrase up : unknownPhrases) {
                vocabSection.append(String.format("- %s (reference example: \"%s\")\n", up.getPhrase(), up.getSentence()));
            }
        }

        return String.format("""
            You are a professional storyteller and novelist who specializes in creating captivating stories for readers of all levels.
            Your stories are engaging, well-structured, and immersive, making readers feel like they're reading a real book.
            You have expertise in adapting your writing style to match different reading levels while maintaining literary quality.

            Create an engaging story that feels like a real book chapter.

            STORY SETTINGS:
            - Category: %s
            - Lexile Level: %dL
            - Word Count: 950-1050 words (STRICTLY ENFORCE)
            - Structure: Divide into appropriate paragraphs for natural story flow

            VOCABULARY TO INCLUDE:
            %s

            OUTPUT FORMAT (return ONLY valid JSON, no markdown):
            {
                "title": "Creative Story Title",
                "sentences": [
                    {"paragraph_number": 1, "sentence_order": 1, "english_text": "First sentence.", "korean_text": "첫 번째 문장."},
                    {"paragraph_number": 1, "sentence_order": 2, "english_text": "Second sentence.", "korean_text": "두 번째 문장."}
                ],
                "quizzes": [
                    {"question": "스토리 내용에 관한 질문 1", "correct_answer": %b},
                    {"question": "스토리 내용에 관한 질문 2", "correct_answer": %b}
                ]
            }

            REQUIREMENTS:
            1. Total word count: 950-1050 English words
            2. Paragraphs numbered sequentially from 1
            3. Each sentence has accurate Korean translation
            4. Create exactly 2 True/False quizzes in Korean
            5. CRITICAL: Quiz 1 correct answer MUST be %b, Quiz 2 correct answer MUST be %b
            6. Create questions that match the given answers based on story content
            7. Return pure JSON only (no code blocks, no markdown)
            """,
                category.getValue(),
                (int) (user.getAbilityScore() * 1.4),
                vocabSection.toString().trim(),
                quiz1Answer,
                quiz2Answer,
                quiz1Answer,
                quiz2Answer
        );
    }

    private List<Sentence> parseResponse(String response) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonResponse = extractJsonFromResponse(response);

            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode sentencesNode = rootNode.get("sentences");

            List<Sentence> sentences = new ArrayList<>();

            if (sentencesNode != null && sentencesNode.isArray()) {
                for (JsonNode sentenceNode : sentencesNode) {
                    Sentence sentence = new Sentence(
                            null,
                            sentenceNode.get("paragraph_number").asInt(),
                            sentenceNode.get("sentence_order").asInt(),
                            sentenceNode.get("english_text").asText(),
                            sentenceNode.get("korean_text").asText()
                    );
                    sentences.add(sentence);
                }
            }
            return sentences;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.AI_RESPONSE_INVALID);
        }
    }

    private String extractTitle(String response) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonResponse = extractJsonFromResponse(response);
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            JsonNode titleNode = rootNode.get("title");
            if (titleNode != null) {
                return titleNode.asText();
            }

            return "Untitled Story";
        } catch (Exception e) {
            throw new CustomException(ErrorCode.AI_RESPONSE_INVALID);
        }
    }

    private String extractJsonFromResponse(String response) {
        String cleaned = response.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }

    private List<Quiz> parseQuizzes(String response, Book book) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonResponse = extractJsonFromResponse(response);
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode quizzesNode = rootNode.get("quizzes");

            List<Quiz> quizzes = new ArrayList<>();

            if (quizzesNode != null && quizzesNode.isArray()) {
                for (JsonNode quizNode : quizzesNode) {
                    Quiz quiz = new Quiz(
                            book,
                            quizNode.get("question").asText(),
                            quizNode.get("correct_answer").asBoolean()
                    );
                    quizzes.add(quiz);
                }
            }
            return quizzes;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.AI_RESPONSE_INVALID);
        }
    }

    private int calculateTotalWords(List<Sentence> sentences) {
        return sentences.stream()
                .mapToInt(sentence -> {
                    String englishText = sentence.getEnglishText();
                    if (englishText == null || englishText.isBlank()) {
                        return 0;
                    }
                    return englishText.trim().split("\\s+").length;
                })
                .sum();
    }

    private record GenerationData(
            Long userId,
            Integer abilityScore,
            BookCategory category,
            String prompt,
            List<Long> reviewWordIds,
            List<Long> reviewPhraseIds
    ) {}
}
