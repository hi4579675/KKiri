package com.kkiri.backend.auth.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 10) String nickname,
        @NotBlank String avatarEmoji,
        @NotBlank String avatarColor
) {}
