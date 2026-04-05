package com.kkiri.backend.global.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 생성 정보 Embedded 클래스.
 * 모든 엔티티의 생성 시간 / 생성자를 담당.
 *
 * @Embeddable : 이 클래스가 다른 엔티티에 내장될 수 있음을 선언.
 *               DB 테이블에 별도 테이블 없이 부모 테이블 컬럼으로 매핑됨.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreateAudit {

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private CreateAudit(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static CreateAudit now() {
        return new CreateAudit(LocalDateTime.now(ZoneOffset.UTC));
    }
}
