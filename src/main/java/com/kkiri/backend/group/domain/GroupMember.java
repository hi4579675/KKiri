

package com.kkiri.backend.group.domain;

import com.kkiri.backend.auth.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "group_members")
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupRole role;

    @Column(nullable = false)
    private LocalDateTime joinedAt;
    public static GroupMember createOwner(Group group, User user) {
        GroupMember gm = new GroupMember();
        gm.group = group;
        gm.user = user;
        gm.role = GroupRole.OWNER;
        gm.joinedAt = LocalDateTime.now(ZoneOffset.UTC);
        return gm;
    }

    public static GroupMember createMember(Group group, User user) {
        GroupMember gm = new GroupMember();
        gm.group = group;
        gm.user = user;
        gm.role = GroupRole.MEMBER;
        gm.joinedAt = LocalDateTime.now(ZoneOffset.UTC);
        return gm;
    }

    public boolean isOwner() {
        return this.role == GroupRole.OWNER;
    }

    public void promoteToOwner() {
        this.role = GroupRole.OWNER;
    }

    public void demoteToMember() {
        this.role = GroupRole.MEMBER;
    }

}
