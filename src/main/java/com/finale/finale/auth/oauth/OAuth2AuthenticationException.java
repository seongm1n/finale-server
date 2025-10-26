package com.finale.finale.auth.oauth;

public class OAuth2AuthenticationException extends RuntimeException {

    private final String provider;

    public OAuth2AuthenticationException(String provider, String message) {
        super(String.format("[%s] %s", provider, message));
        this.provider = provider;
    }

    public OAuth2AuthenticationException(String provider, String message, Throwable cause) {
        super(String.format("[%s] %s", provider, message), cause);
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }
}
