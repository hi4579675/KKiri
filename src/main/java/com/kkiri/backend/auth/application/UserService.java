package com.kkiri.backend.auth.application;

import com.kkiri.backend.auth.application.dto.CompleteProfileRequest;
import com.kkiri.backend.auth.domain.User;
import com.kkiri.backend.auth.infrastructure.UserRepository;
import com.kkiri.backend.global.exception.CustomException;
import com.kkiri.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    /**
     * 닉네임 중복 확인.
     * DB 조회 한 번으로 true/false만 반환 — 전체 유저 조회 없음.
     */
    public boolean isNicknameAvailable(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    /**
     * 프로필 최초 설정 (US-02).
     * 완료 시 profileCompleted = true → 이후 다른 API 접근 허용됨.
     */
    @Transactional
    public void completeProfile(Long userId, CompleteProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (userRepository.existsByNickname(request.nickname())) {
           throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }
        user.completeProfile(request.nickname(), request.avatarEmoji(), request.avatarColor());

    }


    /**
     * FCM 토큰 갱신.
     * 앱 재설치 시 토큰이 바뀌므로 앱 시작마다 최신 토큰으로 덮어씀.
     */
    @Transactional
    public void updateFcmToken(Long userId, String fcmToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.updateFcmToken(fcmToken);
    }
    /**
     * 알림 수신 여부 변경 (US-07).
     * false이면 FCM 발송 시 skip됨.
     */
    @Transactional
    public void updatePushEnabled(Long userId, boolean pushEnabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updatePushEnabled(pushEnabled);
    }

}
