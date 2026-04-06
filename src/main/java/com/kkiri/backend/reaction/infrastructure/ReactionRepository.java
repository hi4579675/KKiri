package com.kkiri.backend.reaction.infrastructure;

import com.kkiri.backend.reaction.domain.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    // 포스트의 전체 반응 (이모지별 카운트 집계용)
    List<Reaction> findByPostId(Long postId);

    // 특정 유저가 해당 포스트에 한 반응 목록 (본인 반응 여부 판단용)
    List<Reaction> findByPostIdAndUserId(Long postId, Long userId);

    // 특정 유저의 특정 이모지 반응 조회 (중복 체크 + 취소 처리용)
    Optional<Reaction> findByPostIdAndUserIdAndEmojiType(Long postId, Long userId, String emojiType);
}
