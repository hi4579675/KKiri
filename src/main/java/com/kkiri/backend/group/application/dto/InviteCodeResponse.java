package com.kkiri.backend.group.application.dto;

import com.kkiri.backend.group.domain.Group;
import java.time.LocalDateTime;

public record InviteCodeResponse(
        Long groupId,
        String inviteCode,
        LocalDateTime inviteCodeExpiredAt
) {
    public static InviteCodeResponse from(Group group) {
        return new InviteCodeResponse(
                group.getId(),
                group.getInviteCode(),
                group.getInviteCodeExpiredAt()
        );
    }
}