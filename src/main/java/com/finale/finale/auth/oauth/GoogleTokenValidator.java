package com.finale.finale.auth.oauth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

@Component
public class GoogleTokenValidator implements OAuth2TokenValidator {

    private static final String PROVIDER_NAME = "google";

    @Value("${oauth2.google.android-client-id:}")
    private String androidClientId;

    @Value("${oauth2.google.ios-client-id:}")
    private String iosClientId;

    @Value("${oauth2.google.web-client-id:}")
    private String webClientId;

    @Override
    public OAuth2UserInfo validate(String idToken) {
        try {
            List<String> audiences = new ArrayList<>();
            if (androidClientId != null && !androidClientId.isEmpty()) {
                audiences.add(androidClientId);
            }
            if (iosClientId != null && !iosClientId.isEmpty()) {
                audiences.add(iosClientId);
            }
            if (webClientId != null && !webClientId.isEmpty()) {
                audiences.add(webClientId);
            }

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
            .setAudience(audiences)
            .build();

            GoogleIdToken googleIdToken = verifier.verify(idToken);

            if (googleIdToken == null) {
                throw new OAuth2AuthenticationException(PROVIDER_NAME, "Invalid token");
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();

            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String providerId = payload.getSubject();

            Boolean emailVerified = payload.getEmailVerified();
            if (emailVerified == null || !emailVerified) {
                throw new OAuth2AuthenticationException(PROVIDER_NAME, "Invalid email");
            }

            return new OAuth2UserInfo(email, name, providerId, PROVIDER_NAME);
        } catch (GeneralSecurityException | IOException e) {
            throw new OAuth2AuthenticationException(PROVIDER_NAME, "Invalid token", e);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
