package com.finale.finale.auth.oauth;

public record OAuth2UserInfo(
        String email,
        String name,
        String providerId,
        String provider
) {

    public OAuth2UserInfo {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email cannot be blank");
        }
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("providerId cannot be blank");
        }
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("provider cannot be blank");
        }
    }

    public String getNameOrDefault() {
        return (name == null || name.isBlank()) ? "User" : name;
    }
}
