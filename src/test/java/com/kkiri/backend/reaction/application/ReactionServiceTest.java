package com.kkiri.backend.reaction.application;

import com.kkiri.backend.auth.domain.User;
import com.kkiri.backend.auth.infrastructure.UserRepository;
import com.kkiri.backend.global.exception.CustomException;
import com.kkiri.backend.global.exception.ErrorCode;
import com.kkiri.backend.group.domain.Group;
import com.kkiri.backend.group.infrastructure.GroupMemberRepository;
import com.kkiri.backend.post.domain.Post;
import com.kkiri.backend.post.infrastructure.PostRepository;
import com.kkiri.backend.reaction.application.dto.ReactionSummaryResponse;
import com.kkiri.backend.reaction.domain.Reaction;
import com.kkiri.backend.reaction.infrastructure.ReactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ReactionServiceTest {

    @Mock ReactionRepository reactionRepository;
    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;
    @Mock GroupMemberRepository groupMemberRepository;

    @InjectMocks ReactionService reactionService;

    User user;
    Group group;
    Post post;

    @BeforeEach
    void setUp() {
        user = User.createPending("kakao_user");
        ReflectionTestUtils.setField(user, "id", 1L);
        user.completeProfile("끼리", "🐶", "#FF0000");

        group = Group.create("우리끼리");
        ReflectionTestUtils.setField(group, "id", 10L);

        post = Post.create(user, group, "https://cdn.example.com/img.jpg", "사진");
        ReflectionTestUtils.setField(post, "id", 100L);
    }

    // ── 반응 추가 ──────────────────────────────────────────────────

    @Test
    @DisplayName("반응 추가 성공 - 새 이모지 저장")
    void addReaction_success() {
        given(postRepository.findById(100L)).willReturn(Optional.of(post));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).willReturn(true);
        given(reactionRepository.findByPostIdAndUserIdAndEmojiType(100L, 1L, "❤️"))
                .willReturn(Optional.empty());
        given(reactionRepository.save(any(Reaction.class))).willAnswer(inv -> inv.getArgument(0));

        reactionService.addReaction(1L, 100L, "❤️");

        then(reactionRepository).should().save(any(Reaction.class));
    }

    @Test
    @DisplayName("반응 추가 - 동일 이모지 중복 요청은 무시 (idempotent)")
    void addReaction_duplicate_ignored() {
        Reaction existing = Reaction.create(post, user, "❤️");
        given(postRepository.findById(100L)).willReturn(Optional.of(post));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).willReturn(true);
        given(reactionRepository.findByPostIdAndUserIdAndEmojiType(100L, 1L, "❤️"))
                .willReturn(Optional.of(existing));

        reactionService.addReaction(1L, 100L, "❤️");

        then(reactionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("반응 추가 - 그룹 멤버가 아니면 예외 발생")
    void addReaction_notMember() {
        given(postRepository.findById(100L)).willReturn(Optional.of(post));
        given(userRepository.findById(99L)).willReturn(Optional.of(user));
        given(groupMemberRepository.existsByGroupIdAndUserId(10L, 99L)).willReturn(false);

        assertThatThrownBy(() -> reactionService.addReaction(99L, 100L, "❤️"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.NOT_GROUP_MEMBER);
    }

    // ── 반응 취소 ──────────────────────────────────────────────────

    @Test
    @DisplayName("반응 취소 성공 - 삭제됨")
    void removeReaction_success() {
        Reaction existing = Reaction.create(post, user, "❤️");
        given(postRepository.findById(100L)).willReturn(Optional.of(post));
        given(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).willReturn(true);
        given(reactionRepository.findByPostIdAndUserIdAndEmojiType(100L, 1L, "❤️"))
                .willReturn(Optional.of(existing));

        reactionService.removeReaction(1L, 100L, "❤️");

        then(reactionRepository).should().delete(existing);
    }

    @Test
    @DisplayName("반응 취소 - 없는 반응이면 무시 (idempotent)")
    void removeReaction_notExists_ignored() {
        given(postRepository.findById(100L)).willReturn(Optional.of(post));
        given(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).willReturn(true);
        given(reactionRepository.findByPostIdAndUserIdAndEmojiType(100L, 1L, "❤️"))
                .willReturn(Optional.empty());

        reactionService.removeReaction(1L, 100L, "❤️");

        then(reactionRepository).should(never()).delete(any());
    }

    // ── 반응 목록 조회 ──────────────────────────────────────────────

    @Test
    @DisplayName("반응 목록 조회 - 이모지별 카운트 및 내 반응 여부 반환")
    void getReactions_success() {
        User user2 = User.createPending("kakao_user2");
        ReflectionTestUtils.setField(user2, "id", 2L);

        Reaction r1 = Reaction.create(post, user, "❤️");
        Reaction r2 = Reaction.create(post, user2, "❤️");
        Reaction r3 = Reaction.create(post, user, "😂");

        given(postRepository.findById(100L)).willReturn(Optional.of(post));
        given(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).willReturn(true);
        given(reactionRepository.findByPostId(100L)).willReturn(List.of(r1, r2, r3));
        given(reactionRepository.findByPostIdAndUserId(100L, 1L)).willReturn(List.of(r1, r3));

        List<ReactionSummaryResponse> result = reactionService.getReactions(1L, 100L);

        assertThat(result).hasSize(2);

        ReactionSummaryResponse heart = result.stream()
                .filter(r -> r.emojiType().equals("❤️")).findFirst().orElseThrow();
        assertThat(heart.count()).isEqualTo(2L);
        assertThat(heart.reacted()).isTrue();

        ReactionSummaryResponse laugh = result.stream()
                .filter(r -> r.emojiType().equals("😂")).findFirst().orElseThrow();
        assertThat(laugh.count()).isEqualTo(1L);
        assertThat(laugh.reacted()).isTrue();
    }
}