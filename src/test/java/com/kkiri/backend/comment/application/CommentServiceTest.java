package com.kkiri.backend.comment.application;

import com.kkiri.backend.auth.domain.User;
import com.kkiri.backend.auth.infrastructure.UserRepository;
import com.kkiri.backend.comment.application.dto.AddCommentRequest;
import com.kkiri.backend.comment.application.dto.CommentResponse;
import com.kkiri.backend.comment.domain.Comment;
import com.kkiri.backend.comment.infrastructure.CommentRepository;
import com.kkiri.backend.global.exception.CustomException;
import com.kkiri.backend.global.exception.ErrorCode;
import com.kkiri.backend.group.domain.Group;
import com.kkiri.backend.group.infrastructure.GroupMemberRepository;
import com.kkiri.backend.post.domain.Post;
import com.kkiri.backend.post.infrastructure.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock CommentRepository commentRepository;
    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;
    @Mock GroupMemberRepository groupMemberRepository;

    @InjectMocks CommentService commentService;

    User user, otherUser;
    Group group;
    Post post;
    Comment comment;

    @BeforeEach
    void setUp() {
        user = User.createPending("kakao_user");
        ReflectionTestUtils.setField(user, "id", 1L);
        user.completeProfile("끼리", "🐶", "#FF0000");

        otherUser = User.createPending("kakao_other");
        ReflectionTestUtils.setField(otherUser, "id", 2L);
        otherUser.completeProfile("남", "🐱", "#00FF00");

        group = Group.create("우리끼리");
        ReflectionTestUtils.setField(group, "id", 10L);

        post = Post.create(user, group, "https://cdn.example.com/img.jpg", "사진");
        ReflectionTestUtils.setField(post, "id", 100L);

        comment = Comment.create(post, user, "좋아요!");
        ReflectionTestUtils.setField(comment, "id", 200L);
    }

    // ── 댓글 추가 ──────────────────────────────────────────────────

    @Test
    @DisplayName("댓글 추가 성공")
    void addComment_success() {
        given(postRepository.findById(100L)).willReturn(Optional.of(post));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).willReturn(true);
        given(commentRepository.save(any(Comment.class))).willAnswer(inv -> inv.getArgument(0));

        CommentResponse response = commentService.addComment(1L, 100L, new AddCommentRequest("좋아요!"));

        assertThat(response.content()).isEqualTo("좋아요!");
        assertThat(response.nickname()).isEqualTo("끼리");
    }

    @Test
    @DisplayName("댓글 추가 - 존재하지 않는 포스트면 예외 발생")
    void addComment_postNotFound() {
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addComment(1L, 999L, new AddCommentRequest("내용")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("댓글 추가 - 그룹 멤버가 아니면 예외 발생")
    void addComment_notMember() {
        given(postRepository.findById(100L)).willReturn(Optional.of(post));
        given(userRepository.findById(99L)).willReturn(Optional.of(otherUser));
        given(groupMemberRepository.existsByGroupIdAndUserId(10L, 99L)).willReturn(false);

        assertThatThrownBy(() -> commentService.addComment(99L, 100L, new AddCommentRequest("내용")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.NOT_GROUP_MEMBER);
    }

    // ── 댓글 삭제 ──────────────────────────────────────────────────

    @Test
    @DisplayName("댓글 삭제 성공")
    void deleteComment_success() {
        given(commentRepository.findById(200L)).willReturn(Optional.of(comment));

        commentService.deleteComment(1L, 200L);

        then(commentRepository).should().delete(comment);
    }

    @Test
    @DisplayName("댓글 삭제 - 본인 댓글이 아니면 예외 발생")
    void deleteComment_notOwner() {
        given(commentRepository.findById(200L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.deleteComment(2L, 200L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.FORBIDDEN_COMMENT);
    }

    @Test
    @DisplayName("댓글 삭제 - 존재하지 않는 댓글이면 예외 발생")
    void deleteComment_notFound() {
        given(commentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteComment(1L, 999L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.COMMENT_NOT_FOUND);
    }
}