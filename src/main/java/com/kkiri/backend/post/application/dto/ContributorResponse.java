package com.kkiri.backend.post.application.dto;

import com.kkiri.backend.auth.domain.User;

// 아바타 바 한 명
public record ContributorResponse(
        Long userId,
        String nickname,
        String avatarEmoji,
        String avatarColor,
        boolean hasPostedToday    // 오늘 포스트 올렸으면 true

) {
    public static ContributorResponse of(User user, boolean hasPostedToday) {
        return new ContributorResponse(
                user.getId(),
                user.getNickname(),
                user.getAvatarEmoji(),
                user.getAvatarColor(),
                hasPostedToday
        );
    }
}
