package com.kkiri.backend.comment.presentation;

import com.kkiri.backend.comment.application.CommentService;
import com.kkiri.backend.comment.application.dto.AddCommentRequest;
import com.kkiri.backend.comment.application.dto.CommentResponse;
import com.kkiri.backend.comment.application.dto.SliceResponse;
import com.kkiri.backend.global.common.ApiResponse;
import com.kkiri.backend.global.exception.SuccessCode;
import com.kkiri.backend.global.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Comment", description = "댓글 API")
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(
            summary = "댓글 목록 조회",
            description = "커서 기반 페이지네이션 (오래된 순 ASC, 기본 20개). cursor 없으면 처음부터"
    )
    @GetMapping("/api/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<SliceResponse<CommentResponse>>> getComments(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long postId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.COMMENTS_FETCHED,
                commentService.getComments(userId, postId, cursor, size)
        );
    }

    @Operation(summary = "댓글 추가")
    @PostMapping("/api/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody AddCommentRequest request
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.COMMENT_CREATED,
                commentService.addComment(userId, postId, request)
        );
    }

    @Operation(summary = "댓글 삭제", description = "본인 댓글만 삭제 가능")
    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long commentId
    ) {
        commentService.deleteComment(userId, commentId);
        return ApiResponse.onSuccess(SuccessCode.COMMENT_DELETED, null);
    }
}
