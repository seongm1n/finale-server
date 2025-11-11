package com.finale.finale.auth.controller;

import com.finale.finale.auth.dto.request.*;
import com.finale.finale.auth.dto.response.LoginResponse;
import com.finale.finale.auth.dto.response.LogoutResponse;
import com.finale.finale.auth.dto.response.RefreshResponse;
import com.finale.finale.auth.dto.response.UserResponse;
import com.finale.finale.book.service.StoryGenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.finale.finale.auth.service.AuthService;

import lombok.RequiredArgsConstructor;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final StoryGenerationService storyGenerationService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal Long userId) {
        UserResponse response = authService.getUserById(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/nickname")
    public ResponseEntity<UserResponse> setNickname(
            @Valid
            @AuthenticationPrincipal Long userId,
            @RequestBody NicknameRequest request
    ) {
        UserResponse response = authService.setNickname(userId, request.nickname());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(new LogoutResponse("로그아웃되었습니다"));
    }

    @DeleteMapping("/withdraw")
    public ResponseEntity<String> withdraw(@AuthenticationPrincipal Long userId) {
        authService.withdraw(userId);
        return ResponseEntity.ok("회원 탈퇴가 완료되었습니다.");
    }

    @PostMapping("/ability")
    public ResponseEntity<UserResponse> setAbility(
            @Valid
            @AuthenticationPrincipal Long userId,
            @RequestBody AbilityRequest request
    ) {
        UserResponse response = authService.setAbility(userId, request);
        storyGenerationService.generate(userId);
        return ResponseEntity.ok(response);
    }
}
