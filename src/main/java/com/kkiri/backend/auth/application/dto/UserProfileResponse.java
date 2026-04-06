package com.kkiri.backend.auth.application.dto;

import com.kkiri.backend.auth.domain.User;

public record UserProfileResponse(
        Long userId,
        String nickname,
        String avatarEmoji,
        String avatarColor
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getAvatarEmoji(),
                user.getAvatarColor()
        );
    }
}
