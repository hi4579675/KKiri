package com.kkiri.backend.auth.presentation;

import com.kkiri.backend.auth.application.UserService;
import com.kkiri.backend.auth.application.dto.CompleteProfileRequest;
import com.kkiri.backend.auth.application.dto.UpdateFcmTokenRequest;
import com.kkiri.backend.auth.application.dto.UpdateProfileRequest;
import com.kkiri.backend.auth.application.dto.UpdatePushEnabledRequest;
import com.kkiri.backend.auth.application.dto.UserProfileResponse;
import com.kkiri.backend.global.common.ApiResponse;
import com.kkiri.backend.global.exception.SuccessCode;
import com.kkiri.backend.global.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "User", description = "유저 관련 API")
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 프로필 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMe(
            @Parameter(hidden = true) @LoginUser Long userId
    ) {
        return ApiResponse.onSuccess(SuccessCode.PROFILE_FETCHED, userService.getMe(userId));
    }

    @Operation(summary = "프로필 수정 (닉네임·아바타)")
    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @Parameter(hidden = true) @LoginUser Long userId,
            @RequestBody UpdateProfileRequest request
    ) {
        userService.updateProfile(userId, request);
        return ApiResponse.onSuccess(SuccessCode.PROFILE_UPDATED, null);
    }

    @Operation(summary = "닉네임 중복 확인", description = "인증 불필요. 프로필 설정 화면에서 실시간 중복 체크")
    @GetMapping("/nickname/check")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkNickname(
            @RequestParam("nickname") String nickname
    ) {
        boolean available = userService.isNicknameAvailable(nickname);
        return ApiResponse.onSuccess(SuccessCode.NICKNAME_AVAILABLE, Map.of("available", available));
    }

    @Operation(summary = "프로필 최초 설정", description = "완료 후 profileCompleted=true, 이후 모든 API 접근 가능")
    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> completeProfile(
            @Parameter(hidden = true) @LoginUser Long userId,
            @RequestBody CompleteProfileRequest request
    ) {
        userService.completeProfile(userId, request);
        return ApiResponse.onSuccess(SuccessCode.PROFILE_CREATED, null);
    }

    @Operation(summary = "FCM 토큰 갱신", description = "앱 시작마다 호출해 최신 토큰 유지")
    @PatchMapping("/fcm-token")
    public ResponseEntity<ApiResponse<Void>> updateFcmToken(
            @Parameter(hidden = true) @LoginUser Long userId,
            @RequestBody UpdateFcmTokenRequest request
    ) {
        userService.updateFcmToken(userId, request.fcmToken());
        return ApiResponse.onSuccess(SuccessCode.PROFILE_UPDATED, null);
    }

    @Operation(summary = "알림 ON/OFF 변경")
    @PatchMapping("/push-enabled")
    public ResponseEntity<ApiResponse<Void>> updatePushEnabled(
            @Parameter(hidden = true) @LoginUser Long userId,
            @RequestBody UpdatePushEnabledRequest request
    ) {
        userService.updatePushEnabled(userId, request.pushEnabled());
        return ApiResponse.onSuccess(SuccessCode.PROFILE_UPDATED, null);
    }
}
