package com.kkiri.backend.global.exception;

import com.kkiri.backend.global.exception.dto.ResponseDto;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuccessCode implements BaseCode {

    // Health
    HEALTH_CHECK(HttpStatus.OK, "서버가 정상 동작 중입니다."),

    // Auth
    LOGIN_SUCCESS(HttpStatus.OK, "로그인에 성공했습니다."),
    TOKEN_REFRESHED(HttpStatus.OK, "토큰이 갱신되었습니다."),

    // User
    PROFILE_CREATED(HttpStatus.CREATED, "프로필이 생성되었습니다."),
    PROFILE_UPDATED(HttpStatus.OK, "프로필이 수정되었습니다."),
    NICKNAME_AVAILABLE(HttpStatus.OK, "사용 가능한 닉네임이에요."),


    // Group
    GROUP_CREATED(HttpStatus.CREATED, "그룹이 생성되었습니다."),
    GROUP_JOINED(HttpStatus.OK, "그룹에 합류했습니다."),
    GROUP_LEFT(HttpStatus.OK, "그룹에서 나왔습니다."),
    INVITE_CODE_REISSUED(HttpStatus.OK, "초대 코드가 재발급되었습니다."),
    GROUP_FETCHED(HttpStatus.OK, "그룹 정보 조회에 성공했습니다."),

    // Post
    POST_CREATED(HttpStatus.CREATED, "포스트가 업로드되었습니다."),
    POST_DELETED(HttpStatus.OK, "포스트가 삭제되었습니다."),
    REACTION_TOGGLED(HttpStatus.OK, "반응이 반영되었습니다."),
    FEED_FETCHED(HttpStatus.OK, "피드 조회에 성공했습니다."),
    PRESIGNED_URL_ISSUED(HttpStatus.OK, "업로드 URL이 발급되었습니다."),

    // Archive
    ARCHIVE_FETCHED(HttpStatus.OK, "아카이브 조회에 성공했습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public ResponseDto getReasonHttpStatus() {
        return ResponseDto.builder()
                .httpStatus(status)
                .code(name())
                .message(message)
                .isSuccess(true)
                .build();
    }
}
