package com.finale.finale.auth.dto.request;

import javax.validation.constraints.NotNull;

public record AbilityRequest(
        @NotNull(message = "리딧 스코어는 필수 입력 값입니다.")
        Integer abilityScore
) {
}
