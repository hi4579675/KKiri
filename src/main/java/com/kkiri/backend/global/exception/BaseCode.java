package com.kkiri.backend.global.exception;

import com.kkiri.backend.global.exception.dto.ResponseDto;

public interface BaseCode {
    ResponseDto getReasonHttpStatus();
}
