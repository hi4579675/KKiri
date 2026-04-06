package com.kkiri.backend.post.application;

import com.kkiri.backend.auth.domain.User;
import com.kkiri.backend.global.exception.CustomException;
import com.kkiri.backend.global.exception.ErrorCode;
import com.kkiri.backend.group.domain.GroupMember;
import com.kkiri.backend.group.infrastructure.GroupMemberRepository;
import com.kkiri.backend.post.application.dto.*;
import com.kkiri.backend.post.domain.Post;
import com.kkiri.backend.post.infrastructure.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private final PostRepository postRepository;
    private final GroupMemberRepository groupMemberRepository;

    public FeedResponse getFeed(Long userId, Long groupId, Integer cursor, int size) {
        // 1. 그룹 멤버 검증 다른 / 그룹 피드 못 보게 먼저 막음
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new CustomException(ErrorCode.NOT_GROUP_MEMBER);
        }

        // 2.  cursor가 있으면 그 값 사용 → 이전 페이지 이어서 조회
        //     cursor가 없으면 현재 시각 hour → 가장 최신부터 조회
        int cursorHourBucket = (cursor != null) ? cursor : LocalDateTime.now(ZoneOffset.UTC).getHour();
        // 서버 시각 기준이 아닌 UTC 기준으로 고정
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        // 3. 포스트 조회
        List<Post> posts = postRepository.findFeed(
                groupId, today, cursorHourBucket, PageRequest.of(0, size)
                // 0번째 페이지, size개 제한
                // 커서 방식이라 항상 "0번째 페이지"지만 WHERE 조건이 cursor로 이미 필터돼 있음
        );

        // 4. hour_bucket으로 그룹핑
        Map<Integer, List<PostResponse>> grouped = posts.stream()
                .collect(Collectors.groupingBy(
                        Post::getHourBucket,
                        LinkedHashMap::new,
                        Collectors.mapping(PostResponse::from, Collectors.toList())
                ));

        List<BucketResponse> buckets = grouped.entrySet().stream()
                .map(e -> new BucketResponse(e.getKey(), e.getValue()))
                .toList();

        // 5. nextCursor 계산
        Integer nextCursor = null;
        if (posts.size() == size) {
            int lastHourBucket = posts.get(posts.size() - 1).getHourBucket();
            if (lastHourBucket > 0) {
                nextCursor = lastHourBucket - 1;
            }
        }

        // 6. 오늘 포스트 올린 유저 ID Set
        List<User> todayContributors = postRepository.findTodayContributors(groupId, today);
        Set<Long> postedUserIds = todayContributors.stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        // 7. 그룹 전체 멤버 → contributors 조합
        List<ContributorResponse> contributors = groupMemberRepository.findByGroupId(groupId).stream()
                .map(GroupMember::getUser)
                .map(user -> ContributorResponse.of(user, postedUserIds.contains(user.getId())))
                .toList();

        return new FeedResponse(contributors, buckets, nextCursor);
    }
}
