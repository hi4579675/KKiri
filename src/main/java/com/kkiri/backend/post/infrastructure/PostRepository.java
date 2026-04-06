package com.kkiri.backend.post.infrastructure;

import com.kkiri.backend.auth.domain.User;
import com.kkiri.backend.post.domain.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 커서 기반 피드 조회 (오늘, archived=false, hourBucket 이하)
    @Query("""
            SELECT p FROM Post p
            JOIN FETCH p.user
            WHERE p.group.id = :groupId
            AND p.archived = false
            AND CAST(p.createAudit.createdAt AS date) = :today
            AND p.hourBucket <= :cursorHourBucket
            ORDER BY p.hourBucket DESC, p.createAudit.createdAt DESC
            """)
    List<Post> findFeed(
            @Param("groupId") Long groupId,
            @Param("today") LocalDate today,
            @Param("cursorHourBucket") int cursorHourBucket,
            Pageable pageable
    );

    // 오늘 포스트 올린 유저 목록 (아바타 바용)
    @Query("""
            SELECT DISTINCT p.user FROM Post p
            WHERE p.group.id = :groupId
            AND p.archived = false
            AND CAST(p.createAudit.createdAt AS date) = :today
            """)
    List<User> findTodayContributors(
            @Param("groupId") Long groupId,
            @Param("today") LocalDate today
    );
}
