package com.finale.finale.auth.oauth;

import org.springframework.security.oauth2.core.OAuth2AccessToken;

public interface OAuth2TokenValidator {
    OAuth2UserInfo validate(String token);
    String getProviderName();
}
