package com.kkiri.backend.post.infrastructure;

import com.kkiri.backend.post.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p JOIN FETCH p.user WHERE p.group.id = :groupId AND p.archived = false ORDER BY p.createAudit.createdAt DESC")
    List<Post> findFeedByGroupId(@Param("groupId") Long groupId);
}
