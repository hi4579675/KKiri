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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoAuthClient kakaoAuthClient;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 카카오 로그인 처리.
     * 앱에서 카카오 accessToken을 받아 유저를 식별하고 JWT를 발급합니다.
     *
     * 신규 유저: pending 상태(프로필 미설정)로 저장 → profileCompleted = false
     * 기존 유저: 그대로 조회 → profileCompleted 값 그대로 반환
     */
    @Transactional
    public TokenResponse kakaoLogin(String kakaoAccessToken) {
        // 1. 카카오 서버에서 유저 정보 조회
        KakaoUserInfo kakaoUserInfo = kakaoAuthClient.getUserInfo(kakaoAccessToken);

        // 2. kakaoId로 기존 유저 조회, 없으면 신규 생성 (pending 상태)
        User user = userRepository.findByKakaoId(kakaoUserInfo.kakaoId())
                .orElseGet(() -> userRepository.save(User.createPending(kakaoUserInfo.kakaoId())));

        // 3. JWT 발급
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        // 4. Refresh Token Redis 저장 (7일 TTL)
        refreshTokenRepository.save(user.getId(), refreshToken, Duration.ofMillis(jwtProvider.getRefreshTokenTime()));

        return new TokenResponse(accessToken, refreshToken, user.isProfileCompleted());
    }

    /**
     * Access Token 재발급 (Refresh Token Rotation).
     * Refresh Token도 함께 교체해 탈취 위험을 줄입니다.
     * 매 재발급마다 새 Refresh Token을 발급하고 Redis를 갱신합니다.
     */
    public TokenResponse refresh(String refreshToken) {
        // 1. Refresh Token 서명/만료 검증
        jwtProvider.validateToken(refreshToken);

        // 2. Refresh Token에서 userId 추출
        Claims claims = jwtProvider.getUserInfoFromToken(refreshToken);
        Long userId = Long.parseLong(claims.getSubject());

        // 3. Redis에 저장된 토큰과 비교 (없거나 다르면 탈취 의심)
        String storedToken = refreshTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_INVALID));

        if (!storedToken.equals(refreshToken)) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        // 4. 새 토큰 발급 (Rotation: Refresh Token도 교체)
        String newAccessToken = jwtProvider.createAccessToken(userId);
        String newRefreshToken = jwtProvider.createRefreshToken(userId);

        // 5. Redis에 새 Refresh Token으로 덮어쓰기
        refreshTokenRepository.save(userId, newRefreshToken, Duration.ofMillis(jwtProvider.getRefreshTokenTime()));

        // profileCompleted는 재발급 시 불필요하므로 true로 고정
        return new TokenResponse(newAccessToken, newRefreshToken, true);
    }
}
