package com.kkiri.backend.post.domain;

import com.kkiri.backend.auth.domain.User;
import com.kkiri.backend.global.audit.CreateAudit;
import com.kkiri.backend.group.domain.Group;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(nullable = false)
    private String imageUrl;

    @Column(length = 30)
    private String caption;

    // 올린 시각의 시간대 (0~23) — 피드 시간대별 그룹핑용
    @Column(nullable = false)
    private int hourBucket;

    // 자정 배치가 true로 변경 → 아카이브 후 삭제 불가
    @Column(nullable = false)
    private boolean archived = false;

    @Embedded
    private CreateAudit createAudit;

    public static Post create(User user, Group group, String imageUrl, String caption) {
        Post post = new Post();
        post.user = user;
        post.group = group;
        post.imageUrl = imageUrl;
        post.caption = caption;
        post.hourBucket = LocalDateTime.now(ZoneOffset.UTC).getHour();
        post.archived = false;
        post.createAudit = CreateAudit.now();
        return post;
    }

    public boolean isOwner(Long userId) {
        return this.user.getId().equals(userId);
    }

    public boolean isArchived() {
        return this.archived;
    }

    public void archive() {
        this.archived = true;
    }
}
