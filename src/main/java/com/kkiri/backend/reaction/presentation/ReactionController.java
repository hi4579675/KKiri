package com.kkiri.backend.reaction.presentation;

import com.kkiri.backend.global.common.ApiResponse;
import com.kkiri.backend.global.exception.SuccessCode;
import com.kkiri.backend.global.security.LoginUser;
import com.kkiri.backend.reaction.application.ReactionService;
import com.kkiri.backend.reaction.application.dto.ReactionSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Reaction", description = "이모지 반응 API")
@RestController
@RequestMapping("/api/posts/{postId}/reactions")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;

    @Operation(
            summary = "반응 추가",
            description = "이미 같은 이모지로 반응한 경우 무시 (idempotent). emojiType은 URL 인코딩된 이모지 문자열"
    )
    @PostMapping("/{emojiType}")
    public ResponseEntity<ApiResponse<Void>> addReaction(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long postId,
            @PathVariable String emojiType
    ) {
        reactionService.addReaction(userId, postId, emojiType);
        return ApiResponse.onSuccess(SuccessCode.REACTION_ADDED, null);
    }

    @Operation(summary = "반응 취소", description = "반응이 없으면 무시 (idempotent)")
    @DeleteMapping("/{emojiType}")
    public ResponseEntity<ApiResponse<Void>> removeReaction(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long postId,
            @PathVariable String emojiType
    ) {
        reactionService.removeReaction(userId, postId, emojiType);
        return ApiResponse.onSuccess(SuccessCode.REACTION_REMOVED, null);
    }

    @Operation(
            summary = "반응 목록 조회",
            description = "이모지별 카운트 + 현재 유저의 반응 여부. reacted=true면 이미 누른 상태"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReactionSummaryResponse>>> getReactions(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long postId
    ) {
        return ApiResponse.onSuccess(SuccessCode.REACTIONS_FETCHED, reactionService.getReactions(userId, postId));
    }
}
