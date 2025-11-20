package com.finale.finale.auth.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RefreshToken 엔티티 테스트")
public class RefreshTokenTest {

    @Test
    @DisplayName("RefreshToken 생성")
    void createRefreshToken() {
        // Given
        User user = new User("test@example.com");
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        // When
        RefreshToken token = new RefreshToken(user, "token-string", expiresAt);

        // Then
        assertThat(token.getUser()).isEqualTo(user);
        assertThat(token.getToken()).isEqualTo("token-string");
        assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("토큰 만료 여부 확인 - 만료되지 않음")
    void isNotExpired() {
        // Given
        User user = new User("test@example.com");
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        RefreshToken token = new RefreshToken(user, "token-string", expiresAt);

        // When & Then
        assertThat(token.isExpired()).isFalse();
    }

    @Test
    @DisplayName("토큰 만료 여부 확인 - 만료됨")
    void isExpired() {
        // Given
        User user = new User("test@example.com");
        LocalDateTime expiresAt = LocalDateTime.now().minusDays(1);
        RefreshToken token = new RefreshToken(user, "token-string", expiresAt);

        // When & Then
        assertThat(token.isExpired()).isTrue();
    }
}
