package com.kkiri.backend.group.presentation;

import com.kkiri.backend.global.common.ApiResponse;
import com.kkiri.backend.global.exception.SuccessCode;
import com.kkiri.backend.global.security.LoginUser;
import com.kkiri.backend.group.application.GroupService;
import com.kkiri.backend.group.application.dto.*;
import java.util.List;
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

    @Operation(summary = "내 그룹 목록")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> getMyGroups(
            @Parameter(hidden = true) @LoginUser Long userId
    ) {
        return ApiResponse.onSuccess(SuccessCode.GROUP_FETCHED, groupService.getMyGroups(userId));
    }

    @Operation(summary = "그룹 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(
            @Parameter(hidden = true) @LoginUser Long userId,
            @Valid @RequestBody CreateGroupRequest request
    ) {
        return ApiResponse.onSuccess(SuccessCode.GROUP_CREATED, groupService.createGroup(userId, request));
    }

    @Operation(summary = "초대 코드 조회")
    @GetMapping("/{groupId}/invite-code")
    public ResponseEntity<ApiResponse<InviteCodeResponse>> getInviteCode(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long groupId
    ) {
        return ApiResponse.onSuccess(SuccessCode.GROUP_FETCHED, groupService.getInviteCode(userId, groupId));
    }

    @Operation(summary = "초대 코드 갱신 (방장 전용)")
    @PostMapping("/{groupId}/invite-code/renew")
    public ResponseEntity<ApiResponse<InviteCodeResponse>> renewInviteCode(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long groupId
    ) {
        return ApiResponse.onSuccess(SuccessCode.INVITE_CODE_REISSUED, groupService.renewInviteCode(userId, groupId));
    }

    @Operation(summary = "초대 코드로 그룹 참여")
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<JoinGroupResponse>> joinGroup(
            @Parameter(hidden = true) @LoginUser Long userId,
            @Valid @RequestBody JoinGroupRequest req
    ) {
        return ApiResponse.onSuccess(SuccessCode.GROUP_JOINED, groupService.joinGroup(userId, req.inviteCode()));
    }

    @Operation(summary = "멤버 목록 조회")
    @GetMapping("/{groupId}/members")
    public ResponseEntity<ApiResponse<List<GroupMemberResponse>>> getMembers(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long groupId
    ) {
        return ApiResponse.onSuccess(SuccessCode.GROUP_FETCHED, groupService.getMembers(userId, groupId));
    }

    @Operation(summary = "멤버 강퇴 (방장 전용)")
    @DeleteMapping("/{groupId}/members/{targetUserId}")
    public ResponseEntity<ApiResponse<Void>> kickMember(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long groupId,
            @PathVariable Long targetUserId
    ) {
        groupService.kickMember(userId, groupId, targetUserId);
        return ApiResponse.onSuccess(SuccessCode.GROUP_LEFT, null);
    }

    @Operation(summary = "그룹 탈퇴")
    @DeleteMapping("/{groupId}/members/me")
    public ResponseEntity<ApiResponse<Void>> leaveGroup(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long groupId
    ) {
        groupService.leaveGroup(userId, groupId);
        return ApiResponse.onSuccess(SuccessCode.GROUP_LEFT, null);
    }

    @Operation(summary = "방장 위임 (방장 전용)")
    @PatchMapping("/{groupId}/owner")
    public ResponseEntity<ApiResponse<Void>> transferOwner(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long groupId,
            @RequestBody TransferOwnerRequest req
    ) {
        groupService.transferOwner(userId, groupId, req.targetUserId());
        return ApiResponse.onSuccess(SuccessCode.GROUP_FETCHED, null);
    }
}
