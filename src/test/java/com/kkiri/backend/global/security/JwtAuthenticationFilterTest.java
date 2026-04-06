package com.kkiri.backend.global.security;

import com.kkiri.backend.global.exception.CustomException;
import com.kkiri.backend.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
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
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock JwtProvider jwtProvider;

    JwtAuthenticationFilter filter;
    MockHttpServletRequest request;
    MockHttpServletResponse response;
    MockFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtProvider);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
    }

    // ── 버그 재현 ──────────────────────────────────────────────────

    @Test
    @DisplayName("[버그 재현] 만료 토큰 → 예외 전파 없이 401 직접 응답" +
            " (수정 전: throw 전파 → 인증 없는 상태로 AuthorizationFilter → 403)")
    void expiredToken_returns401_notPropagateException() throws Exception {
        request.addHeader("Authorization", "Bearer expired-token");
        given(jwtProvider.resolveToken("Bearer expired-token")).willReturn("expired-token");
        willThrow(new CustomException(ErrorCode.TOKEN_EXPIRED))
                .given(jwtProvider).validateToken("expired-token");

        filter.doFilter(request, response, chain);

        // 수정 후: 필터가 직접 401 작성 → 예외 전파 없음
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"success\":false");
        // 핵심: chain.doFilter()가 호출되지 않음 → 예외가 전파되지 않은 증거
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("[버그 재현] 변조 토큰 → 예외 전파 없이 401 직접 응답")
    void invalidToken_returns401_notPropagateException() throws Exception {
        request.addHeader("Authorization", "Bearer tampered-token");
        given(jwtProvider.resolveToken("Bearer tampered-token")).willReturn("tampered-token");
        willThrow(new CustomException(ErrorCode.TOKEN_INVALID))
                .given(jwtProvider).validateToken("tampered-token");

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    // ── 정상 동작 ──────────────────────────────────────────────────

    @Test
    @DisplayName("Authorization 헤더 없으면 인증 없이 필터 체인 통과")
    void noToken_passesThrough() throws Exception {
        given(jwtProvider.resolveToken(null)).willReturn(null);

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull(); // 체인 계속 진행
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("유효한 토큰 → SecurityContext에 userId 등록 후 필터 체인 통과")
    void validToken_setsAuthenticationAndPassesThrough() throws Exception {
        request.addHeader("Authorization", "Bearer valid-token");
        given(jwtProvider.resolveToken("Bearer valid-token")).willReturn("valid-token");

        Claims mockClaims = mock(Claims.class);
        given(mockClaims.getSubject()).willReturn("1");
        given(jwtProvider.getUserInfoFromToken("valid-token")).willReturn(mockClaims);

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(1L);
    }
}