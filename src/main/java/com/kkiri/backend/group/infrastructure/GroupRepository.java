package com.kkiri.backend.group.infrastructure;

import com.kkiri.backend.group.domain.Group;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {
}
