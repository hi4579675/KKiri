package com.kkiri.backend.auth.infrastructure;

import com.kkiri.backend.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 카카오 로그인 시 기존 유저 조회 (없으면 신규 가입 처리)
    Optional<User> findByKakaoId(String kakaoId);

    // 닉네임 중복 확인용
    boolean existsByNickname(String nickname);
}
