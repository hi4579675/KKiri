package com.kkiri.backend.auth.application.dto;

// 프로필 설정 요청 (nickname + 아바타 이모지 + 아바타 색상 항상 같이 전달)
public record CompleteProfileRequest(
        String nickname,
        String avatarEmoji,
        String avatarColor
) {}