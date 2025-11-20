package com.finale.finale.auth.dto.request;

public record LoginRequest(
        String provider,
        String idToken,
        String authCode
) {
}
