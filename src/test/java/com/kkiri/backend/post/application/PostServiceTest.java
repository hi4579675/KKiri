package com.kkiri.backend.post.application;

import com.kkiri.backend.auth.domain.User;
import com.kkiri.backend.auth.infrastructure.UserRepository;
import com.kkiri.backend.global.exception.CustomException;
import com.kkiri.backend.global.exception.ErrorCode;
import com.kkiri.backend.group.domain.Group;
import com.kkiri.backend.group.domain.GroupMember;
import com.kkiri.backend.group.infrastructure.GroupMemberRepository;
import com.kkiri.backend.group.infrastructure.GroupRepository;
import com.kkiri.backend.post.application.dto.CreatePostRequest;
import com.kkiri.backend.post.application.dto.PostResponse;
import com.kkiri.backend.post.domain.Post;
import com.kkiri.backend.post.infrastructure.PostRepository;
import com.kkiri.backend.post.infrastructure.R2StorageAdapter;
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
class PostServiceTest {

    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;
    @Mock GroupRepository groupRepository;
    @Mock GroupMemberRepository groupMemberRepository;
    @Mock R2StorageAdapter r2StorageAdapter;
    @Mock FcmService fcmService;

    @InjectMocks PostService postService;

    User user;
    Group group;
    Post post;

    static final String PUBLIC_URL = "https://cdn.example.com";

    @BeforeEach
    void setUp() {
        // @Value 필드 직접 주입
        ReflectionTestUtils.setField(postService, "publicUrl", PUBLIC_URL);

        user = User.createPending("kakao_user");
        ReflectionTestUtils.setField(user, "id", 1L);
        user.completeProfile("끼리", "🐶", "#FF0000");

        group = Group.create("우리끼리");
        ReflectionTestUtils.setField(group, "id", 10L);

        post = Post.create(user, group, PUBLIC_URL + "/img/test.jpg", "오늘의 사진");
        ReflectionTestUtils.setField(post, "id", 100L);
    }

    // ── 포스트 생성 ──────────────────────────────────────────────────

    @Test
    @DisplayName("포스트 생성 성공 - FCM 비동기 발송 호출됨")
    void createPost_success() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(groupRepository.findById(10L)).willReturn(Optional.of(group));
        given(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).willReturn(true);
        given(postRepository.save(any(Post.class))).willAnswer(inv -> inv.getArgument(0));

        CreatePostRequest request = new CreatePostRequest(10L, "img/test.jpg", "오늘의 사진");
        PostResponse response = postService.createPost(1L, request);

        assertThat(response.caption()).isEqualTo("오늘의 사진");
        assertThat(response.imageUrl()).isEqualTo(PUBLIC_URL + "/img/test.jpg");
        then(fcmService).should().sendPostNotification(eq(10L), eq(1L), eq("끼리"));
    }

    @Test
    @DisplayName("포스트 생성 - 그룹 멤버가 아니면 예외 발생")
    void createPost_notMember() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(groupRepository.findById(10L)).willReturn(Optional.of(group));
        given(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).willReturn(false);

        CreatePostRequest request = new CreatePostRequest(10L, "img/test.jpg", "캡션");

        assertThatThrownBy(() -> postService.createPost(1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.NOT_GROUP_MEMBER);
    }

    // ── 포스트 삭제 ──────────────────────────────────────────────────

    @Test
    @DisplayName("포스트 삭제 성공 - R2 비동기 삭제 호출됨")
    void deletePost_success() {
        given(postRepository.findById(100L)).willReturn(Optional.of(post));

        postService.deletePost(1L, 100L);

        then(postRepository).should().delete(post);
        then(r2StorageAdapter).should().deleteObject("img/test.jpg");
    }

    @Test
    @DisplayName("포스트 삭제 - 본인 포스트가 아니면 예외 발생")
    void deletePost_forbidden() {
        given(postRepository.findById(100L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost(99L, 100L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.FORBIDDEN_POST);
    }

    @Test
    @DisplayName("포스트 삭제 - 아카이브된 포스트는 삭제 불가")
    void deletePost_archived() {
        post.archive();
        given(postRepository.findById(100L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost(1L, 100L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.ARCHIVED_POST_DELETE_FORBIDDEN);
    }
}