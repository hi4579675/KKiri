package com.kkiri.backend.global.exception;

import lombok.Getter;


/**
 * 비즈니스 로직에서 의도적으로 던지는 커스텀 예외.
 * BaseErrorCode를 감싸서 RuntimeException으로 만들어줌, Service에서 이걸 사용할거임
 * 사용 예시:
 *   throw new CustomException(ErrorCode._NOT_FOUND);
 */
@Getter
public class CustomException extends RuntimeException{

    private final BaseErrorCode baseErrorCode;

    public CustomException(BaseErrorCode baseErrorCode) {
        // RuntimeException의 message도 채워줌 → 로그에서 e.getMessage()로 바로 확인 가능
        super(baseErrorCode.getReasonHttpStatus().getMessage());
        this.baseErrorCode = baseErrorCode;
    }
}
