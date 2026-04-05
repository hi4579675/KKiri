package com.kkiri.backend.global.exception;

import com.kkiri.backend.global.exception.dto.ResponseDto;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements BaseErrorCode {

    // Auth
    KAKAO_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "카카오 로그인에 실패했습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    PROFILE_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "프로필을 먼저 설정해주세요."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임이에요."),


    // Group
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "그룹을 찾을 수 없습니다."),
    INVALID_INVITE_CODE(HttpStatus.BAD_REQUEST, "존재하지 않는 코드예요."),
    EXPIRED_INVITE_CODE(HttpStatus.BAD_REQUEST, "만료된 초대 코드예요."),
    GROUP_FULL(HttpStatus.BAD_REQUEST, "그룹이 가득 찼어요."),
    ALREADY_IN_GROUP(HttpStatus.BAD_REQUEST, "이미 그룹에 속해 있습니다."),
    NOT_GROUP_MEMBER(HttpStatus.FORBIDDEN, "그룹 멤버가 아닙니다."),
    NOT_GROUP_OWNER(HttpStatus.FORBIDDEN, "그룹 방장이 아닙니다."),

    // Post
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "포스트를 찾을 수 없습니다."),
    FORBIDDEN_POST(HttpStatus.FORBIDDEN, "본인 포스트만 삭제할 수 있습니다."),
    IMAGE_TOO_LARGE(HttpStatus.BAD_REQUEST, "사진이 너무 커요."),

    // Common
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // ─── JWT / Token ──────────────────────────────────────────
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED,  "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED,  "토큰이 존재하지 않습니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    BLACKLISTED_TOKEN(HttpStatus.UNAUTHORIZED,  "로그아웃 처리된 토큰입니다.");



    private final HttpStatus status;
    private final String message;

    @Override
    public ResponseDto getReasonHttpStatus() {
        return ResponseDto.builder()
                .httpStatus(status)
                .code(name())
                .message(message)
                .isSuccess(false)
                .build();
    }
}
