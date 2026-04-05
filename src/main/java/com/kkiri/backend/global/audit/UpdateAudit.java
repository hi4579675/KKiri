package com.kkiri.backend.global.audit;

// UpdateAudit.java
// ─────────────────────────────────────────────────────────────────────────────

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 수정 정보 Embedded 클래스.
 * 처음 생성 시엔 null로 비워두고, 수정 발생 시 touch()로 갱신.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateAudit {

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private UpdateAudit(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static UpdateAudit empty() {
        return new UpdateAudit(null);
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}