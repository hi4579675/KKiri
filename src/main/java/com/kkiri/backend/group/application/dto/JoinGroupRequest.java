package com.kkiri.backend.group.application.dto;
import jakarta.validation.constraints.NotBlank;

public record JoinGroupRequest(
        @NotBlank String inviteCode
) {}