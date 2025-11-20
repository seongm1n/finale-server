package com.finale.finale.auth.dto.request;

import javax.validation.constraints.NotNull;

public record RefreshRequest(
        @NotNull(message = "리프레시 토큰은 필수 입력 값입니다.")
        String refreshToken
) {
}
