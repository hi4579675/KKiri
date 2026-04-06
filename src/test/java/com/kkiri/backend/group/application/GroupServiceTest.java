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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock GroupRepository groupRepository;
    @Mock GroupMemberRepository groupMemberRepository;
    @Mock UserRepository userRepository;

    @InjectMocks GroupService groupService;

    User owner, member;
    Group group;
    GroupMember ownerMember, regularMember;

    @BeforeEach
    void setUp() {
        owner = User.createPending("kakao_owner");
        ReflectionTestUtils.setField(owner, "id", 1L);
        owner.completeProfile("방장", "🐶", "#FF0000");

        member = User.createPending("kakao_member");
        ReflectionTestUtils.setField(member, "id", 2L);
        member.completeProfile("멤버", "🐱", "#00FF00");

        group = Group.create("우리끼리");
        ReflectionTestUtils.setField(group, "id", 10L);

        ownerMember = GroupMember.createOwner(group, owner);
        ReflectionTestUtils.setField(ownerMember, "id", 100L);

        regularMember = GroupMember.createMember(group, member);
        ReflectionTestUtils.setField(regularMember, "id", 101L);
    }

    // ── 그룹 생성 ──────────────────────────────────────────────────

    @Test
    @DisplayName("그룹 생성 성공 - 생성자가 OWNER로 등록됨")
    void createGroup_success() {
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(groupRepository.save(any(Group.class))).willAnswer(inv -> inv.getArgument(0));
        given(groupMemberRepository.save(any(GroupMember.class))).willAnswer(inv -> inv.getArgument(0));

        GroupResponse response = groupService.createGroup(1L, new CreateGroupRequest("우리끼리"));

        assertThat(response.name()).isEqualTo("우리끼리");
        then(groupRepository).should().save(any(Group.class));
        then(groupMemberRepository).should().save(any(GroupMember.class));
    }

    @Test
    @DisplayName("그룹 생성 - 존재하지 않는 유저면 예외 발생")
    void createGroup_userNotFound() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.createGroup(999L, new CreateGroupRequest("그룹")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.USER_NOT_FOUND);
    }

    // ── 초대 코드 조회 ──────────────────────────────────────────────

    @Test
    @DisplayName("초대 코드 조회 성공")
    void getInviteCode_success() {
        given(groupRepository.findById(10L)).willReturn(Optional.of(group));
        given(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).willReturn(true);

        InviteCodeResponse response = groupService.getInviteCode(1L, 10L);

        assertThat(response.inviteCode()).isNotBlank();
        assertThat(response.groupId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("초대 코드 조회 - 만료된 코드면 예외 발생")
    void getInviteCode_expired() {
        ReflectionTestUtils.setField(group, "inviteCodeExpiredAt",
                LocalDateTime.now(ZoneOffset.UTC).minusDays(1));
        given(groupRepository.findById(10L)).willReturn(Optional.of(group));
        given(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).willReturn(true);

        assertThatThrownBy(() -> groupService.getInviteCode(1L, 10L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.EXPIRED_INVITE_CODE);
    }

    @Test
    @DisplayName("초대 코드 조회 - 그룹 멤버가 아니면 예외 발생")
    void getInviteCode_notMember() {
        given(groupRepository.findById(10L)).willReturn(Optional.of(group));
        given(groupMemberRepository.existsByGroupIdAndUserId(10L, 99L)).willReturn(false);

        assertThatThrownBy(() -> groupService.getInviteCode(99L, 10L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.NOT_GROUP_MEMBER);
    }

    // ── 그룹 참여 ──────────────────────────────────────────────────

    @Test
    @DisplayName("초대 코드로 그룹 참여 성공")
    void joinGroup_success() {
        String code = group.getInviteCode();
        given(userRepository.findById(2L)).willReturn(Optional.of(member));
        given(groupRepository.findByInviteCode(code)).willReturn(Optional.of(group));
        given(groupMemberRepository.existsByGroupIdAndUserId(10L, 2L)).willReturn(false);
        given(groupMemberRepository.countByGroupId(10L)).willReturn(1L);
        given(groupMemberRepository.save(any(GroupMember.class))).willAnswer(inv -> inv.getArgument(0));

        JoinGroupResponse response = groupService.joinGroup(2L, code);

        assertThat(response.groupId()).isEqualTo(10L);
        assertThat(response.memberCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("그룹 참여 - 이미 멤버면 예외 발생")
    void joinGroup_alreadyMember() {
        String code = group.getInviteCode();
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(groupRepository.findByInviteCode(code)).willReturn(Optional.of(group));
        given(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).willReturn(true);

        assertThatThrownBy(() -> groupService.joinGroup(1L, code))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.ALREADY_IN_GROUP);
    }

    @Test
    @DisplayName("그룹 참여 - 만료된 초대 코드면 예외 발생")
    void joinGroup_expiredCode() {
        ReflectionTestUtils.setField(group, "inviteCodeExpiredAt",
                LocalDateTime.now(ZoneOffset.UTC).minusDays(1));
        String code = group.getInviteCode();

        given(userRepository.findById(2L)).willReturn(Optional.of(member));
        given(groupRepository.findByInviteCode(code)).willReturn(Optional.of(group));
        given(groupMemberRepository.existsByGroupIdAndUserId(10L, 2L)).willReturn(false);

        assertThatThrownBy(() -> groupService.joinGroup(2L, code))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.EXPIRED_INVITE_CODE);
    }

    @Test
    @DisplayName("그룹 참여 - 정원 초과면 예외 발생")
    void joinGroup_groupFull() {
        String code = group.getInviteCode();
        given(userRepository.findById(2L)).willReturn(Optional.of(member));
        given(groupRepository.findByInviteCode(code)).willReturn(Optional.of(group));
        given(groupMemberRepository.existsByGroupIdAndUserId(10L, 2L)).willReturn(false);
        given(groupMemberRepository.countByGroupId(10L)).willReturn(6L);

        assertThatThrownBy(() -> groupService.joinGroup(2L, code))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.GROUP_FULL);
    }

    @Test
    @DisplayName("그룹 참여 - 잘못된 초대 코드면 예외 발생")
    void joinGroup_invalidCode() {
        given(userRepository.findById(2L)).willReturn(Optional.of(member));
        given(groupRepository.findByInviteCode("ZZZZZZ")).willReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.joinGroup(2L, "ZZZZZZ"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.INVALID_INVITE_CODE);
    }

    // ── 멤버 강퇴 ──────────────────────────────────────────────────

    @Test
    @DisplayName("멤버 강퇴 성공")
    void kickMember_success() {
        given(groupMemberRepository.findByGroupIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMember));
        given(groupMemberRepository.findByGroupIdAndUserId(10L, 2L)).willReturn(Optional.of(regularMember));

        groupService.kickMember(1L, 10L, 2L);

        then(groupMemberRepository).should().delete(regularMember);
    }

    @Test
    @DisplayName("멤버 강퇴 - 방장이 아니면 예외 발생")
    void kickMember_notOwner() {
        given(groupMemberRepository.findByGroupIdAndUserId(10L, 2L)).willReturn(Optional.of(regularMember));

        assertThatThrownBy(() -> groupService.kickMember(2L, 10L, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.NOT_GROUP_OWNER);
    }

    @Test
    @DisplayName("멤버 강퇴 - 대상이 방장이면 예외 발생")
    void kickMember_cannotKickOwner() {
        GroupMember secondOwner = GroupMember.createOwner(group, member);
        ReflectionTestUtils.setField(secondOwner, "id", 102L);

        given(groupMemberRepository.findByGroupIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMember));
        given(groupMemberRepository.findByGroupIdAndUserId(10L, 2L)).willReturn(Optional.of(secondOwner));

        assertThatThrownBy(() -> groupService.kickMember(1L, 10L, 2L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.CANNOT_KICK_OWNER);
    }

    // ── 그룹 탈퇴 ──────────────────────────────────────────────────

    @Test
    @DisplayName("일반 멤버 탈퇴 성공")
    void leaveGroup_memberSuccess() {
        given(groupMemberRepository.findByGroupIdAndUserId(10L, 2L)).willReturn(Optional.of(regularMember));

        groupService.leaveGroup(2L, 10L);

        then(groupMemberRepository).should().delete(regularMember);
        then(groupRepository).should(never()).deleteById(any());
    }

    @Test
    @DisplayName("방장 탈퇴 - 남은 멤버가 있으면 첫 번째 멤버 방장 승격")
    void leaveGroup_ownerWithRemainingMembers() {
        given(groupMemberRepository.findByGroupIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMember));
        given(groupMemberRepository.findByGroupId(10L)).willReturn(List.of(regularMember));

        groupService.leaveGroup(1L, 10L);

        then(groupMemberRepository).should().delete(ownerMember);
        assertThat(regularMember.isOwner()).isTrue();
    }

    @Test
    @DisplayName("방장 탈퇴 - 혼자였으면 그룹 삭제")
    void leaveGroup_ownerAlone_groupDeleted() {
        given(groupMemberRepository.findByGroupIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMember));
        given(groupMemberRepository.findByGroupId(10L)).willReturn(List.of());

        groupService.leaveGroup(1L, 10L);

        then(groupRepository).should().deleteById(10L);
    }

    // ── 방장 위임 ──────────────────────────────────────────────────

    @Test
    @DisplayName("방장 위임 성공 - 역할이 교체됨")
    void transferOwner_success() {
        given(groupMemberRepository.findByGroupIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMember));
        given(groupMemberRepository.findByGroupIdAndUserId(10L, 2L)).willReturn(Optional.of(regularMember));

        groupService.transferOwner(1L, 10L, 2L);

        assertThat(ownerMember.isOwner()).isFalse();
        assertThat(regularMember.isOwner()).isTrue();
    }

    @Test
    @DisplayName("방장 위임 - 방장이 아닌 유저가 시도하면 예외 발생")
    void transferOwner_notOwner() {
        given(groupMemberRepository.findByGroupIdAndUserId(10L, 2L)).willReturn(Optional.of(regularMember));

        assertThatThrownBy(() -> groupService.transferOwner(2L, 10L, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.NOT_GROUP_OWNER);
    }
}