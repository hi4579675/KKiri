package com.kkiri.backend.group.infrastructure;

import com.kkiri.backend.group.domain.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
}
