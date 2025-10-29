package com.finale.finale.auth.dto.response;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    UserResponse user
) {
}
