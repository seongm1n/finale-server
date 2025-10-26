package com.finale.finale.auth.oauth;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@Component
public class AppleTokenValidator implements OAuth2TokenValidator {

    private static final String PROVIDER_NAME = "apple";
    private static final String APPLE_PUBLIC_KEYS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    @Value("${oauth2.apple.client-id}")
    private String clientId;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public OAuth2UserInfo validate(String idToken) {
        try {
            String[] tokenParts = idToken.split("\\.");
            if (tokenParts.length != 3) {
                throw new OAuth2AuthenticationException(PROVIDER_NAME, "Invalid token format");
            }

            String headerJson = new String(Base64.getUrlDecoder().decode(tokenParts[0]));
            JsonNode header = objectMapper.readTree(headerJson);
            String kid = header.get("kid").asText();

            PublicKey publicKey = getApplePublicKey(kid);

            Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(idToken)
                .getPayload();

            if (!APPLE_ISSUER.equals(claims.getIssuer())) {
                throw new OAuth2AuthenticationException(PROVIDER_NAME, "Invalid issuer");
            }

            if (!clientId.equals(claims.getAudience().iterator().next())) {
                throw new OAuth2AuthenticationException(PROVIDER_NAME, "Invalid audience");
            }

            String email = claims.get("email", String.class);
            String providerId = claims.getSubject();
            Boolean emailVerified = claims.get("email_verified", Boolean.class);

            if (email == null) {
                throw new OAuth2AuthenticationException(PROVIDER_NAME, "Email not provided");
            }

            if (emailVerified == null || !emailVerified) {
                throw new OAuth2AuthenticationException(PROVIDER_NAME, "Email not verified");
            }

            return new OAuth2UserInfo(email, null, providerId, PROVIDER_NAME);

        } catch (OAuth2AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new OAuth2AuthenticationException(PROVIDER_NAME, "Token validation failed", e);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    private PublicKey getApplePublicKey(String kid) {
        try {
            String response = restTemplate.getForObject(APPLE_PUBLIC_KEYS_URL, String.class);
            JsonNode keysNode = objectMapper.readTree(response);
            JsonNode keys = keysNode.get("keys");

            for (JsonNode key : keys) {
                if (kid.equals(key.get("kid").asText())) {
                    String n = key.get("n").asText();
                    String e = key.get("e").asText();

                    byte[] nBytes = Base64.getUrlDecoder().decode(n);
                    byte[] eBytes = Base64.getUrlDecoder().decode(e);

                    BigInteger modulus = new BigInteger(1, nBytes);
                    BigInteger exponent = new BigInteger(1, eBytes);

                    RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
                    KeyFactory factory = KeyFactory.getInstance("RSA");

                    return factory.generatePublic(spec);
                }
            }

            throw new OAuth2AuthenticationException(PROVIDER_NAME, "Public key not found for kid: " + kid);

        } catch (Exception e) {
            throw new OAuth2AuthenticationException(PROVIDER_NAME, "Failed to get Apple public key", e);
        }
    }
}
