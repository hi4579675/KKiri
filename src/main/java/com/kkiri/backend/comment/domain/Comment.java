package com.kkiri.backend.comment.domain;

import com.kkiri.backend.auth.domain.User;
import com.kkiri.backend.global.audit.CreateAudit;
import com.kkiri.backend.global.audit.UpdateAudit;
import com.kkiri.backend.post.domain.Post;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String content;

    @Embedded
    private CreateAudit createAudit;

    // 현재 댓글 수정 기능은 없지만 UpdateAudit 유지
    // → 향후 수정 기능 추가 시 스키마 변경 없이 touch()만 호출하면 됨
    @Embedded
    private UpdateAudit updateAudit;

    public static Comment create(Post post, User user, String content) {
        Comment comment = new Comment();
        comment.post = post;
        comment.user = user;
        comment.content = content;
        comment.createAudit = CreateAudit.now();
        comment.updateAudit = UpdateAudit.empty();
        return comment;
    }

    public boolean isOwner(Long userId) {
        return this.user.getId().equals(userId);
    }
}
