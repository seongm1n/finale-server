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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
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
                    "meaning": "Korean meaning",
                    "start": start index (0-based)
                  },
                  ...
                ],
                "phrases": [
                  {
                    "expression": ["word1", "word2", ...],
                    "meaning": "Korean meaning",
                    "start": [start index of word1, start index of word2, ...]
                  },
                  ...
                ]
              }

              ---

              Detailed Rules:
              - Include in `"phrases"` only those combinations that convey a distinct meaning beyond the sum of individual words.
                (e.g., "looked the information up" → `"expression": ["look", "up"]`)
              - Even if the phrase components are not adjacent in the sentence, include them if they function together as one semantic unit.
              - Each `"start"` in `"phrases"` must correspond to the start indices of each word in `"expression"`.
              - Every word must appear in `"words"` with its `"start"` index (0-based character position).
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
                    Integer start = wordNode.get("start").asInt();

                    Word wordEntity = new Word(sentence, word, meaning, start);
                    wordRepository.save(wordEntity);
                }
            }

            JsonNode phrasesNode = root.get("phrases");
            if (phrasesNode != null && phrasesNode.isArray()) {
                for (JsonNode phraseNode : phrasesNode) {
                    String meaning = phraseNode.get("meaning").asText();
                    Phrase phrase = new Phrase(sentence, meaning);

                    JsonNode expressionNode = phraseNode.get("expression");
                    JsonNode startNode = phraseNode.get("start");

                    for (int i = 0; i < expressionNode.size(); i++) {
                        String wordText = expressionNode.get(i).asText();
                        Integer startPos = startNode.get(i).asInt();

                        PhraseWord phraseWord = new PhraseWord(phrase, wordText, startPos);
                        phrase.addPhraseWord(phraseWord);
                    }

                    phraseRepository.save(phrase);
                }
            }

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse word meaning response", e);
        }
    }
}
