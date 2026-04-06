package com.kkiri.backend.group.presentation;

import com.kkiri.backend.global.security.LoginUser;
import com.kkiri.backend.group.application.GroupService;
import com.kkiri.backend.group.application.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @Operation(summary = "그룹 생성")
    @PostMapping
    public ResponseEntity<?> createGroup(
            @Parameter(hidden = true) @LoginUser Long userId,
            @Valid @RequestBody CreateGroupRequest request
    ) {
        return ResponseEntity.ok(groupService.createGroup(userId, request));
    }

    @Operation(summary = "초대 코드 조회")
    @GetMapping("/{groupId}/invite-code")
    public ResponseEntity<?> getInviteCode(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long groupId
    ) {
        return ResponseEntity.ok(groupService.getInviteCode(userId, groupId));
    }

    @Operation(summary = "초대 코드 갱신 (방장 전용)")
    @PostMapping("/{groupId}/invite-code/renew")
    public ResponseEntity<?> renewInviteCode(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long groupId
    ) {
        return ResponseEntity.ok(groupService.renewInviteCode(userId, groupId));
    }

    @Operation(summary = "초대 코드로 그룹 참여")
    @PostMapping("/join")
    public ResponseEntity<?> joinGroup(
            @Parameter(hidden = true) @LoginUser Long userId,
            @Valid @RequestBody JoinGroupRequest req
    ) {
        return ResponseEntity.ok(groupService.joinGroup(userId, req.inviteCode()));
    }

    @Operation(summary = "멤버 강퇴 (방장 전용)")
    @DeleteMapping("/{groupId}/members/{targetUserId}")
    public ResponseEntity<?> kickMember(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long groupId,
            @PathVariable Long targetUserId
    ) {
        groupService.kickMember(userId, groupId, targetUserId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "그룹 탈퇴")
    @DeleteMapping("/{groupId}/members/me")
    public ResponseEntity<?> leaveGroup(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long groupId
    ) {
        groupService.leaveGroup(userId, groupId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "방장 위임 (방장 전용)")
    @PatchMapping("/{groupId}/owner")
    public ResponseEntity<?> transferOwner(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long groupId,
            @RequestBody TransferOwnerRequest req
    ) {
        groupService.transferOwner(userId, groupId, req.targetUserId());
        return ResponseEntity.ok().build();
    }
}
