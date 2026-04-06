package com.kkiri.backend.reaction.domain;

import com.kkiri.backend.auth.domain.User;
import com.kkiri.backend.global.audit.CreateAudit;
import com.kkiri.backend.post.domain.Post;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "reactions",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"post_id", "user_id", "emoji_type"}
        )
)
public class Reaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 포스트에 달린 반응인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // 누가 반응했는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // "❤️", "😂", "😮" 등 이모지 문자열 그대로 저장
    // UniqueConstraint(post_id, user_id, emoji_type) → 같은 이모지 중복 방지
    @Column(name = "emoji_type", nullable = false)
    private String emojiType;

    @Embedded
    private CreateAudit createAudit;

    public static Reaction create(Post post, User user, String emojiType) {
        Reaction reaction = new Reaction();
        reaction.post = post;
        reaction.user = user;
        reaction.emojiType = emojiType;
        reaction.createAudit = CreateAudit.now();
        return reaction;
    }
}
