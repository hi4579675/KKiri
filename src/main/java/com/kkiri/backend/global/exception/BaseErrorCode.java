package com.kkiri.backend.global.exception;

import com.kkiri.backend.global.exception.dto.ResponseDto;

/**
 * 에러 코드 Enum(ErrorCode)이 구현해야 할 인터페이스.
 * 에러 코드라면 status랑 message는 반드시 가져야해를 강제하는 인터페이스임
 * ApiResponse.onFailure()와 GlobalExceptionHandler가 이 타입만 받도록 강제함.
 */
public interface BaseErrorCode {
    ResponseDto getReasonHttpStatus();
}
