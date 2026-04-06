package com.kkiri.backend.auth.presentation;

import com.kkiri.backend.auth.application.AuthService;
import com.kkiri.backend.auth.application.dto.KakaoCodeLoginRequest;
import com.kkiri.backend.auth.application.dto.KakaoLoginRequest;
import com.kkiri.backend.auth.application.dto.RefreshRequest;
import com.kkiri.backend.auth.application.dto.TokenResponse;
import com.kkiri.backend.global.common.ApiResponse;
import com.kkiri.backend.global.exception.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "카카오 로그인", description = "카카오 SDK accessToken으로 서버 JWT 발급. profileCompleted=false면 프로필 설정 필요")
    @PostMapping("/kakao")
    public ResponseEntity<ApiResponse<TokenResponse>> kakaoLogin(@RequestBody KakaoLoginRequest request) {
        TokenResponse response = authService.kakaoLogin(request.accessToken());
        return ApiResponse.onSuccess(SuccessCode.LOGIN_SUCCESS, response);
    }

    @Operation(summary = "카카오 로그인 (인가 코드)", description = "웹 OAuth 플로우용 — 서버가 직접 카카오 토큰 교환 수행")
    @PostMapping("/kakao/code")
    public ResponseEntity<ApiResponse<TokenResponse>> kakaoLoginWithCode(@RequestBody KakaoCodeLoginRequest request) {
        TokenResponse response = authService.kakaoLoginWithCode(request.code(), request.redirectUri());
        return ApiResponse.onSuccess(SuccessCode.LOGIN_SUCCESS, response);
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 Access Token 재발급 (Rotation 방식)")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@RequestBody RefreshRequest request) {
        TokenResponse response = authService.refresh(request.refreshToken());
        return ApiResponse.onSuccess(SuccessCode.TOKEN_REFRESHED, response);
    }
}
