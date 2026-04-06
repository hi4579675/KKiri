package com.kkiri.backend.auth.application.dto;

// 웹/앱에서 인가 코드를 받아 서버에서 카카오 토큰 교환
public record KakaoCodeLoginRequest(String code, String redirectUri) {}
