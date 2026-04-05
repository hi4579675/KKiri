package com.kkiri.backend.auth.domain;

import com.kkiri.backend.global.audit.CreateAudit;
import com.kkiri.backend.global.audit.UpdateAudit;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 카카오에서 발급하는 고유 식별자
    // unique = true → 같은 카카오 계정으로 중복 가입 방지 + 자동으로 인덱스 생성됨
    @Column(nullable = false, unique = true)
    private String kakaoId;

    // 최대 10자, 글로벌 중복 불가 (unique = true)
    // nullable = true → 카카오 로그인 직후 pending 상태에서는 null (US-02 프로필 설정 전)
    @Column(nullable = true, unique = true, length = 10)
    private String nickname;

    // 이모지 아바타 ex) "🐶"
    // nullable = true → pending 상태에서는 null
    @Column(nullable = true)
    private String avatarEmoji;

    // 아바타 테두리 컬러 ex) "#FF5733" - 피드에서 멤버 구별용
    // nullable = true → pending 상태에서는 null
    @Column(nullable = true)
    private String avatarColor;

    // FCM 푸시 알림 토큰
    // nullable - 알림 거부 or 로그아웃 시 null
    @Column
    private String fcmToken;

    // 프로필 설정 완료 여부
    // false → 카카오 로그인 직후 (nickname/avatar 없는 pending 상태)
    // true  → US-02 프로필 설정 완료 후
    // ProfileCompleteFilter에서 이 값을 체크해 미완료 유저의 다른 API 접근을 막음
    @Column(nullable = false)
    private boolean profileCompleted = false;

    // 푸시 알림 수신 여부 (US-07)
    // FCM 발송 시 false인 유저는 발송 생략
    @Column(nullable = false)
    private boolean pushEnabled = true;

    // 수정 감사 정보 (updated_at)
    // 처음엔 null, 프로필 수정 시 touch() 호출로 갱신
    @Embedded
    private UpdateAudit updateAudit;

    // 생성 감사 정보 (created_at)
    // 카카오 로그인 시 서버가 자동 생성
    @Embedded
    private CreateAudit createAudit;

    // SoftDeleteAudit 없음
    // User 탈퇴 = 개인정보 삭제 의무 → 소프트딜리트보다 실제 삭제 or 개인정보 초기화가 맞음

    // 카카오 로그인 최초 진입 시 사용
    // nickname/avatar 없는 pending 상태로 저장, 이후 completeProfile()로 완성
    public static User createPending(String kakaoId) {
        User user = new User();
        user.kakaoId = kakaoId;
        user.profileCompleted = false;
        user.pushEnabled = true;
        user.createAudit = CreateAudit.now();
        user.updateAudit = UpdateAudit.empty();
        return user;
    }

    // US-02 최초 프로필 설정
    // profileCompleted = true로 변경 → 이후 다른 API 접근 허용
    public void completeProfile(String nickname, String avatarEmoji, String avatarColor) {
        this.nickname = nickname;
        this.avatarEmoji = avatarEmoji;
        this.avatarColor = avatarColor;
        this.profileCompleted = true;
        this.updateAudit.touch();
    }

    // 프로필 수정 (닉네임, 아바타 3개는 항상 같이 변경)
    public void updateProfile(String nickname, String avatarEmoji, String avatarColor) {
        this.nickname = nickname;
        this.avatarEmoji = avatarEmoji;
        this.avatarColor = avatarColor;
        this.updateAudit.touch();
    }

    // 앱 시작마다 호출 - 기기 토큰 갱신 (재설치 시 토큰 변경 대응)
    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    // 로그아웃 시 호출 - null이면 알림 발송 skip
    public void clearFcmToken() {
        this.fcmToken = null;
    }

    // 알림 설정 변경 (US-07)
    public void updatePushEnabled(boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
        this.updateAudit.touch();
    }
}
