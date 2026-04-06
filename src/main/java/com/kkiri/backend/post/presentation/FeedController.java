package com.kkiri.backend.post.presentation;

import com.kkiri.backend.global.common.ApiResponse;
import com.kkiri.backend.global.exception.SuccessCode;
import com.kkiri.backend.global.security.LoginUser;
import com.kkiri.backend.post.application.FeedService;
import com.kkiri.backend.post.application.dto.FeedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Feed", description = "피드 관련 API")
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @Operation(
            summary = "오늘의 피드 조회",
            description = "시간대(hourBucket)별로 그룹핑된 피드. cursor 없으면 현재 UTC 시각부터 내림차순 조회"
    )
    @GetMapping("/{groupId}/feed")
    public ResponseEntity<ApiResponse<FeedResponse>> getFeed(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long groupId,
            @RequestParam(required = false) Integer cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.onSuccess(SuccessCode.FEED_FETCHED, feedService.getFeed(userId, groupId, cursor, size));
    }
}
