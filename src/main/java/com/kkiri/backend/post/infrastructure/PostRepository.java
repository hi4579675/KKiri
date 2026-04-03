package com.kkiri.backend.post.infrastructure;

import com.kkiri.backend.post.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
