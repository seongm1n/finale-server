package com.finale.finale.book.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finale.finale.book.dto.QuizResponse;
import com.finale.finale.book.dto.StoryGenerationRequest;
import com.finale.finale.book.dto.StoryGenerationResponse;
import com.finale.finale.book.dto.SentenceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoryGenerationService {

    private final ChatClient chatClient;

    public StoryGenerationResponse generate(StoryGenerationRequest request) {
        String promptText = createPrompt(request);

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

    private String createPrompt(StoryGenerationRequest request) {
        String words = (request.recommendedWords() != null)
                ? String.join(", ", request.recommendedWords())
                : " ";

        return String.format("""
            Create an English story for language learners in %s category with Lexile score %d.
            Use these recommended words: %s

            CRITICAL REQUIREMENTS:
            1. The story MUST be between 450-550 words in total (count all English words)
            2. Create a creative and engaging title
            3. Divide the story into 4-5 paragraphs
            4. Each paragraph should contain multiple sentences
            5. Each sentence should be appropriate for Lexile %d reading level
            6. Number paragraphs sequentially (1, 2, 3, 4, 5)
            7. Each sentence must have Korean translation
            8. Create EXACTLY 2 OX (True/False) quizzes in Korean based on the story content

            WORD COUNT REQUIREMENT: The total English word count across ALL sentences MUST be between 450-550 words.
            This is MANDATORY.

            IMPORTANT: Return ONLY valid JSON in this EXACT format (no markdown, no extra text):
            {
                "title": "story title here",
                "sentences": [
                    {
                        "paragraph_number": 1,
                        "sentence_order": 1,
                        "english_text": "first sentence of paragraph 1",
                        "korean_text": "첫 번째 단락의 첫 문장 한국어 번역"
                    },
                    {
                        "paragraph_number": 1,
                        "sentence_order": 2,
                        "english_text": "second sentence of paragraph 1",
                        "korean_text": "첫 번째 단락의 두 번째 문장 한국어 번역"
                    },
                    {
                        "paragraph_number": 2,
                        "sentence_order": 1,
                        "english_text": "first sentence of paragraph 2",
                        "korean_text": "두 번째 단락의 첫 문장 한국어 번역"
                    }
                ],
                "quizzes": [
                    {
                        "question": "첫번째 퀴즈.",
                        "correct_answer": true
                    },
                    {
                        "question": "두번째 퀴즈.",
                        "correct_answer": false
                    }
                ]
            }

            REMINDER: Generate enough sentences to reach 450-550 total English words.
            Make sure paragraphs are properly numbered (1, 2, 3, 4, 5).
            Create EXACTLY 2 OX quizzes in Korean about the story.
            Return ONLY the JSON object, nothing else.
            """,
                request.category(),
                request.abilityScore(),
                words,
                request.abilityScore()
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
