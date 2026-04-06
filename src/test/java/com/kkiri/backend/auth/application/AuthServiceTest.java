package com.kkiri.backend.auth.application;

import com.kkiri.backend.auth.application.dto.TokenResponse;
import com.kkiri.backend.auth.domain.User;
import com.kkiri.backend.auth.infrastructure.KakaoAuthClient;
import com.kkiri.backend.auth.infrastructure.KakaoAuthClient.KakaoUserInfo;
import com.kkiri.backend.auth.infrastructure.RefreshTokenRepository;
import com.kkiri.backend.auth.infrastructure.UserRepository;
import com.kkiri.backend.global.exception.CustomException;
import com.kkiri.backend.global.exception.ErrorCode;
import com.kkiri.backend.global.security.JwtProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock KakaoAuthClient kakaoAuthClient;
    @Mock UserRepository userRepository;
    @Mock JwtProvider jwtProvider;
    @Mock RefreshTokenRepository refreshTokenRepository;

    @InjectMocks AuthService authService;

    User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = User.createPending("kakao_123");
        ReflectionTestUtils.setField(existingUser, "id", 1L);
    }

    // ── 카카오 로그인 ──────────────────────────────────────────────

    @Test
    @DisplayName("카카오 로그인 - 신규 유저: pending 상태로 저장 후 JWT 발급")
    void kakaoLogin_newUser() {
        KakaoUserInfo userInfo = new KakaoUserInfo("kakao_new", "신규유저");
        User newUser = User.createPending("kakao_new");
        ReflectionTestUtils.setField(newUser, "id", 2L);

        given(kakaoAuthClient.getUserInfo("kakao-access-token")).willReturn(userInfo);
        given(userRepository.findByKakaoId("kakao_new")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(newUser);
        given(jwtProvider.createAccessToken(2L)).willReturn("access-token");
        given(jwtProvider.createRefreshToken(2L)).willReturn("refresh-token");
        given(jwtProvider.getRefreshTokenTime()).willReturn(604_800_000L);

        TokenResponse response = authService.kakaoLogin("kakao-access-token");

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.profileCompleted()).isFalse(); // pending 상태
        then(userRepository).should().save(any(User.class));
        then(refreshTokenRepository).should().save(eq(2L), eq("refresh-token"), any(Duration.class));
    }

    @Test
    @DisplayName("카카오 로그인 - 기존 유저: DB 조회 후 JWT 재발급")
    void kakaoLogin_existingUser() {
        existingUser.completeProfile("끼리", "🐶", "#FF0000");
        KakaoUserInfo userInfo = new KakaoUserInfo("kakao_123", "끼리");

        given(kakaoAuthClient.getUserInfo("kakao-access-token")).willReturn(userInfo);
        given(userRepository.findByKakaoId("kakao_123")).willReturn(Optional.of(existingUser));
        given(jwtProvider.createAccessToken(1L)).willReturn("access-token");
        given(jwtProvider.createRefreshToken(1L)).willReturn("refresh-token");
        given(jwtProvider.getRefreshTokenTime()).willReturn(604_800_000L);

        TokenResponse response = authService.kakaoLogin("kakao-access-token");

        assertThat(response.profileCompleted()).isTrue();
        then(userRepository).should(never()).save(any()); // 기존 유저는 저장 안 함
    }

    // ── Refresh Token Rotation ─────────────────────────────────────

    @Test
    @DisplayName("토큰 재발급 성공 - Access/Refresh 모두 새 토큰으로 교체됨")
    void refresh_success() {
        Claims mockClaims = mock(Claims.class);
        given(mockClaims.getSubject()).willReturn("1");

        given(jwtProvider.getUserInfoFromToken("old-refresh-token")).willReturn(mockClaims);
        given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.of("old-refresh-token"));
        given(jwtProvider.createAccessToken(1L)).willReturn("new-access-token");
        given(jwtProvider.createRefreshToken(1L)).willReturn("new-refresh-token");
        given(jwtProvider.getRefreshTokenTime()).willReturn(604_800_000L);

        TokenResponse response = authService.refresh("old-refresh-token");

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        // 새 Refresh Token이 Redis에 덮어쓰기됨
        then(refreshTokenRepository).should().save(eq(1L), eq("new-refresh-token"), any(Duration.class));
    }

    @Test
    @DisplayName("토큰 재발급 - Redis에 저장된 토큰이 없으면 예외 발생 (로그아웃/만료)")
    void refresh_tokenNotInRedis() {
        Claims mockClaims = mock(Claims.class);
        given(mockClaims.getSubject()).willReturn("1");

        given(jwtProvider.getUserInfoFromToken("old-refresh-token")).willReturn(mockClaims);
        given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("old-refresh-token"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.REFRESH_TOKEN_INVALID);
    }

    @Test
    @DisplayName("토큰 재발급 - Redis 토큰과 요청 토큰 불일치 시 예외 발생 (탈취 의심)")
    void refresh_tokenMismatch() {
        Claims mockClaims = mock(Claims.class);
        given(mockClaims.getSubject()).willReturn("1");

        given(jwtProvider.getUserInfoFromToken("attacker-refresh-token")).willReturn(mockClaims);
        given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.of("legit-refresh-token"));

        assertThatThrownBy(() -> authService.refresh("attacker-refresh-token"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.REFRESH_TOKEN_INVALID);
    }
}