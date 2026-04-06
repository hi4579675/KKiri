package com.kkiri.backend.reaction.application;

import com.kkiri.backend.auth.domain.User;
import com.kkiri.backend.auth.infrastructure.UserRepository;
import com.kkiri.backend.global.exception.CustomException;
import com.kkiri.backend.global.exception.ErrorCode;
import com.kkiri.backend.group.infrastructure.GroupMemberRepository;
import com.kkiri.backend.post.domain.Post;
import com.kkiri.backend.post.infrastructure.PostRepository;
import com.kkiri.backend.reaction.application.dto.ReactionSummaryResponse;
import com.kkiri.backend.reaction.domain.Reaction;
import com.kkiri.backend.reaction.infrastructure.ReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;

    // 반응 추가 (idempotent — 이미 같은 이모지 반응이 있으면 무시)
    @Transactional
    public void addReaction(Long userId, Long postId, String emojiType) {
        Post post = getPost(postId);
        User user = getUser(userId);
        validateMember(post, userId);

        // 이미 존재하면 그냥 반환 (중복 요청에 안전)
        boolean alreadyExists = reactionRepository
                .findByPostIdAndUserIdAndEmojiType(postId, userId, emojiType)
                .isPresent();
        if (alreadyExists) return;

        reactionRepository.save(Reaction.create(post, user, emojiType));
    }

    // 반응 취소 (없으면 무시 — 클라이언트 중복 요청에 안전)
    @Transactional
    public void removeReaction(Long userId, Long postId, String emojiType) {
        Post post = getPost(postId);
        validateMember(post, userId);

        reactionRepository
                .findByPostIdAndUserIdAndEmojiType(postId, userId, emojiType)
                .ifPresent(reactionRepository::delete);
    }

    // 반응 목록 조회
    // 반환: 이모지별 카운트 + 현재 유저의 반응 여부
    public List<ReactionSummaryResponse> getReactions(Long userId, Long postId) {
        Post post = getPost(postId);
        validateMember(post, userId);

        // 해당 포스트의 전체 반응
        List<Reaction> allReactions = reactionRepository.findByPostId(postId);

        // 현재 유저가 반응한 이모지 타입 Set (O(1) contains 조회)
        Set<String> myEmojiTypes = reactionRepository.findByPostIdAndUserId(postId, userId)
                .stream()
                .map(Reaction::getEmojiType)
                .collect(Collectors.toSet());

        // 이모지별 카운트 집계
        Map<String, Long> counts = allReactions.stream()
                .collect(Collectors.groupingBy(Reaction::getEmojiType, Collectors.counting()));

        return counts.entrySet().stream()
                .map(e -> new ReactionSummaryResponse(
                        e.getKey(),
                        e.getValue(),
                        myEmojiTypes.contains(e.getKey())
                ))
                .toList();
    }

    private Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    // post.getGroup().getId() — Group이 LAZY라도 ID만 꺼내는 건 프록시에서 처리 (DB 조회 없음)
    private void validateMember(Post post, Long userId) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(post.getGroup().getId(), userId)) {
            throw new CustomException(ErrorCode.NOT_GROUP_MEMBER);
        }
    }
}
