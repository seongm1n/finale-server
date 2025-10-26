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

            if (kakaoAccount == null || !kakaoAccount.has("email")) {
                throw new OAuth2AuthenticationException(PROVIDER_NAME, "Email not provided");
            }

            String email = kakaoAccount.get("email").asText();

            boolean emailVerified = kakaoAccount.has("is_email_verified")
                && kakaoAccount.get("is_email_verified").asBoolean();

            if (!emailVerified) {
                throw new OAuth2AuthenticationException(PROVIDER_NAME, "Email not verified");
            }

            String name = null;
            if (kakaoAccount.has("profile")) {
                JsonNode profile = kakaoAccount.get("profile");
                if (profile.has("nickname")) {
                    name = profile.get("nickname").asText();
                }
            }

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
}
