package com.kkiri.backend.global.security;

import com.kkiri.backend.auth.domain.User;
import com.kkiri.backend.auth.infrastructure.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileCompleteFilterTest {

    @Mock UserRepository userRepository;

    ProfileCompleteFilter filter;
    MockHttpServletRequest request;
    MockHttpServletResponse response;
    MockFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new ProfileCompleteFilter(userRepository);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
    }

    void authenticateAs(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList())
        );
    }

    // ── 버그 재현 ──────────────────────────────────────────────────

    @Test
    @DisplayName("[버그 재현] 프로필 미완료 유저 → throw 아닌 403 + ApiResponse JSON 직접 응답" +
            " (수정 전: throw CustomException → Filter 레이어라 ControllerAdvice 미적용 → BasicErrorController → 의도치 않은 응답 포맷)")
    void profileNotCompleted_returns403WithJsonBody_notThrowException() throws Exception {
        authenticateAs(1L);
        User pendingUser = User.createPending("kakao_123");
        given(userRepository.findById(1L)).willReturn(Optional.of(pendingUser));

        filter.doFilter(request, response, chain);

        // 수정 후: 직접 응답 작성
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).contains("application/json");
        // ApiResponse 포맷 확인 (ControllerAdvice 없이도 일관된 응답)
        assertThat(response.getContentAsString()).contains("PROFILE_NOT_COMPLETED");
        assertThat(response.getContentAsString()).contains("\"success\":false");
        // 예외 전파 없이 응답 후 종료됨
        assertThat(chain.getRequest()).isNull();
    }

    // ── 정상 동작 ──────────────────────────────────────────────────

    @Test
    @DisplayName("프로필 완료 유저 → 필터 체인 통과")
    void profileCompleted_passesThrough() throws Exception {
        authenticateAs(1L);
        User completedUser = User.createPending("kakao_123");
        completedUser.completeProfile("끼리", "🐶", "#FF0000");
        given(userRepository.findById(1L)).willReturn(Optional.of(completedUser));

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("미인증 요청 → 프로필 필터 스킵 (Security가 인가 처리)")
    void unauthenticated_passesThrough() throws Exception {
        // SecurityContext에 인증 정보 없음 (토큰 없는 요청)

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        then(userRepository).should(never()).findById(any()); // DB 조회 자체를 안 함
    }

    @Test
    @DisplayName("/api/auth/** 경로는 shouldNotFilter → 프로필 미완료도 로그인/재발급 가능")
    void authPath_shouldNotFilter() {
        request.setRequestURI("/api/auth/kakao");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("/api/users/me/profile 경로는 shouldNotFilter → 프로필 설정 자체는 접근 가능")
    void profileSetupPath_shouldNotFilter() {
        request.setRequestURI("/api/users/me/profile");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("보호된 경로(/api/groups/my)는 shouldNotFilter = false → 필터 실행됨")
    void protectedPath_shouldFilter() {
        request.setRequestURI("/api/groups/my");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }
}