package com.finale.finale.book.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finale.finale.book.domain.Phrase;
import com.finale.finale.book.domain.PhraseWord;
import com.finale.finale.book.domain.Sentence;
import com.finale.finale.book.domain.Word;
import com.finale.finale.book.repository.PhraseRepository;
import com.finale.finale.book.repository.WordRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class WordMeaningService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WordRepository wordRepository;
    private final PhraseRepository phraseRepository;

    private static final String ANALYSIS_PROMPT = """
              You are an expert linguist and natural language processing researcher.
              Your task is to precisely analyze a given English sentence,
              extracting both the meaning of each word and any multi-word expressions (phrases)
              that act as single semantic units in context.
              Return the result strictly in JSON format, without any explanations or extra text.

              ---

              Output Format (JSON object):
              {
                "words": [
                  {
                    "word": "word",
                    "meaning": "Korean meaning"
                  },
                  ...
                ],
                "phrases": [
                  {
                    "expression": ["word1", "word2", ...],
                    "meaning": "Korean meaning"
                  },
                  ...
                ]
              }

              ---

              Rules:
              - Each word must provide its basic meaning in Korean.
              - Identify multi-word expressions (phrasal verbs, idioms, etc.) that form a single semantic unit.
              - ⚠️ CRITICAL: In "expression", each element MUST be a separate word (no spaces).
                ❌ Wrong: ["spoke of"]
                ✅ Correct: ["spoke", "of"]
              - Even if phrase components are not adjacent, include them if they function together.
                (e.g., "looked the information up" → ["looked", "up"])
              - If no phrases exist, return `"phrases": []`.
              - Output must be **only valid JSON**, no additional commentary.

              ---

              Sentence: "{sentence}"
              """;

    @Transactional
    public void extractWordMeanings(Sentence sentence) {
        String prompt = ANALYSIS_PROMPT.replace("{sentence}", sentence.getEnglishText());

        String jsonResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);

            JsonNode wordsNode = root.get("words");
            if (wordsNode != null && wordsNode.isArray()) {
                for (JsonNode wordNode : wordsNode) {
                    String word = wordNode.get("word").asText();
                    String meaning = wordNode.get("meaning").asText();

                    Integer location = findWordLocation(sentence.getEnglishText(), word, 0);
                    Word wordEntity = new Word(sentence, word, meaning, location);
                    wordRepository.save(wordEntity);
                }
            }

            JsonNode phrasesNode = root.get("phrases");
            if (phrasesNode != null && phrasesNode.isArray()) {
                for (JsonNode phraseNode : phrasesNode) {
                    String meaning = phraseNode.get("meaning").asText();
                    Phrase phrase = new Phrase(sentence, meaning);

                    JsonNode expressionNode = phraseNode.get("expression");
                    int searchFrom = 0;

                    for (int i = 0; i < expressionNode.size(); i++) {
                        String wordText = expressionNode.get(i).asText();
                        Integer location = findWordLocation(sentence.getEnglishText(), wordText, searchFrom);
                        searchFrom = location + wordText.length();

                        PhraseWord phraseWord = new PhraseWord(phrase, wordText, location);
                        phrase.addPhraseWord(phraseWord);
                    }

                    phraseRepository.save(phrase);
                }
            }

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse word meaning response", e);
        }
    }

    private Integer findWordLocation(String sentence, String word, int fromIndex) {
        String pattern = "\\b" + Pattern.quote(word) + "\\b";
        Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(sentence);

        if (m.find(fromIndex)) {
            return m.start();
        }

        log.warn("Word '{}' not found in sentence: '{}'", word, sentence);
        return 0;
    }
}
