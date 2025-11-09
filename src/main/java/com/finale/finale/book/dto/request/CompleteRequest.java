package com.finale.finale.book.dto.request;

import javax.validation.constraints.NotNull;
import java.util.List;

public record CompleteRequest(
        @NotNull(message = "퀴즈 답변 목록은 필수 입력 값입니다.")
        List<QuizAnswer> quizAnswers,

        List<UnknownWord> unknownWords
) {
    public record QuizAnswer(
            @NotNull(message = "퀴즈 ID는 필수 입력 값입니다.")
            Long quizId,

            @NotNull(message = "사용자 답변은 필수 입력 값입니다.")
            Boolean userAnswer
    ) {}

    public record UnknownWord(
            @NotNull(message = "단어는 필수 입력 값입니다.")
            String word,

            @NotNull(message = "단어 뜻은 필수 입력 값입니다.")
            String wordMeaning,

            @NotNull(message = "문장은 필수 입력 값입니다.")
            String sentence,

            @NotNull(message = "문장 뜻은 필수 입력 값입니다.")
            String sentenceMeaning,

            @NotNull(message = "문장 ID는 필수 입력 값입니다.")
            Long sentenceId,

            @NotNull(message = "단어 위치는 필수 입력 값입니다.")
            int location,

            @NotNull(message = "단어 길이는 필수 입력 값입니다.")
            int length
    ) {}
}
