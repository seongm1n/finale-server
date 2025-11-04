package com.finale.finale.auth.config;

import com.finale.finale.config.JwtAuthenticationFilter;
import com.finale.finale.config.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT 인증 필터 테스트")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("유효한 JWT 토큰이 있으면 인증 정보가 SecurityContext에 설정됨")
    void doFilterInternal_WithValidToken_ShouldSetAuthentication() throws ServletException, IOException {
        // Given
        String validToken = "valid.jwt.token";
        Long userId = 1L;
        String email = "test@example.com";
        String role = "USER";

        request.addHeader("Authorization", "Bearer " + validToken);

        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(validToken)).thenReturn(userId);
        when(jwtTokenProvider.getEmailFromToken(validToken)).thenReturn(email);
        when(jwtTokenProvider.getRoleFromToken(validToken)).thenReturn(role);

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(userId);
        assertThat(authentication.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactly("ROLE_USER");

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 인증 정보가 설정되지 않음")
    void doFilterInternal_WithoutAuthorizationHeader_ShouldNotSetAuthentication() throws ServletException, IOException {
        // Given

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(jwtTokenProvider, never()).validateToken(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Bearer로 시작하지 않는 토큰은 무시됨")
    void doFilterInternal_WithoutBearerPrefix_ShouldNotSetAuthentication() throws ServletException, IOException {
        // Given
        request.addHeader("Authorization", "InvalidPrefix token");

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(jwtTokenProvider, never()).validateToken(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("토큰 파싱 중 예외가 발생해도 필터 체인은 계속 진행됨")
    void doFilterInternal_WithExceptionDuringParsing_ShouldContinueFilterChain() throws ServletException, IOException {
        // Given
        String token = "exception.throwing.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(token)).thenThrow(new RuntimeException("Token Parsing error"));

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("ADMIN 역할을 가진 토큰은 ROLE_ADMIN 권한이 설정됨")
    void doFilterInternal_WithAdminRole_ShouldSetAdminAuthority() throws ServletException,
            IOException {
        // Given
        String validToken = "valid.jwt.token";
        Long userId = 2L;
        String email = "admin@example.com";
        String role = "ADMIN";

        request.addHeader("Authorization", "Bearer " + validToken);

        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(validToken)).thenReturn(userId);
        when(jwtTokenProvider.getEmailFromToken(validToken)).thenReturn(email);
        when(jwtTokenProvider.getRoleFromToken(validToken)).thenReturn(role);

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(userId);
        assertThat(authentication.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactly("ROLE_ADMIN");

        verify(filterChain, times(1)).doFilter(request, response);
    }
}
