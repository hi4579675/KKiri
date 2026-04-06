package com.kkiri.backend.comment.application.dto;

import java.util.List;

// 커서 기반 페이지네이션 응답 공통 래퍼
// size+1개를 조회해서 hasNext를 판단하는 방식
// nextCursor = null이면 마지막 페이지
public record SliceResponse<T>(
        List<T> content,
        Long nextCursor,
        boolean hasNext
) {}
