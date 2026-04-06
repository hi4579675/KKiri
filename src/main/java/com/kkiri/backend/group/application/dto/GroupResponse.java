package com.kkiri.backend.group.application.dto;

import com.kkiri.backend.group.domain.Group;
import java.time.LocalDateTime;

public record GroupResponse(
        Long groupId,
        String name,
        String inviteCode,
        LocalDateTime inviteCodeExpiredAt,
        int maxMembers
) {
    public static GroupResponse from(Group group) {
        return new GroupResponse(
                group.getId(),
                group.getName(),
                group.getInviteCode(),
                group.getInviteCodeExpiredAt(),
                group.getMaxMembers()
        );
    }
}