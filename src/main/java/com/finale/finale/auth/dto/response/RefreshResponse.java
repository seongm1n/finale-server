package com.finale.finale.auth.dto.response;

public record RefreshResponse(
    String accessToken,
    String refreshToken,
    UserResponse user
) {
}
