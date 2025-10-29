package com.finale.finale.book.dto.request;

import javax.validation.constraints.NotNull;
import java.util.List;

public record StoryGenerationRequest(
        int abilityScore,

        @NotNull(message = "카테고리는 필수 입력 값입니다.")
        String category,

        List<String> recommendedWords
) {
}
