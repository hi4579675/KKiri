package com.kkiri.backend.group.application;

import com.kkiri.backend.auth.domain.User;
import com.kkiri.backend.auth.infrastructure.UserRepository;
import com.kkiri.backend.global.exception.CustomException;
import com.kkiri.backend.global.exception.ErrorCode;
import com.kkiri.backend.group.application.dto.CreateGroupRequest;
import com.kkiri.backend.group.application.dto.GroupResponse;
import com.kkiri.backend.group.application.dto.InviteCodeResponse;
import com.kkiri.backend.group.application.dto.JoinGroupResponse;
import com.kkiri.backend.group.domain.Group;
import com.kkiri.backend.group.domain.GroupMember;
import com.kkiri.backend.group.infrastructure.GroupMemberRepository;
import com.kkiri.backend.group.infrastructure.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    // 그룹 생성
    @Transactional
    public GroupResponse createGroup(Long userId, CreateGroupRequest req) {
        User user = getUser(userId);
        Group group = Group.create(req.name());
        groupRepository.save(group);
        groupMemberRepository.save(GroupMember.createOwner(group, user));
        return GroupResponse.from(group);
    }

    // 초대 코드 조회
    public InviteCodeResponse getInviteCode(Long userId, Long groupId) {
        Group group = getGroup(groupId);
        validateMember(groupId, userId);
        if (group.isInviteCodeExpired()) {
            throw new CustomException(ErrorCode.EXPIRED_INVITE_CODE);
        }
        return InviteCodeResponse.from(group);
    }

    // 초대 코드 갱신(방장 전용)
    @Transactional
    public InviteCodeResponse renewInviteCode(Long userId, Long groupId) {
        Group group = getGroup(groupId);
        validateMember(groupId, userId);
        group.renewInviteCode();
        return InviteCodeResponse.from(group);
    }

    // 초대 코드로 그룹 참여
    @Transactional
    public JoinGroupResponse joinGroup(Long userId, String inviteCode) {
        User user = getUser(userId);
        Group group = groupRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INVITE_CODE));

        if (groupMemberRepository.existsByGroupIdAndUserId(group.getId(), userId)) {
            throw new CustomException(ErrorCode.ALREADY_IN_GROUP);
        }
        if (group.isInviteCodeExpired()) {
            throw new CustomException(ErrorCode.EXPIRED_INVITE_CODE);
        }
        long currentCount = groupMemberRepository.countByGroupId(group.getId());
        if (currentCount >= group.getMaxMembers()) {
            throw new CustomException(ErrorCode.GROUP_FULL);
        }

        groupMemberRepository.save(GroupMember.createMember(group, user));
        return new JoinGroupResponse(group.getId(), group.getName(), currentCount + 1);
    }
    // 멤버 강퇴 (방장 전용)
    @Transactional
    public void kickMember(Long ownerId, Long groupId, Long targetUserId) {
        validateOwner(groupId, ownerId);
        GroupMember target = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
        if (target.isOwner()) {
            throw new CustomException(ErrorCode.CANNOT_KICK_OWNER);
        }
        groupMemberRepository.delete(target);
    }

    // 그룹 탈퇴
    @Transactional
    public void leaveGroup(Long userId, Long groupId) {
        GroupMember me = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));

        groupMemberRepository.delete(me);

        if (me.isOwner()) {
            List<GroupMember> remaining = groupMemberRepository.findByGroupId(groupId);
            if (remaining.isEmpty()) {
                groupRepository.deleteById(groupId);
            } else {
                // 랜덤으로 방장 승격
                remaining.get(0).promoteToOwner();
            }
        }
    }

    // 방장 위임
    @Transactional
    public void transferOwner(Long ownerId, Long groupId, Long targetUserId) {
        GroupMember currentOwner = groupMemberRepository.findByGroupIdAndUserId(groupId, ownerId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
        if (!currentOwner.isOwner()) {
            throw new CustomException(ErrorCode.NOT_GROUP_OWNER);
        }
        GroupMember target = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));

        currentOwner.demoteToMember();
        target.promoteToOwner();
    }
    // ── 내부 헬퍼 ──────────────────────────────────────────────

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private Group getGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));
    }

    private void validateMember(Long groupId, Long userId) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new CustomException(ErrorCode.NOT_GROUP_MEMBER);
        }
    }

    private void validateOwner(Long groupId, Long userId) {
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
        if (!member.isOwner()) {
            throw new CustomException(ErrorCode.NOT_GROUP_OWNER);
        }
    }
 }
