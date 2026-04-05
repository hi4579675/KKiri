package com.kkiri.backend.global.audit;

// ─────────────────────────────────────────────────────────────────────────────
// SoftDeleteAudit.java
// ─────────────────────────────────────────────────────────────────────────────

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 소프트 딜리트 정보 Embedded 클래스.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SoftDeleteAudit {

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private SoftDeleteAudit(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public static SoftDeleteAudit active() {
        return new SoftDeleteAudit(null);
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
