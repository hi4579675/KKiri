package com.kkiri.backend.post.presentation;

import com.kkiri.backend.global.common.ApiResponse;
import com.kkiri.backend.global.exception.SuccessCode;
import com.kkiri.backend.global.security.LoginUser;
import com.kkiri.backend.post.application.PostService;
import com.kkiri.backend.post.application.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Post", description = "포스트 관련 API")
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "presigned URL 발급", description = "클라이언트가 R2에 직접 업로드할 수 있는 임시 URL 발급 (15분 유효)")
    @PostMapping("/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> generatePresignedUrl(
            @Parameter(hidden = true) @LoginUser Long userId,
            @Valid @RequestBody PresignedUrlRequest req
    ) {
        return ApiResponse.onSuccess(SuccessCode.PRESIGNED_URL_ISSUED, postService.generatePresignedUrl(userId, req));
    }

    @Operation(summary = "포스트 생성", description = "R2 업로드 완료 후 imageKey + caption으로 포스트 저장. FCM 푸시 자동 발송")
    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @Parameter(hidden = true) @LoginUser Long userId,
            @Valid @RequestBody CreatePostRequest req
    ) {
        return ApiResponse.onSuccess(SuccessCode.POST_CREATED, postService.createPost(userId, req));
    }

    @Operation(summary = "포스트 삭제", description = "본인 포스트 + 당일 활성 포스트만 삭제 가능. R2 이미지 비동기 삭제")
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long postId
    ) {
        postService.deletePost(userId, postId);
        return ApiResponse.onSuccess(SuccessCode.POST_DELETED, null);
    }
}
