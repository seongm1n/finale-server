package com.finale.finale.auth.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finale.finale.auth.config.JwtTokenProvider;
import com.finale.finale.auth.domain.OAuthProvider;
import com.finale.finale.auth.domain.RefreshToken;
import com.finale.finale.auth.domain.User;
import com.finale.finale.auth.dto.LoginRequest;
import com.finale.finale.auth.dto.LoginResponse;
import com.finale.finale.auth.dto.RefreshRequest;
import com.finale.finale.auth.dto.RefreshResponse;
import com.finale.finale.auth.dto.UserResponse;
import com.finale.finale.auth.oauth.OAuth2AuthenticationException;
import com.finale.finale.auth.oauth.OAuth2UserInfo;
import com.finale.finale.auth.oauth.OAuth2ValidatorFactory;
import com.finale.finale.auth.repository.OAuthProviderRepository;
import com.finale.finale.auth.repository.RefreshTokenRepository;
import com.finale.finale.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final OAuth2ValidatorFactory validatorFactory;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final OAuthProviderRepository oauthProviderRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    public LoginResponse login(LoginRequest request) {
        String token = request.idToken() != null ? request.idToken() : request.authCode();

        OAuth2UserInfo oauth2UserInfo = validatorFactory
            .getValidator(request.provider())
            .validate(token);

        User user = userRepository.findByEmail(oauth2UserInfo.email())
            .orElseGet(() -> createNewUser(oauth2UserInfo));

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        saveRefreshToken(user, refreshToken);

        return new LoginResponse(accessToken, refreshToken, toUserResponse(user));
    }

    public RefreshResponse refresh(RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
            .orElseThrow(() -> new OAuth2AuthenticationException("refresh", "Invalid refresh token"));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new OAuth2AuthenticationException("refresh", "Expired refresh token");
        }

        User user = refreshToken.getUser();

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        refreshTokenRepository.delete(refreshToken);
        saveRefreshToken(user, newRefreshToken);

        return new RefreshResponse(newAccessToken, newRefreshToken);
    }

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new OAuth2AuthenticationException("user", "User not found"));
        return toUserResponse(user);
    }

    public UserResponse setNickname(Long userId, String nickname) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new OAuth2AuthenticationException("user", "User not found"));
        user.setNickname(nickname);
        userRepository.save(user);

        return toUserResponse(user);
    }

    public void logout(String token) {
        refreshTokenRepository.findByToken(token)
            .ifPresent(refreshTokenRepository::delete);
    }

    private User createNewUser(OAuth2UserInfo oauth2UserInfo) {
        User user = new User(oauth2UserInfo.email());
        user = userRepository.save(user);

        OAuthProvider oauthProvider = new OAuthProvider(
            user,
            oauth2UserInfo.provider(),
            oauth2UserInfo.providerId()
        );
        oauthProviderRepository.save(oauthProvider);

        return user;
    }

    private void saveRefreshToken(User user, String token) {
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000);
        RefreshToken refreshToken = new RefreshToken(user, token, expiresAt);
        refreshTokenRepository.save(refreshToken);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            user.getRole(),
            user.getAbilityScore(),
            user.getEffortScore(),
            user.getBookReadCount(),
            user.needsNickname()
        );
    }
}
