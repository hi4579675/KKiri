package com.kkiri.backend.auth.presentation;

import com.kkiri.backend.auth.application.UserService;
import com.kkiri.backend.auth.application.dto.CompleteProfileRequest;
import com.kkiri.backend.auth.application.dto.UpdateFcmTokenRequest;
import com.kkiri.backend.auth.application.dto.UpdatePushEnabledRequest;
import com.kkiri.backend.global.common.ApiResponse;
import com.kkiri.backend.global.exception.SuccessCode;
import com.kkiri.backend.global.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 닉네임 중복 확인.
     * 프로필 설정 화면에서 실시간으로 호출 — 인증 불필요 (permitAll).
     */
    @GetMapping("/nickname/check")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkNickname(@RequestParam("nickname") String nickname) {
        boolean available = userService.isNicknameAvailable(nickname);
        return ApiResponse.onSuccess(SuccessCode.NICKNAME_AVAILABLE, Map.of("available", available));
    }
    /**
     * 프로필 최초 설정 (US-02).
     * 완료 후 profileCompleted = true → ProfileCompleteFilter 통과 가능.
     */
    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> completeProfile(
            @LoginUser Long userId,
            @RequestBody CompleteProfileRequest request
            ){
        userService.completeProfile(userId, request);
        return ApiResponse.onSuccess(SuccessCode.PROFILE_CREATED, null);
    }

    /**
     * FCM 토큰 갱신.
     * 앱 시작마다 호출해 최신 토큰 유지.
     */
    @PatchMapping("/fcm-token")
    public ResponseEntity<ApiResponse<Void>> updateFcmToken(
            @LoginUser Long userId,
            @RequestBody UpdateFcmTokenRequest request){
        userService.updateFcmToken(userId, request.fcmToken());
        return ApiResponse.onSuccess(SuccessCode.PROFILE_UPDATED, null);
    }
    /**
     * 알림 ON/OFF 변경.
     */
    @PatchMapping("/push-enabled")
    public ResponseEntity<ApiResponse<Void>> updatePushEnabled(
            @LoginUser Long userId,
            @RequestBody UpdatePushEnabledRequest request) {

        userService.updatePushEnabled(userId, request.pushEnabled());
        return ApiResponse.onSuccess(SuccessCode.PROFILE_UPDATED, null);
    }

}
