package com.kkiri.backend.global.common;

import com.kkiri.backend.global.exception.BaseCode;
import com.kkiri.backend.global.exception.BaseErrorCode;
import org.springframework.http.ResponseEntity;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data
) {
    // 성공 응답
    public static <T> ResponseEntity<ApiResponse<T>> onSuccess(BaseCode successCode, T data) {
        var reason = successCode.getReasonHttpStatus();
        return ResponseEntity
                .status(reason.getHttpStatus())
                .body(new ApiResponse<>(true, reason.getCode(), reason.getMessage(), data));
    }

    // 실패 응답
    public static <T> ResponseEntity<ApiResponse<T>> onFailure(BaseErrorCode errorCode) {
        var reason = errorCode.getReasonHttpStatus();
        return ResponseEntity
                .status(reason.getHttpStatus())
                .body(new ApiResponse<>(false, reason.getCode(), reason.getMessage(), null));
    }

    // 실패 응답 (validation 등 커스텀 메시지가 필요한 경우)
    public static <T> ResponseEntity<ApiResponse<T>> onFailure(BaseErrorCode errorCode, String message) {
        var reason = errorCode.getReasonHttpStatus();
        return ResponseEntity
                .status(reason.getHttpStatus())
                .body(new ApiResponse<>(false, reason.getCode(), message, null));
    }

}
