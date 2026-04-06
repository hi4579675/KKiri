package com.kkiri.backend.reaction.application.dto;

public record ReactionSummaryResponse(
        String emojiType,  // "❤️", "😂", "😮" 등
        long count,        // 해당 이모지 반응 수
        boolean reacted    // 현재 로그인 유저가 이 이모지로 반응했는지
) {}
