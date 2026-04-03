package com.kkiri.backend.global.exception.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@Builder
public class ResponseDto {
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
    private final boolean isSuccess; // 성공이면 true, 실패면 false

}
