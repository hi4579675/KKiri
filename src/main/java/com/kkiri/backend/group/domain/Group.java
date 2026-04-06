package com.kkiri.backend.group.domain;

import com.kkiri.backend.global.audit.CreateAudit;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "groups")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(nullable = false, unique = true, length = 6)
    private String inviteCode;

    @Column(nullable = false)
    private LocalDateTime inviteCodeExpiredAt;

    @Column(nullable = false)
    private int maxMembers = 6;

    @Embedded
    private CreateAudit createAudit;

    // 팩토리 메서드 패턴
    public static Group create(String name) {
        Group group = new Group();
        group.name = name;
        group.maxMembers = 6;
        group.inviteCode = generateCode();
        group.inviteCodeExpiredAt = LocalDateTime.now(ZoneOffset.UTC).plusDays(7);
        group.createAudit = CreateAudit.now();
        return group;
    }

    public boolean isInviteCodeExpired() {
        return LocalDateTime.now(ZoneOffset.UTC).isAfter(inviteCodeExpiredAt);
    }

    public void renewInviteCode() {
        this.inviteCode = generateCode();
        this.inviteCodeExpiredAt = LocalDateTime.now(ZoneOffset.UTC).plusDays(7);
    }

    // 생성 로직이 서비스가 아닌 엔티티 안에 넣는 이유 : Service가 아닌 도메인 객체가 자신의
    // 상태를 책임짐(도메인 모델 패턴)
    public static String generateCode() {
        return UUID.randomUUID()
                .toString()
                .replaceAll("-", "")
                .substring(0, 6)
                .toUpperCase();
    }


}
