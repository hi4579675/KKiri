package com.kkiri.backend.post.application;

import com.kkiri.backend.auth.domain.User;
import com.kkiri.backend.auth.infrastructure.UserRepository;
import com.kkiri.backend.global.exception.CustomException;
import com.kkiri.backend.global.exception.ErrorCode;
import com.kkiri.backend.group.domain.Group;
import com.kkiri.backend.group.infrastructure.GroupMemberRepository;
import com.kkiri.backend.group.infrastructure.GroupRepository;
import com.kkiri.backend.post.application.dto.*;
import com.kkiri.backend.post.domain.Post;
import com.kkiri.backend.post.infrastructure.PostRepository;
import com.kkiri.backend.post.infrastructure.R2StorageAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final R2StorageAdapter r2StorageAdapter;
    private final FcmService fcmService;

    @Value("${r2.public-url}")
    private String publicUrl;

    // presigned URL 발급
    // createPost와 달리 여기서도 멤버 검증을 하는 이유:
    // presigned URL 발급 자체가 R2 리소스 생성 권한 부여이므로,
    // 비멤버가 URL을 발급받아 업로드하는 것을 막아야 함
    public PresignedUrlResponse generatePresignedUrl(Long userId, PresignedUrlRequest req) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(req.groupId(), userId)) {
            throw new CustomException(ErrorCode.NOT_GROUP_MEMBER);
        }
        return r2StorageAdapter.generatePresignedUrl(req.groupId(), req.fileName(), req.contentType());
    }

    // 포스트 생성
    @Transactional
    public PostResponse createPost(Long userId, CreatePostRequest req) {
        User user = getUser(userId);
        Group group = groupRepository.findById(req.groupId())
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        if (!groupMemberRepository.existsByGroupIdAndUserId(req.groupId(), userId)) {
            throw new CustomException(ErrorCode.NOT_GROUP_MEMBER);
        }

        String imageUrl = publicUrl + "/" + req.imageKey();
        Post post = Post.create(user, group, imageUrl, req.caption());
        postRepository.save(post);

        // 비동기 FCM 발송
        fcmService.sendPostNotification(group.getId(), userId, user.getNickname());

        return PostResponse.from(post);
    }

    // 포스트 삭제
    @Transactional
    public void deletePost(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.isOwner(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_POST);
        }
        if (post.isArchived()) {
            throw new CustomException(ErrorCode.ARCHIVED_POST_DELETE_FORBIDDEN);
        }

        String imageKey = post.getImageUrl().replace(publicUrl + "/", "");
        postRepository.delete(post);

        // 비동기 R2 삭제
        r2StorageAdapter.deleteObject(imageKey);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
