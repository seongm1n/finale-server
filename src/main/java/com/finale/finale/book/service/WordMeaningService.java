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

              Analysis Rules:
              1️⃣ Analyze each individual word and provide its basic meaning in Korean.
              2️⃣ Identify and extract any combination of two or more words that together form a single semantic unit
                 (e.g., phrasal verbs, idioms, prepositional phrases, or common expressions).
              3️⃣ Return both results as separate JSON arrays.
                 If there are no phrases in the sentence, return `"phrases": []`.

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

              Detailed Rules:
              - Include in `"phrases"` only those combinations that convey a distinct meaning beyond the sum of individual words.
                (e.g., "looked the information up" → `"expression": ["look", "up"]`)
              - Even if the phrase components are not adjacent in the sentence, include them if they function together as one semantic unit.
              - If no multi-word expressions exist, `"phrases": []` must still be returned.
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
        String lowerSentence = sentence.toLowerCase();
        String lowerWord = word.toLowerCase();

        int index = lowerSentence.indexOf(lowerWord, fromIndex);

        if (index == -1) {
            log.warn("Word '{}' not found in sentence: '{}'", word, sentence);
            return 0;
        }

        return index;
    }
}
