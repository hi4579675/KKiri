package com.kkiri.backend.group.presentation;

import com.kkiri.backend.global.common.ApiResponse;
import com.kkiri.backend.global.security.LoginUser;
import com.kkiri.backend.group.application.GroupService;
import com.kkiri.backend.group.application.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Group", description = "그룹 관련 API")
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;

    // 그룹 생성
    @Operation(summary = "그룹 생성")
    @PostMapping
    public ResponseEntity<?> createGroup(
            @LoginUser Long userId,
            @Valid @RequestBody CreateGroupRequest request
    ) {
        GroupResponse response = groupService.createGroup(userId, request);
        return ResponseEntity.ok(response);
    }
    // 초대 코드 조회
    @Operation(summary = "초대 코드 조회")
    @GetMapping("/{groupId}/invite-code")
    public ResponseEntity<?> getInviteCode(
            @LoginUser Long userId,
            @PathVariable Long groupId
    ) {
        InviteCodeResponse response = groupService.getInviteCode(userId, groupId);
        return ResponseEntity.ok(response);
    }

    // 초대 코드 갱신
    @Operation(summary = "초대 코드 갱신 (방장 전용)")
    @PostMapping("/{groupId}/invite-code/renew")
    public ResponseEntity<?> renewInviteCode(
            @LoginUser Long userId,
            @PathVariable Long groupId
    ) {
        InviteCodeResponse response = groupService.renewInviteCode(userId, groupId);
        return ResponseEntity.ok(response);
    }

    // 초대 코드로 그룹 참여
    @Operation(summary = "초대 코드로 그룹 참여")
    @PostMapping("/join")
    public ResponseEntity<?> joinGroup(
            @LoginUser Long userId,
            @Valid @RequestBody JoinGroupRequest req
    ) {
        JoinGroupResponse response = groupService.joinGroup(userId, req.inviteCode());
        return ResponseEntity.ok(response);
    }

    // 멤버 강퇴
    @Operation(summary = "멤버 강퇴 (방장 전용)")
    @DeleteMapping("/{groupId}/members/{targetUserId}")
    public ResponseEntity<?> kickMember(
            @LoginUser Long userId,
            @PathVariable Long groupId,
            @PathVariable Long targetUserId
    ) {
        groupService.kickMember(userId, groupId, targetUserId);
        return ResponseEntity.noContent().build();
    }

    // 그룹 탈퇴
    @Operation(summary = "그룹 탈퇴")
    @DeleteMapping("/{groupId}/members/me")
    public ResponseEntity<?> leaveGroup(
            @LoginUser Long userId,
            @PathVariable Long groupId
    ) {
        groupService.leaveGroup(userId, groupId);
        return ResponseEntity.noContent().build();
    }

    // 방장 위임
    @Operation(summary = "방장 위임 (방장 전용)")
    @PatchMapping("/{groupId}/owner")
    public ResponseEntity<?> transferOwner(
            @LoginUser Long userId,
            @PathVariable Long groupId,
            @RequestBody TransferOwnerRequest req
    ) {
        groupService.transferOwner(userId, groupId, req.targetUserId());
        return ResponseEntity.ok().build();
    }
}
