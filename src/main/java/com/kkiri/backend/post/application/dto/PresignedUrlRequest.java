package com.kkiri.backend.post.application.dto;

import jakarta.validation.constraints.NotBlank;

public record PresignedUrlRequest(
        @NotBlank String fileName, // R2 저장 경로에 포함
        @NotBlank String contentType // R2가 Content-Type 검증용으로 씀
) {}
