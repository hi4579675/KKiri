package com.kkiri.backend.post.application.dto;

import java.util.List;

public record FeedResponse(
        List<ContributorResponse> contributors, // 그룹 전체 멤버(아바타 바)
        List<BucketResponse> buckets, // 시간대별 포스트 묶음
        Integer nextCursor // 다음 페이지 커서
) {}
