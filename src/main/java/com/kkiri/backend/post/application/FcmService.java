package com.kkiri.backend.post.application;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.kkiri.backend.group.domain.GroupMember;
import com.kkiri.backend.group.infrastructure.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final GroupMemberRepository groupMemberRepository;

    // 포스트 업로드 시 그룹 멤버에게 푸시 알림 (비동기)
    @Async
    public void sendPostNotification(Long groupId, Long postOwnerId, String ownerNickname) {
        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);

        for (GroupMember member : members) {
            // 본인 제외
            if (member.getUser().getId().equals(postOwnerId)) continue;

            // 알림 거부 or 토큰 없는 유저 제외
            String fcmToken = member.getUser().getFcmToken();
            if (!member.getUser().isPushEnabled() || fcmToken == null) continue;

            try {
                Message message = Message.builder()
                        .setToken(fcmToken)
                        .setNotification(Notification.builder()
                                .setTitle("끼리")
                                .setBody(ownerNickname + "가 새 게시물을 올렸어요 📸")
                                .build())
                        .build();

                FirebaseMessaging.getInstance().send(message);
                log.info("[FCM] 발송 완료 → userId: {}", member.getUser().getId());
            } catch (Exception e) {
                log.error("[FCM] 발송 실패 → userId: {}", member.getUser().getId(), e);
            }
        }
    }
}
