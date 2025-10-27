package com.finale.finale.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finale.finale.auth.dto.LoginRequest;
import com.finale.finale.auth.dto.LoginResponse;
import com.finale.finale.auth.dto.LogoutRequest;
import com.finale.finale.auth.dto.LogoutResponse;
import com.finale.finale.auth.dto.NicknameRequest;
import com.finale.finale.auth.dto.RefreshRequest;
import com.finale.finale.auth.dto.RefreshResponse;
import com.finale.finale.auth.dto.UserResponse;
import com.finale.finale.auth.service.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@RequestBody RefreshRequest request) {
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
        @AuthenticationPrincipal Long userId,
        @RequestBody NicknameRequest request
    ) {
        UserResponse response = authService.setNickname(userId, request.nickname());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(new LogoutResponse("로그아웃되었습니다"));
    }
}
