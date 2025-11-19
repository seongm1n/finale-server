package com.finale.finale.auth.service;

import com.finale.finale.auth.domain.OAuthProvider;
import com.finale.finale.auth.domain.RefreshToken;
import com.finale.finale.auth.domain.User;
import com.finale.finale.auth.dto.request.AbilityRequest;
import com.finale.finale.auth.dto.request.LoginRequest;
import com.finale.finale.auth.dto.request.RefreshRequest;
import com.finale.finale.auth.dto.response.LoginResponse;
import com.finale.finale.auth.dto.response.NicknameCheckResponse;
import com.finale.finale.auth.dto.response.RefreshResponse;
import com.finale.finale.auth.dto.response.UserResponse;
import com.finale.finale.auth.oauth.OAuth2TokenValidator;
import com.finale.finale.auth.oauth.OAuth2UserInfo;
import com.finale.finale.auth.oauth.OAuth2ValidatorFactory;
import com.finale.finale.auth.repository.OAuthProviderRepository;
import com.finale.finale.auth.repository.RefreshTokenRepository;
import com.finale.finale.auth.repository.UserRepository;
import com.finale.finale.book.repository.BookRepository;
import com.finale.finale.book.repository.PhraseRepository;
import com.finale.finale.book.repository.QuizRepository;
import com.finale.finale.book.repository.SentenceRepository;
import com.finale.finale.book.repository.UnknownPhraseRepository;
import com.finale.finale.book.repository.UnknownWordRepository;
import com.finale.finale.book.repository.WordRepository;
import com.finale.finale.config.JwtTokenProvider;
import com.finale.finale.exception.CustomException;
import com.finale.finale.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
public class AuthServiceTest {

    @Mock
    private OAuth2ValidatorFactory validatorFactory;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OAuthProviderRepository oauthProviderRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private OAuth2TokenValidator tokenValidator;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private SentenceRepository sentenceRepository;

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private UnknownWordRepository unknownWordRepository;

    @Mock
    private UnknownPhraseRepository unknownPhraseRepository;

    @Mock
    private PhraseRepository phraseRepository;

    @Mock
    private WordRepository wordRepository;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800000L);
    }

    @Test
    @DisplayName("로그인 - 신규 유저")
    void loginNewUser() {
        // Given
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800000L);

        LoginRequest request = new LoginRequest("GOOGLE", "id-token", null);
        OAuth2UserInfo userInfo = new OAuth2UserInfo(
                "new@example.com",
                "Test User",
                "google-123",
                "GOOGLE"
        );

        User savedUser = new User("new@example.com");
        ReflectionTestUtils.setField(savedUser, "id", 1L);

        given(validatorFactory.getValidator("GOOGLE")).willReturn(tokenValidator);
        given(tokenValidator.validate("id-token")).willReturn(userInfo);
        given(userRepository.findByEmail("new@example.com")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtTokenProvider.generateAccessToken(1L, "new@example.com", "USER"))
                .willReturn("access-token");
        given(jwtTokenProvider.generateRefreshToken(1L)).willReturn("refresh-token");

        // When
        LoginResponse response = authService.login(request);

        // Then
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().email()).isEqualTo("new@example.com");

        verify(userRepository).save(any(User.class));
        verify(oauthProviderRepository).save(any(OAuthProvider.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("로그인 - 기존 유저")
    void loginExistingUser() {
        // Given
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800000L);

        LoginRequest request = new LoginRequest("GOOGLE", "id-token", null);
        OAuth2UserInfo userInfo = new OAuth2UserInfo(
                "existing@example.com",
                "Existing User",
                "google-123",
                "GOOGLE"
        );

        User existingUser = new User("existing@example.com");
        ReflectionTestUtils.setField(existingUser, "id", 1L);

        given(validatorFactory.getValidator("GOOGLE")).willReturn(tokenValidator);
        given(tokenValidator.validate("id-token")).willReturn(userInfo);

        given(userRepository.findByEmail("existing@example.com")).willReturn(Optional.of(existingUser));
        given(jwtTokenProvider.generateAccessToken(1L, "existing@example.com", "USER"))
                .willReturn("access-token");
        given(jwtTokenProvider.generateRefreshToken(1L)).willReturn("refresh-token");

        // When
        LoginResponse response = authService.login(request);

        // Then
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.user().email()).isEqualTo("existing@example.com");

        verify(userRepository, never()).save(any(User.class));
        verify(oauthProviderRepository, never()).save(any(OAuthProvider.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("토큰 갱신 - 성공")
    void refreshSuccess() {
        // Given
        User user = new User("test@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800000L);

        RefreshToken oldToken = new RefreshToken(
                user,
                "old-refresh-token",
                LocalDateTime.now().plusDays(7)
        );

        RefreshRequest request = new RefreshRequest("old-refresh-token");

        given(refreshTokenRepository.findByToken("old-refresh-token"))
                .willReturn(Optional.of(oldToken));
        given(jwtTokenProvider.generateAccessToken(1L, "test@example.com", "USER"))
                .willReturn("new-access-token");
        given(jwtTokenProvider.generateRefreshToken(1L))
                .willReturn("new-refresh-token");

        // When
        RefreshResponse response = authService.refresh(request);

        // Then
        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");

        verify(refreshTokenRepository).delete(oldToken);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("토큰 갱신 - 완료된 토큰")
    void refreshExpiredToken() {
        // Given
        User user = new User("test@example.com");
        RefreshToken expiredToken = new RefreshToken(
                user,
                "expired-token",
                LocalDateTime.now().minusDays(1)
        );

        RefreshRequest request = new RefreshRequest("expired-token");

        given(refreshTokenRepository.findByToken("expired-token"))
                .willReturn(Optional.of(expiredToken));

        // When & Then
        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_EXPIRED);

        verify(refreshTokenRepository).delete(expiredToken);
    }

    @Test
    @DisplayName("닉네임 설정 - 성공")
    void setNickname() {
        // Given
        User user = new User("test@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("TestNick")).willReturn(false);
        given(userRepository.save(user)).willReturn(user);

        // When
        UserResponse response = authService.setNickname(1L, "TestNick");

        // Then
        assertThat(response.nickname()).isEqualTo("TestNick");
        assertThat(user.getNickname()).isEqualTo("TestNick");
        verify(userRepository).existsByNickname("TestNick");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("닉네임 설정 - 중복된 닉네임")
    void setNicknameDuplicate() {
        // Given
        User user = new User("test@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("ExistingNick")).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.setNickname(1L, "ExistingNick"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NICKNAME_ALREADY_EXISTS);

        verify(userRepository).existsByNickname("ExistingNick");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("닉네임 설정 - 기존과 동일한 닉네임")
    void setNicknameSameAsBefore() {
        // Given
        User user = new User("test@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        user.setNickname("SameNick");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> authService.setNickname(1L, "SameNick"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NICKNAME_SAME_AS_BEFORE);

        verify(userRepository, never()).existsByNickname(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("능력 점수 초기화")
    void setAbility() {
        // Given
        User user = new User("test@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);

        AbilityRequest request = new AbilityRequest(700);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.save(user)).willReturn(user);

        // When
        UserResponse response = authService.setAbility(1L, request);

        // Then
        assertThat(response.abilityScore()).isEqualTo(700);
        assertThat(user.getAbilityScore()).isEqualTo(700);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("회원 탈퇴 - 사용자 없음")
    void withdrawUserNotFound() {
        // Given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.withdraw(999L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("회원 탈퇴 - 성공")
    void withdrawSuccess() {
        // Given
        User user = new User("test@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);

        OAuthProvider oauthProvider = new OAuthProvider(user, "GOOGLE", "google-123");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(bookRepository.findAllByUser(user)).willReturn(java.util.List.of());
        given(oauthProviderRepository.findByUser(user)).willReturn(Optional.of(oauthProvider));

        // When
        authService.withdraw(1L);

        // Then
        verify(unknownWordRepository).deleteAllByUserId(1L);
        verify(bookRepository).deleteAll(any());
        verify(oauthProviderRepository).delete(oauthProvider);
        verify(refreshTokenRepository).deleteByUser(user);
        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("닉네임 중복 확인 - 사용 가능")
    void checkNicknameAvailable() {
        // Given
        given(userRepository.existsByNickname("NewNick")).willReturn(false);

        // When
        NicknameCheckResponse response = authService.checkNickname("NewNick");

        // Then
        assertThat(response.isAvailable()).isTrue();
        assertThat(response.nickname()).isEqualTo("NewNick");
        verify(userRepository).existsByNickname("NewNick");
    }

    @Test
    @DisplayName("닉네임 중복 확인 - 사용 불가")
    void checkNicknameUnavailable() {
        // Given
        given(userRepository.existsByNickname("ExistingNick")).willReturn(true);

        // When
        NicknameCheckResponse response = authService.checkNickname("ExistingNick");

        // Then
        assertThat(response.isAvailable()).isFalse();
        assertThat(response.nickname()).isEqualTo("ExistingNick");
        assertThat(response.message()).isEqualTo("이미 사용 중인 닉네임입니다.");
        verify(userRepository).existsByNickname("ExistingNick");
    }
}
