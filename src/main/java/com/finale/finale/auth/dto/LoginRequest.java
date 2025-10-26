package com.finale.finale.auth.dto;

public record LoginRequest(
    String provider,
    String idToken,
    String authCode
) {
}
