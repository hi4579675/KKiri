package com.kkiri.backend.auth.application.dto;

// 앱 시작마다 갱신하는 FCM 푸시 토큰
public record UpdateFcmTokenRequest(String fcmToken) {}
