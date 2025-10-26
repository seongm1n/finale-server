package com.finale.finale.auth.dto;

public record RefreshResponse(
    String accessToken,
    String refreshToken
) {
}
