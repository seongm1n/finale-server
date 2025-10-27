package com.finale.finale.book.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finale.finale.book.domain.UnknownWord;
import com.finale.finale.book.dto.QuizResponse;
import com.finale.finale.book.dto.StoryGenerationRequest;
import com.finale.finale.book.dto.StoryGenerationResponse;
import com.finale.finale.book.dto.SentenceResponse;
import com.finale.finale.book.repository.UnknownWordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoryGenerationService {

    private final ChatClient chatClient;
    private final UnknownWordRepository unknownWordRepository;

    public StoryGenerationResponse generate(StoryGenerationRequest request, Long userId) {
        List<UnknownWord> unknownWords = new ArrayList<>();
        if (userId != null) {
            unknownWords = unknownWordRepository.findTop10ByUserIdAndNextReviewDateBeforeOrEqual(userId, LocalDate.now());
        }

        String promptText = createPrompt(request, unknownWords);

        String response = chatClient.prompt()
                .user(promptText)
                .call()
                .content();

        List<SentenceResponse> sentences = parseResponse(response);
        int totalWords = calculateTotalWords(sentences);
        List<QuizResponse> quizzes = parseQuizzes(response);

        return new StoryGenerationResponse(
                1L,
                extractTitle(response),
                request.category(),
                request.abilityScore(),
                totalWords,
                sentences,
                quizzes,
                LocalDateTime.now()
        );
    }

    private String createPrompt(StoryGenerationRequest request, List<UnknownWord> unknownWords) {
        StringBuilder vocabSection = new StringBuilder();

        if (request.recommendedWords() != null && !request.recommendedWords().isEmpty()) {
            vocabSection.append("NEW WORDS: ").append(String.join(", ", request.recommendedWords()));
        }

        if (!unknownWords.isEmpty()) {
            if (vocabSection.length() > 0) vocabSection.append("\n");
            vocabSection.append("REVIEW WORDS (use these words naturally in the story):\n");
            for (UnknownWord uw : unknownWords) {
                vocabSection.append(String.format("- %s (example: \"%s\")\n", uw.getWord(), uw.getSentence()));
            }
        }

        return String.format("""
            Create an English learning story.

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
                    {"question": "스토리 내용에 관한 질문 1", "correct_answer": true},
                    {"question": "스토리 내용에 관한 질문 2", "correct_answer": false}
                ]
            }

            REQUIREMENTS:
            1. Total word count: 950-1050 English words
            2. Paragraphs numbered sequentially from 1
            3. Each sentence has accurate Korean translation
            4. Create exactly 2 True/False quizzes in Korean
            5. Return pure JSON only (no code blocks, no markdown)
            """,
                request.category(),
                request.abilityScore(),
                vocabSection.toString().trim()
        );
    }

    private List<SentenceResponse> parseResponse(String response) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonResponse = extractJsonFromResponse(response);

            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode sentencesNode = rootNode.get("sentences");

            List<SentenceResponse> sentences = new ArrayList<>();

            if (sentencesNode != null && sentencesNode.isArray()) {
                for (JsonNode sentenceNode : sentencesNode) {
                    SentenceResponse sentence = new SentenceResponse(
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
            System.err.println("Failed to parse GPT response: " + e.getMessage());
            return new ArrayList<>();
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
            System.err.println("Failed to extract title: " + e.getMessage());
            return "Untitled Story";
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

    private List<QuizResponse> parseQuizzes(String response) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonResponse = extractJsonFromResponse(response);
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode quizzesNode = rootNode.get("quizzes");

            List<QuizResponse> quizzes = new ArrayList<>();

            if (quizzesNode != null && quizzesNode.isArray()) {
                long quizId = 1L;
                for (JsonNode quizNode : quizzesNode) {
                    QuizResponse quiz = new QuizResponse(
                            quizId++,
                            quizNode.get("question").asText(),
                            quizNode.get("correct_answer").asBoolean()
                    );
                    quizzes.add(quiz);
                }
            }

            return quizzes;
        } catch (Exception e) {
            System.err.println("Failed to parse quizzes: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private int calculateTotalWords(List<SentenceResponse> sentences) {
        return sentences.stream()
                .mapToInt(sentence -> {
                    String englishText = sentence.englishText();
                    if (englishText == null || englishText.isBlank()) {
                        return 0;
                    }
                    return englishText.trim().split("\\s+").length;
                })
                .sum();
    }
}
