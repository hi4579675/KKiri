package com.kkiri.backend.comment.infrastructure;

import com.kkiri.backend.comment.domain.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 커서 기반 댓글 조회 (id > cursorId, created_at ASC — 오래된 댓글부터)
    // JOIN FETCH p.user → N+1 방지 (댓글마다 User 조회 SQL 추가 발생 막음)
    // cursorId = 0L이면 처음부터 조회 (첫 페이지)
    @Query("""
            SELECT c FROM Comment c
            JOIN FETCH c.user
            WHERE c.post.id = :postId
            AND c.id > :cursorId
            ORDER BY c.createAudit.createdAt ASC
            """)
    List<Comment> findComments(
            @Param("postId") Long postId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
