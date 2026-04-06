package com.kkiri.backend.post.application.dto;

public record PresignedUrlResponse(
        String presignedUrl, // 업로드 요청
        String imageKey, // 업로드 완료 후 createPost 요청 때 이걸 다시 서버에 보냄
        String imageUrl // 완성 후 화면에 보여줄 URL
) {}
