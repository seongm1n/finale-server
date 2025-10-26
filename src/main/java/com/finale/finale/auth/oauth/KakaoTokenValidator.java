package com.finale.finale.auth.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class KakaoTokenValidator implements OAuth2TokenValidator {

    private static final String PROVIDER_NAME = "kakao";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    @Value("${oauth2.kakao.client-id}")
    private String clientId;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public OAuth2UserInfo validate(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                KAKAO_USER_INFO_URL,
                HttpMethod.GET,
                entity,
                String.class
            );

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            String providerId = rootNode.get("id").asText();
            JsonNode kakaoAccount = rootNode.get("kakao_account");

            String email = extractEmail(kakaoAccount, providerId);
            String name = extractName(kakaoAccount);

            return new OAuth2UserInfo(email, name, providerId, PROVIDER_NAME);

        } catch (HttpClientErrorException e) {
            throw new OAuth2AuthenticationException(PROVIDER_NAME, "Invalid access token", e);
        } catch (Exception e) {
            throw new OAuth2AuthenticationException(PROVIDER_NAME, "Token validation failed", e);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    private String extractEmail(JsonNode kakaoAccount, String providerId) {
        if (kakaoAccount == null || !kakaoAccount.has("email")) {
            return "kakao_" + providerId + "@finale.internal";
        }

        String email = kakaoAccount.get("email").asText();
        boolean emailVerified = kakaoAccount.has("is_email_verified")
            && kakaoAccount.get("is_email_verified").asBoolean();

        return emailVerified ? email : "kakao_" + providerId + "@finale.internal";
    }

    private String extractName(JsonNode kakaoAccount) {
        if (kakaoAccount == null || !kakaoAccount.has("profile")) {
            return null;
        }

        JsonNode profile = kakaoAccount.get("profile");
        return profile.has("nickname") ? profile.get("nickname").asText() : null;
    }
}
