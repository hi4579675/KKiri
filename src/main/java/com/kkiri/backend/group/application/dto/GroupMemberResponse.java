package com.kkiri.backend.group.application.dto;

import com.kkiri.backend.group.domain.GroupMember;

import java.time.LocalDateTime;

public record GroupMemberResponse(
        Long userId,
        String nickname,
        String avatarEmoji,
        String avatarColor,
        String role,
        LocalDateTime joinedAt
) {
    public static GroupMemberResponse from(GroupMember gm) {
        return new GroupMemberResponse(
                gm.getUser().getId(),
                gm.getUser().getNickname(),
                gm.getUser().getAvatarEmoji(),
                gm.getUser().getAvatarColor(),
                gm.getRole().name(),
                gm.getJoinedAt()
        );
    }
}
