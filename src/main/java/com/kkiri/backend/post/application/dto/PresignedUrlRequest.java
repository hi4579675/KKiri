package com.kkiri.backend.post.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PresignedUrlRequest(
        @NotNull Long groupId,       // 저장 경로에 포함 (groups/{groupId}/...)
        @NotBlank String fileName,   // R2 저장 경로에 포함
        @NotBlank String contentType // R2가 Content-Type 검증용으로 씀
) {}
