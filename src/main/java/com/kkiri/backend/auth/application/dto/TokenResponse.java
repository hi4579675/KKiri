package com.kkiri.backend.auth.application.dto;

// 서버 → 앱: 로그인/재발급 응답
// profileCompleted = false이면 앱에서 프로필 설정 화면으로 이동
public record TokenResponse(
        String accessToken,
        String refreshToken,
        boolean profileCompleted
) {}
