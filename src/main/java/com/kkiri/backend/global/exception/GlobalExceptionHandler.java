package com.kkiri.backend.global.exception;

import com.kkiri.backend.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * [비즈니스 커스텀 예외 처리]
     * Service 계층에서 throw new CustomException(ErrorCode.XXX) 시 호출됨.
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        log.error("[CustomException] code={}, message={}",
                e.getBaseErrorCode().getReasonHttpStatus().getCode(),
                e.getMessage());
        return ApiResponse.onFailure(e.getBaseErrorCode());
    }

    /**
     * [@Valid 검증 실패 처리]
     * Request DTO의 @Valid 검증 실패 시 호출됨.
     * 여러 필드 에러 중 첫 번째 메시지만 반환.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        log.error("[ValidationException] message={}", message);
        return ApiResponse.onFailure(ErrorCode.INVALID_REQUEST, message);
    }

    /**
     * [예상치 못한 서버 에러 처리]
     * 위에서 잡히지 않은 모든 예외의 최종 처리.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("[UnhandledException] message={}", e.getMessage(), e);
        return ApiResponse.onFailure(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
