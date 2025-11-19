package com.finale.finale.auth.dto.response;

public record NicknameCheckResponse(
        boolean isAvailable,
        String nickname,
        String message
) {
    public static NicknameCheckResponse available(String nickname) {
        return new NicknameCheckResponse(true, nickname, "사용 가능한 닉네임입니다.");
    }

    public static NicknameCheckResponse unavailable(String nickname) {
        return new NicknameCheckResponse(false, nickname, "이미 사용 중인 닉네임입니다.");
    }
}
