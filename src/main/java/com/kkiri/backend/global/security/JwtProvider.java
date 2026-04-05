package com.kkiri.backend.global.security;

import com.kkiri.backend.global.exception.CustomException;
import com.kkiri.backend.global.exception.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.security.SecurityException;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.secret}")
    private String secretKey;

    // Access Token : 1시간
    // Refresh Token : 7일 ( Redis에 저장, 로그아웃 시 삭제 )
    @Value("${jwt.access-expiration}")
    private long ACCESS_TOKEN_TIME;

    @Value("${jwt.refresh-expiration}")
    private long REFRESH_TOKEN_TIME;

    private SecretKey key;





    /**
     * Secret Key를 설정합니다.
     *
     * @PostConstruct 이유:
     * *   - @Value는 빈 생성 이후에 주입됨.
     * *   - 생성자에서 바로 key를 만들면 secretKey가 아직 null → NPE 발생.
     * *   - @PostConstruct는 의존성 주입이 완료된 직후 딱 한 번 실행되므로 안전.
     */
    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        key = Keys.hmacShaKeyFor(keyBytes);
    }

    // ─── 토큰 생성 ────────────────────────────────────────────────────────────

    /**
     * Access Token 생성.
     * subject에 memberId를 넣어 이후 요청에서 회원을 빠르게 식별.
     *
     * @param userId DB PK (subject)
     * @return 순수 Access Token 문자열 (Bearer 접두사 미포함)
     */
    public String createAccessToken(Long userId) {
        return createToken(userId, ACCESS_TOKEN_TIME);
    }

    /**
     * Refresh Token 생성.
     * Access Token 만료 시 재발급용으로 사용.
     * 최소한의 정보(memberId)만 담아 탈취 시 피해를 줄임.
     * 생성 후 Redis에 저장 → 로그아웃 시 삭제, 재발급 시 비교.
     *
     * @param userId DB PK (subject)
     * @return 순수 Refresh Token 문자열 (Bearer 접두사 미포함)
     */
    public String createRefreshToken(Long userId) {
        return createToken(userId, REFRESH_TOKEN_TIME);
    }

    /**
     * 공통 토큰 생성 로직.
     * subject에 memberId를 저장하는 이유:
     * - 기존 username(이메일) 방식은 이메일 변경 시 토큰 무효화 문제 발생.
     * - memberId(PK)는 불변값 → 안전하게 식별자로 사용 가능.
     * Bearer 접두사를 붙이지 않는 이유:
     * - 생성 시 붙이면 Redis 저장 / 블랙리스트 key로 쓸 때마다 제거해야 하는 번거로움 발생.
     * - 헤더에 붙이고 떼는 역할은 Controller / Filter에서 담당. (관심사 분리)
     *
     * @param userId     DB PK
     * @param expireTime 만료 시간 (밀리초)
     * @return 순수 JWT 토큰 문자열
     */
    private String createToken(Long userId, long expireTime) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId)) //토큰의 주인 Principal을 설정, PK 값 넣어서 변경이 안되게 함
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireTime))
                .signWith(key)  // 비밀키로 전체 내용 서명함
                .compact();
    }
    /**
     * 토큰 해석 로직
     * 토큰에서 Claims(payload 전체) 추출.
     * memberId 등 정보를 꺼낼 때 사용.
     *
     * @param token 순수 토큰 문자열
     * @return 토큰에 포함된 Claims 정보
     */
    public Claims getUserInfoFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key) // 우리가 가진 키로만 서명을 해라
                .build() //
                .parseSignedClaims(token) // 실제 토큰을 뜯어보는 과정, 변조되거나 만료 시에 예외 발생 (핵심 보인 장치)
                .getPayload(); // 실제 정보(userId)를 가져옴
    }

    // ─── 토큰 검증 ────────────────────────────────────────────────────────────



    /**
     * 토큰 유효성 검증.
     * JwtAuthenticationFilter에서 매 요청마다 호출됨.
     * JwtException 대신 CustomException을 throw하는 이유:
     * - JwtException은 Spring Security 내부 예외 → GlobalExceptionHandler가 잡지 못함.
     * - CustomException으로 던져야 GlobalExceptionHandler → ApiResponse 포맷으로 응답 가능.
     * @param token 순수 토큰 문자열
     * @return 유효하면 true (예외가 발생하지 않는 경우에만 도달)
     */
    public void validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);

        } catch (ExpiredJwtException e) {
            log.error("[JwtProvider] 만료된 토큰 - {}", e.getMessage());
            throw new CustomException(ErrorCode.TOKEN_EXPIRED);
        } catch (SecurityException | MalformedJwtException e) {
            // 서명 불일치 또는 토큰 형식 오류 — 변조 의심
            log.error("[JwtProvider] 유효하지 않은 토큰 - {}", e.getMessage());
            throw new CustomException(ErrorCode.TOKEN_INVALID);
        } catch (UnsupportedJwtException e) {
            // 지원하지 않는 JWT 형식 (알고리즘 불일치 등)
            log.error("[JwtProvider] 지원하지 않는 토큰 형식 - {}", e.getMessage());
            throw new CustomException(ErrorCode.TOKEN_INVALID);
        } catch (IllegalArgumentException e) {
            // 토큰이 null이거나 빈 문자열
            log.error("[JwtProvider] 토큰이 비어있음 - {}", e.getMessage());
            throw new CustomException(ErrorCode.TOKEN_INVALID);
        }
    }

    // ─── 토큰 유틸 ────────────────────────────────────────────────────────────

    /**
     * Authorization 헤더에서 순수 토큰 추출.
     * null 반환 케이스:
     *   - 헤더 자체가 없는 경우
     *   - "Bearer "로 시작하지 않는 경우 (잘못된 형식)
     * → JwtFilter에서 null이면 토큰 검증을 건너뜀 (permitAll 경로 대응)
     *
     * @param bearerToken Authorization 헤더 값
     * @return 순수 토큰 문자열 or null
     */
    public String resolveToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }
    /**
     * 토큰의 남은 유효 시간을 계산.
     * @param token 계산할 순수 토큰 문자열
     * @return 남은 시간 (밀리초)
     */
    // TODO :  만료된 토큰이 들어오면 예외가 터질 수 있음,
    //  로그아웃 시 이미 만료된 토큰으로 호출할 일이 있다면 추후 처리가 필요
    public long getExpiration(String token) {
        return getUserInfoFromToken(token).getExpiration().getTime() - new Date().getTime();
    }

    /**
     * Access Token 최대 유효 시간 반환.
     * RefreshTokenRepository에서 Redis TTL 설정 시 사용.
     *
     * @return ACCESS_TOKEN_TIME (밀리초)
     */
    public long getAccessTokenTime() {
        return ACCESS_TOKEN_TIME;
    }
    /**
     * Refresh Token 최대 유효 시간 반환.
     * RefreshTokenRepository에서 Redis TTL 설정 시 사용.
     *
     * @return REFRESH_TOKEN_TIME (밀리초)
     */
    public long getRefreshTokenTime() {
        return REFRESH_TOKEN_TIME;
    }
}
