package com.kkiri.backend.auth.application.dto;

// 앱 → 서버: Access Token 재발급 시 Refresh Token 전달
public record RefreshRequest(String refreshToken) {}
