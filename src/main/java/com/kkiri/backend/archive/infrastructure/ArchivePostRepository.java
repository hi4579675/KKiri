package com.kkiri.backend.archive.infrastructure;

import com.kkiri.backend.archive.domain.ArchivePost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArchivePostRepository extends JpaRepository<ArchivePost, Long> {
}
