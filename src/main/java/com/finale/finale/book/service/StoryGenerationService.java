package com.finale.finale.book.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        List<SentenceResponse> sentence = parseResponse(response);

        return new StoryGenerationResponse(
                1L,
                extractTitle(response),
                request.category(),
                request.abilityScore(),
                sentence,
                LocalDateTime.now()
        );
    }

    private String createPrompt(StoryGenerationRequest request) {
        String words = (request.recommendedWords() != null)
                ? String.join(", ", request.recommendedWords())
                : "adventure, mysterious, ancient";

        return String.format("""
            Create an English story for language learners in %s category with Lexile score %d.
            Use these recommended words: %s

            CRITICAL REQUIREMENTS:
            1. The story MUST be EXACTLY 600 words in total (count all English words)
            2. Create a creative and engaging title
            3. Divide the story into 4-5 paragraphs
            4. Each paragraph should contain multiple sentences
            5. Each sentence should be appropriate for Lexile %d reading level
            6. Number paragraphs sequentially (1, 2, 3, 4, 5)
            7. Each sentence must have Korean translation

            WORD COUNT REQUIREMENT: The total English word count across ALL sentences MUST equal approximately 600 words.
            This is MANDATORY. Do NOT generate fewer than 600 words.

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
                ]
            }

            REMINDER: Generate enough sentences to reach 600 total English words.
            Make sure paragraphs are properly numbered (1, 2, 3, 4, 5).
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
}
