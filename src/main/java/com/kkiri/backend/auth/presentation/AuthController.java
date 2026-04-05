package com.kkiri.backend.auth.presentation;

import com.kkiri.backend.auth.application.AuthService;
import com.kkiri.backend.auth.application.dto.KakaoLoginRequest;
import com.kkiri.backend.auth.application.dto.RefreshRequest;
import com.kkiri.backend.auth.application.dto.TokenResponse;
import com.kkiri.backend.global.common.ApiResponse;
import com.kkiri.backend.global.exception.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 카카오 로그인.
     * 앱이 카카오 SDK로 받은 accessToken을 전달하면 서버 JWT를 발급합니다.
     * profileCompleted = false이면 앱은 프로필 설정 화면으로 이동해야 합니다.
     */
    @PostMapping("/kakao")
    public ResponseEntity<ApiResponse<TokenResponse>> kakaoLogin(@RequestBody KakaoLoginRequest request) {
        TokenResponse response = authService.kakaoLogin(request.accessToken());
        return ApiResponse.onSuccess(SuccessCode.LOGIN_SUCCESS, response);
    }

    /**
     * Access Token 재발급.
     * Access Token 만료 시 Refresh Token으로 새 토큰을 발급받습니다.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@RequestBody RefreshRequest request) {
        TokenResponse response = authService.refresh(request.refreshToken());
        return ApiResponse.onSuccess(SuccessCode.TOKEN_REFRESHED, response);
    }
}
