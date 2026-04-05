package com.kkiri.backend.auth.application.dto;

// 앱 → 서버: 카카오 로그인 시 카카오에서 받은 accessToken 전달
public record KakaoLoginRequest(String accessToken) {}