package com.finale.finale.auth.dto;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    UserResponse user
) {
}
