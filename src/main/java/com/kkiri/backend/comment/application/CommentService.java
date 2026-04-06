package com.kkiri.backend.comment.application;

import com.kkiri.backend.auth.domain.User;
import com.kkiri.backend.auth.infrastructure.UserRepository;
import com.kkiri.backend.comment.application.dto.AddCommentRequest;
import com.kkiri.backend.comment.application.dto.CommentResponse;
import com.kkiri.backend.comment.application.dto.SliceResponse;
import com.kkiri.backend.comment.domain.Comment;
import com.kkiri.backend.comment.infrastructure.CommentRepository;
import com.kkiri.backend.global.exception.CustomException;
import com.kkiri.backend.global.exception.ErrorCode;
import com.kkiri.backend.group.infrastructure.GroupMemberRepository;
import com.kkiri.backend.post.domain.Post;
import com.kkiri.backend.post.infrastructure.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;

    // 댓글 목록 조회 (커서 기반, created_at ASC — 오래된 댓글부터)
    // size+1개 조회 후 hasNext 판단 → nextCursor 반환
    public SliceResponse<CommentResponse> getComments(Long userId, Long postId, Long cursorId, int size) {
        Post post = getPost(postId);
        validateMember(post, userId);

        // cursorId null이면 0 → id > 0 조건으로 처음부터 조회
        long cursor = (cursorId != null) ? cursorId : 0L;

        // size+1개 조회해서 다음 페이지 존재 여부 판단
        List<Comment> comments = commentRepository.findComments(
                postId, cursor, PageRequest.of(0, size + 1)
        );

        boolean hasNext = comments.size() > size;
        List<Comment> content = hasNext ? comments.subList(0, size) : comments;

        // 다음 커서 = 현재 페이지 마지막 댓글의 id
        Long nextCursor = hasNext ? content.get(content.size() - 1).getId() : null;

        return new SliceResponse<>(
                content.stream().map(CommentResponse::from).toList(),
                nextCursor,
                hasNext
        );
    }

    // 댓글 추가
    @Transactional
    public CommentResponse addComment(Long userId, Long postId, AddCommentRequest request) {
        Post post = getPost(postId);
        User user = getUser(userId);
        validateMember(post, userId);

        Comment comment = Comment.create(post, user, request.content());
        commentRepository.save(comment);

        // TODO Phase 10: 포스트 오너에게 FCM 댓글 알림 발송
        // fcmService.sendCommentNotification(post.getUser().getId(), userId, postId);

        return CommentResponse.from(comment);
    }

    // 댓글 삭제 (본인 댓글만 가능)
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.isOwner(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_COMMENT);
        }

        commentRepository.delete(comment);
    }

    private Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateMember(Post post, Long userId) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(post.getGroup().getId(), userId)) {
            throw new CustomException(ErrorCode.NOT_GROUP_MEMBER);
        }
    }
}
