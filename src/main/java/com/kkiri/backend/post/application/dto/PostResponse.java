package com.kkiri.backend.post.application.dto;

import com.kkiri.backend.post.domain.Post;

import java.time.LocalDateTime;

public record PostResponse(
        Long postId,
        Long userId,
        String nickname,
        String avatarEmoji,
        String avatarColor,
        String imageUrl,
        String caption,
        int hourBucket,
        LocalDateTime createdAt
) {
    // 엔티티엔 DB 관련 필드가 섞여있어서 팩토리 메서드로 변환
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getUser().getId(),
                post.getUser().getNickname(),
                post.getUser().getAvatarEmoji(),
                post.getUser().getAvatarColor(),
                post.getImageUrl(),
                post.getCaption(),
                post.getHourBucket(),
                post.getCreateAudit().getCreatedAt()
        );
    }
}
