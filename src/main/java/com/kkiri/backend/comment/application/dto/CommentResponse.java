package com.kkiri.backend.comment.application.dto;

import com.kkiri.backend.comment.domain.Comment;

import java.time.LocalDateTime;

public record CommentResponse(
        Long commentId,
        Long userId,
        String nickname,
        String avatarEmoji,
        String avatarColor,
        String content,
        LocalDateTime createdAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getUser().getId(),
                comment.getUser().getNickname(),
                comment.getUser().getAvatarEmoji(),
                comment.getUser().getAvatarColor(),
                comment.getContent(),
                comment.getCreateAudit().getCreatedAt()
        );
    }
}
