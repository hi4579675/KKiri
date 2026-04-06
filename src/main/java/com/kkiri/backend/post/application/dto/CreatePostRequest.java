package com.kkiri.backend.post.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotNull Long groupId,
        @NotBlank String imageKey,
        @Size(max = 30) String caption
) {}
