package com.finale.finale.auth.service;

import java.time.LocalDateTime;

import com.finale.finale.auth.dto.request.AbilityRequest;
import com.finale.finale.auth.dto.request.LoginRequest;
import com.finale.finale.auth.dto.request.RefreshRequest;
import com.finale.finale.auth.dto.response.LoginResponse;
import com.finale.finale.auth.dto.response.RefreshResponse;
import com.finale.finale.auth.dto.response.UserResponse;
import com.finale.finale.exception.CustomException;
import com.finale.finale.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finale.finale.config.JwtTokenProvider;
import com.finale.finale.auth.domain.OAuthProvider;
import com.finale.finale.auth.domain.RefreshToken;
import com.finale.finale.auth.domain.User;
import com.finale.finale.auth.oauth.OAuth2UserInfo;
import com.finale.finale.auth.oauth.OAuth2ValidatorFactory;
import com.finale.finale.auth.repository.OAuthProviderRepository;
import com.finale.finale.auth.repository.RefreshTokenRepository;
import com.finale.finale.auth.repository.UserRepository;
import com.finale.finale.book.repository.BookRepository;
import com.finale.finale.book.repository.PhraseRepository;
import com.finale.finale.book.repository.QuizRepository;
import com.finale.finale.book.repository.SentenceRepository;
import com.finale.finale.book.repository.UnknownWordRepository;
import com.finale.finale.book.repository.WordRepository;

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
    private final BookRepository bookRepository;
    private final SentenceRepository sentenceRepository;
    private final QuizRepository quizRepository;
    private final UnknownWordRepository unknownWordRepository;
    private final PhraseRepository phraseRepository;
    private final WordRepository wordRepository;

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
            .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_INVALID));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        User user = refreshToken.getUser();

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        refreshTokenRepository.delete(refreshToken);
        saveRefreshToken(user, newRefreshToken);

        return new RefreshResponse(
                newAccessToken,
                newRefreshToken,
                toUserResponse(user)
                );
    }

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return toUserResponse(user);
    }

    public UserResponse setNickname(Long userId, String nickname) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (user.getNickname() != null && user.getNickname().equals(nickname)) {
            throw new CustomException(ErrorCode.NICKNAME_SAME_AS_BEFORE);
        }
        if (userRepository.existsByNickname(nickname)) {
            throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }
        user.setNickname(nickname);
        userRepository.save(user);

        return toUserResponse(user);
    }

    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        var books = bookRepository.findAllByUser(user);

        for (var book : books) {
            book.getReviewWords().clear();

            var sentences = sentenceRepository.findAllByBook(book);

            for (var sentence : sentences) {
                phraseRepository.deleteAllBySentence(sentence);
                wordRepository.deleteAllBySentence(sentence);
            }

            quizRepository.deleteAll(quizRepository.findAllByBook(book));
            sentenceRepository.deleteAll(sentences);
        }

        unknownWordRepository.deleteAllByUserId(userId);
        bookRepository.deleteAll(books);

        oauthProviderRepository.findByUser(user)
            .ifPresent(oauthProviderRepository::delete);

        refreshTokenRepository.deleteByUser(user);

        userRepository.delete(user);
    }

    public void logout(String token) {
        refreshTokenRepository.findByToken(token)
            .ifPresent(refreshTokenRepository::delete);
    }

    public UserResponse setAbility(Long userId, AbilityRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.initAbilityScore(request.abilityScore());
        userRepository.save(user);

        return toUserResponse(user);
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
            user.getImageCategory().name(),
            user.getRole(),
            user.getAbilityScore(),
            user.getBookReadCount(),
            user.needsNickname()
        );
    }
}
