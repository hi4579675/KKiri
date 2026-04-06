package com.kkiri.backend.group.application.dto;

public record JoinGroupResponse(
        Long groupId,
        String name,
        long memberCount
) {}