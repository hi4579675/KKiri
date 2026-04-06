package com.kkiri.backend.post.application.dto;

import java.util.List;
// 시간대 하나
public record BucketResponse(
        int hourBucket, // // 14, 12, 9 ... (0~23)
        List<PostResponse> posts  // 해당 시간대의 포스트들
) {}
