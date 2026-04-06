package com.kkiri.backend.group.infrastructure;

import com.kkiri.backend.group.domain.GroupMember;
import com.kkiri.backend.group.domain.GroupRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    List<GroupMember> findByGroupId(Long groupId);

    long countByGroupId(Long groupId);

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    Optional<GroupMember> findByGroupIdAndRole(Long groupId, GroupRole role);

    @org.springframework.data.jpa.repository.Query("SELECT gm FROM GroupMember gm JOIN FETCH gm.group WHERE gm.user.id = :userId")
    List<GroupMember> findByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}
