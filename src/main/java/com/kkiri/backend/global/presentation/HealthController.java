package com.kkiri.backend.global.presentation;

import com.kkiri.backend.global.common.ApiResponse;
import com.kkiri.backend.global.exception.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "서버 상태 확인")
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @Operation(summary = "헬스 체크")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Void>> health() {
        return ApiResponse.onSuccess(SuccessCode.HEALTH_CHECK, null);
    }
}
