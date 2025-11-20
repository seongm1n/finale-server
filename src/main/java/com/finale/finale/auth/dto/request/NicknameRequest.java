package com.finale.finale.auth.dto.request;

import javax.validation.constraints.NotNull;

public record NicknameRequest(
        @NotNull(message = "닉네임은 필수 입력 값입니다.")
        String nickname
) {
}
