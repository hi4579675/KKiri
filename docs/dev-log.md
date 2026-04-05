# dev-log

## 2026-04-06

**한 것**
- Phase 3 프로필 설정 구현
    - `UserService` (닉네임 중복 확인, 프로필 완료, FCM 토큰, 알림 설정)
    - `UserController` (`GET /api/users/me/nickname/check`, `POST /api/users/me/profile`, `PATCH /api/users/me/fcm-token`, `PATCH /api/users/me/push-enabled`)
    - `ProfileCompleteFilter` (프로필 미완료 유저 API 접근 차단)
    - DTO: `CompleteProfileRequest`, `UpdateFcmTokenRequest`, `UpdatePushEnabledRequest`
    - `UserRepository.existsByNickname()` 추가
    - `ErrorCode.DUPLICATE_NICKNAME`, `SuccessCode.NICKNAME_AVAILABLE` 추가
- test.http Phase 3 테스트 케이스 작성

**막힌 것 & 해결법**
- 카카오 로그인 401 (`ip mismatched`)
  → 카카오 개발자 콘솔 플랫폼 키 > 호출 허용 IP 주소에 등록된 IP 삭제 (로컬 개발 중에는 비워두기)
  → 인증 어플 설정을 잘못해서 accessToken을 이상한 걸 발급 받았었음...(바보)
- 프로필 설정 500 NPE (`Cannot invoke UpdateAudit.touch() because this.updateAudit is null`)
  → JPA는 `@Embedded` 컬럼이 전부 null이면 객체 자체를 null로 로드함
  → `User`에 `private touch()` 헬퍼 추가해 null이면 초기화 후 touch
- `ProfileCompleteFilter`가 JWT 앞에서 실행돼 필터 무력화
  → `addFilterBefore` → `addFilterAfter`로 수정 (JwtAuthenticationFilter 이후에 실행돼야 SecurityContext에 인증 정보가 있음)
- `GET /api/users/me/fcm-token` 호출 시 500
  → 해당 경로는 `PATCH`만 존재 → GlobalExceptionHandler가 405를 500으로 래핑한 것
  → test.http를 PATCH로 수정

**다음에 할 것**
- Phase 4: 그룹 도메인 (Group, GroupMember 엔티티, 그룹 생성/참여/탈퇴/초대코드 재발급)
- Railway 배포 후 실제 카카오 로그인 검증

## 2025-04-04

**한 것**
- KakaoAuthClient 구현 (카카오 유저 정보 API 호출)
- UserRepository, RefreshTokenRepository 구현
- RedisConfig, CorsConfig 추가
- AuthService 구현 (카카오 로그인, Refresh Token Rotation)
- AuthController 구현 (POST /api/auth/kakao, POST /api/auth/refresh)
- TokenResponse, KakaoLoginRequest, RefreshRequest DTO 구현
- Swagger UI 추가 (/swagger-ui/index.html)
- 로컬 테스트용 /api/auth/dev/login 엔드포인트 추가
 

**막힌 것**
- 카카오 IP 제한 문제: 로컬 IP(183.109.80.187)가 카카오 API에서 차단됨
- jwt.expiration vs jwt.access-expiration 키 이름 불일치로 앱 실행 실패
  → 해결: application.yml의 키 이름을 jwt.access-expiration으로 통일
- JwtProvider Key 타입 오류 (verifyWith()가 SecretKey 요구)
  → 해결: 필드 타입을 Key → SecretKey로 변경
- SecurityConfig에서 jwtProvider에 final 누락 → null 주입으로 필터에서 NPE
  → 해결: private final JwtProvider jwtProvider로 수정
- WebClient 내부 private static 클래스로 JSON 파싱 실패
  → 해결: Map<String, Object>로 파싱 방식 변경


**다음에 할 것**
- Phase 3: 프로필 설정 API (US-02)
- 


## 2025-04-03

**한 것**
 

**막힌 것**
 

**다음에 할 것**
 