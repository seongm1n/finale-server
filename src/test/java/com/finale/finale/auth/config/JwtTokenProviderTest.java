package com.finale.finale.auth.config;

import com.finale.finale.config.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("JWT 토큰 제공자 테스트")
class JwtTokenProviderTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Long testUserId;
    private String testEmail;
    private String testRole;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testEmail = "test@example.com";
        testRole = "USER";
    }

    @Test
    @DisplayName("액세스 토큰 생성 성공")
    void generateAccessToken_ShouldReturnValidToken() {
        // Given
        Long userId = testUserId;
        String email = testEmail;
        String role = testRole;

        // When
        String token = jwtTokenProvider.generateAccessToken(userId, email, role);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("리프레시 토큰 생성 성공")
    void generateRefreshToken_ShouldReturnValidToken() {
        // Given
        Long userId = testUserId;

        // When
        String token = jwtTokenProvider.generateRefreshToken(userId);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("유효한 엑세스 토큰에서 사용자 ID 추출 성공")
    void getUserIdFromToken_WithValidToken_ShouldReturnUserId() {
        // Givne
        String token = jwtTokenProvider.generateAccessToken(testUserId, testEmail, testRole);

        // When
        Long extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

        // Then
        assertThat(extractedUserId).isEqualTo(testUserId);
    }

    @Test
    @DisplayName("유효한 토큰에서 이메일 추출 성공")
    void getEmailFromToken_WithValidToken_ShouldReturnEmail() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUserId, testEmail, testRole);

        // When
        String extractedEmail = jwtTokenProvider.getEmailFromToken(token);

        // Then
        assertThat(extractedEmail).isEqualTo(testEmail);
    }

    @Test
    @DisplayName("유효한 토큰에서 권한 추출 성공")
    void getRoleFromToken_WithValidToken_ShouldReturnRole() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUserId, testEmail, testRole);

        // When
        String extractedRole = jwtTokenProvider.getRoleFromToken(token);

        // Then
        assertThat(extractedRole).isEqualTo(testRole);
    }

    @Test
    @DisplayName("유효한 토큰 검증 성공")
    void validateToken_WithValidToken_ShouldReturnTrue() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUserId, testEmail, testRole);

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("변조된 토큰 검증 실패")
    void validateToken_WithTamperedToken_ShouldReturnFalse() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUserId, testEmail, testRole);
        String tamperedToken = token + "tampered";

        // When
        boolean isValid = jwtTokenProvider.validateToken(tamperedToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("다른 시크릿 키로 생성된 토큰 검증 실패")
    void validateToken_WithDifferentSecretKey_ShouldReturnFalse() {
        // Given
        SecretKey differentKey = Keys.hmacShaKeyFor("test-secret-key-for-junit-testing".getBytes());
        String tokenWithDifferentKey = Jwts.builder()
                .subject(String.valueOf(testUserId))
                .claim("email", testEmail)
                .claim("role", testRole)
                .signWith(differentKey)
                .compact();

        // When
        boolean isValid = jwtTokenProvider.validateToken(tokenWithDifferentKey);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("리프레시 토큰에서 사용자 ID 추출 성공")
    void getUserIdFromToken_WithRefreshToken_ShouldReturnUserId() {
        // Given
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUserId);

        // When
        Long extractedUserId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        // Then
        assertThat(extractedUserId).isEqualTo(testUserId);
    }
}
