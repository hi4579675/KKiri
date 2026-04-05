package com.kkiri.backend.auth.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    // Redis key 형식: "refresh:{userId}"
    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Refresh Token 저장.
     * TTL을 설정해 만료 시 Redis가 자동으로 삭제합니다.
     */
    public void save(Long userId, String refreshToken, Duration ttl) {
        redisTemplate.opsForValue().set(KEY_PREFIX + userId, refreshToken, ttl);
    }
    /**
     * 저장된 Refresh Token 조회.
     * 없으면 Optional.empty() 반환 (만료됐거나 로그아웃된 경우).
     */
    public Optional<String> findByUserId(Long userId) {
        String token = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        return Optional.ofNullable(token);
    }

    /**
     * Refresh Token 삭제.
     * 로그아웃 시 호출해 해당 토큰으로 재발급을 막습니다.
     */
    public void delete(Long userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }
}
