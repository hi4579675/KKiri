package com.kkiri.backend.global.security;

import com.kkiri.backend.global.exception.CustomException;
import com.kkiri.backend.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

class JwtProviderTest {

    JwtProvider jwtProvider;

    // HS256은 최소 256bit(32byte) 키 필요 — 36바이트 문자열을 Base64로 인코딩해서 사용
    static final String TEST_SECRET = Base64.getEncoder()
            .encodeToString("this-is-a-test-secret-key-for-kkiri".getBytes(StandardCharsets.UTF_8));
    static final long ACCESS_EXP  = 3_600_000L;   // 1시간
    static final long REFRESH_EXP = 604_800_000L; // 7일

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtProvider, "ACCESS_TOKEN_TIME", ACCESS_EXP);
        ReflectionTestUtils.setField(jwtProvider, "REFRESH_TOKEN_TIME", REFRESH_EXP);
        jwtProvider.init(); // @PostConstruct 수동 호출
    }

    // ── 토큰 생성 ──────────────────────────────────────────────────

    @Test
    @DisplayName("Access Token 생성 - userId가 subject에 포함됨")
    void createAccessToken_subjectIsUserId() {
        String token = jwtProvider.createAccessToken(1L);

        Claims claims = jwtProvider.getUserInfoFromToken(token);
        assertThat(claims.getSubject()).isEqualTo("1");
    }

    @Test
    @DisplayName("Refresh Token 생성 - userId가 subject에 포함됨")
    void createRefreshToken_subjectIsUserId() {
        String token = jwtProvider.createRefreshToken(42L);

        Claims claims = jwtProvider.getUserInfoFromToken(token);
        assertThat(claims.getSubject()).isEqualTo("42");
    }

    @Test
    @DisplayName("Access/Refresh 토큰은 동일 userId로 생성해도 서로 다른 문자열")
    void accessAndRefreshTokenAreDifferent() {
        String access  = jwtProvider.createAccessToken(1L);
        String refresh = jwtProvider.createRefreshToken(1L);

        assertThat(access).isNotEqualTo(refresh);
    }

    // ── 토큰 검증 ──────────────────────────────────────────────────

    @Test
    @DisplayName("유효한 토큰은 validateToken 통과")
    void validateToken_valid() {
        String token = jwtProvider.createAccessToken(1L);

        assertThatCode(() -> jwtProvider.validateToken(token))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("만료된 토큰이면 TOKEN_EXPIRED 예외 발생")
    void validateToken_expired() {
        // 만료 시간을 -1000ms로 설정 → 과거에 이미 만료된 토큰 생성
        ReflectionTestUtils.setField(jwtProvider, "ACCESS_TOKEN_TIME", -1000L);
        String expiredToken = jwtProvider.createAccessToken(1L);

        assertThatThrownBy(() -> jwtProvider.validateToken(expiredToken))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.TOKEN_EXPIRED);
    }

    @Test
    @DisplayName("변조된 토큰이면 TOKEN_INVALID 예외 발생")
    void validateToken_tampered() {
        String token = jwtProvider.createAccessToken(1L);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> jwtProvider.validateToken(tampered))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.TOKEN_INVALID);
    }

    // ── Bearer 파싱 ──────────────────────────────────────────────

    @Test
    @DisplayName("resolveToken - Bearer 접두사 제거 후 순수 토큰 반환")
    void resolveToken_extractsToken() {
        String token = jwtProvider.createAccessToken(1L);
        String bearer = "Bearer " + token;

        assertThat(jwtProvider.resolveToken(bearer)).isEqualTo(token);
    }

    @Test
    @DisplayName("resolveToken - Bearer 없으면 null 반환")
    void resolveToken_noBearer_returnsNull() {
        assertThat(jwtProvider.resolveToken(null)).isNull();
        assertThat(jwtProvider.resolveToken("InvalidHeader")).isNull();
    }
}