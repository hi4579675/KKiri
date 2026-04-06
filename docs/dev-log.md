# dev-log

## 2026-04-06 (Phase 5 — 포스트 도메인)

**한 것**
- `Post` 엔티티 구현 (`hourBucket` 자동 기록, `archived` 필드, 팩토리 메서드)
- `PostRepository` 구현
- `R2Config` — S3Client, S3Presigner 빈 등록 (Cloudflare R2 연동)
- `R2StorageAdapter` — presigned PUT URL 생성 (15분 유효), 비동기 오브젝트 삭제
- `AsyncConfig` — `@EnableAsync` 등록
- `FcmConfig` — Firebase Admin SDK 초기화 (서비스 계정 JSON 환경변수로 관리)
- `FcmService` — 포스트 업로드 시 그룹 멤버 FCM 푸시 비동기 발송
- `PostService` — presigned URL 발급 / 포스트 생성 / 포스트 삭제
- `PostController` — 3개 엔드포인트 구현 (US-05, US-06, US-07)
- DTO: `PresignedUrlRequest`, `PresignedUrlResponse`, `CreatePostRequest`, `PostResponse`
- `ErrorCode.ARCHIVED_POST_DELETE_FORBIDDEN` 추가
- `SwaggerConfig`, 전체 컨트롤러 Swagger 어노테이션 적용 (`@Parameter(hidden = true)`)

**막힌 것 & 해결법**
- R2 presigned URL PUT 400 Bad Request
  → Postman이 `Cache-Control`, `Postman-Token` 헤더 자동 추가 → 서명 불일치
  → curl로 테스트하니 바로 성공 (`Content-Type` 헤더 하나만 넣어야 함)
- Swagger에서 `@LoginUser` 파라미터가 쿼리 파라미터로 노출됨
  → `@Parameter(hidden = true)` 추가로 해결
- Swagger Authorize 자물쇠 버튼 없음
  → `SwaggerConfig` 신규 생성, Bearer SecurityScheme 등록

**다음에 할 것**
- Phase 6: 피드 조회 (시간대별 그룹핑, 친구 스토리)
- FCM 서비스 계정 JSON 환경변수 연결 (Railway)
- `joinGroup` 테스트용 코드 → 운영용으로 원복

---

## 2025-04-03

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



## 2026-04-04

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

---

## 2026-04-05 (Railway 배포 + 실제 카카오 로그인 검증)

**한 것**
- `application-prod.yml` Redis 비밀번호 설정 추가 (`spring.data.redis.password`)
- `.gitignore`에서 `application-prod.yml` 제외 (환경변수 참조만 있어 커밋 안전)
- Railway KKiri 서비스 환경변수 설정 (DB, Redis, JWT, Kakao 등)
- Railway 도메인 발급 (`kkiri-production.up.railway.app`)
- `build.gradle` Java toolchain → `sourceCompatibility`/`targetCompatibility`로 변경
- `Procfile` 추가 (JAR 실행 경로 직접 지정)
- Railway 배포 후 실제 카카오 로그인 + 프로필 설정 검증 완료

**막힌 것 & 해결법**
- Railway 빌드 실패: Java 17 못 찾음
  → `toolchain { languageVersion = JavaLanguageVersion.of(17) }` 방식이 Railway에서 특정 JDK 설치를 탐색하다 실패
  → `sourceCompatibility = JavaVersion.VERSION_17`로 변경해 시스템 Java 그대로 사용
- Railway 배포 후 Crashed: JAR 파일 못 찾음
  → Railway 기본 실행 명령어가 `*/build/libs/*.jar`로 탐색하는데 실제 경로는 `build/libs/`
  → `Procfile`에 실행 명령어 직접 지정
- 앱 실행 시 `local` 프로파일 활성화 (`application.yml`에 `spring.profiles.active: local` 하드코딩됨)
  → Railway 환경변수 `SPRING_PROFILES_ACTIVE=prod`가 안 먹힘
  → `Procfile`에 `-Dspring.profiles.active=prod` 직접 추가
- `${DB_URL}` 리터럴 문자열로 전달됨
  → Railway Variables에서 `${{Postgres.PGHOST}}` 참조 변수가 체인 해석이 안 됨
  → Postgres 서비스 Connect 탭에서 실제 내부 URL 확인 후 직접 입력
  → `DB_URL=jdbc:postgresql://postgres.railway.internal:5432/railway`

**다음에 할 것**
- Phase 4: 그룹 도메인 (Group, GroupMember 엔티티, 그룹 생성/참여/탈퇴/초대코드 재발급)

## 2026-04-06 (Phase 4 — 그룹 도메인)

**한 것**
- `Group`, `GroupMember`, `GroupRole` 엔티티 구현
  - `Group.create()` 팩토리 메서드 + 6자리 UUID 기반 초대 코드 자동 생성 (7일 만료)
  - `GroupMember` 연결 엔티티로 User ↔ Group 다대다 풀기 (`@ManyToOne LAZY`)
  - `GroupRole` enum (OWNER / MEMBER)
- `GroupRepository`, `GroupMemberRepository` 구현
  - `findByInviteCode`, `findByGroupIdAndUserId`, `countByGroupId`, `existsByGroupIdAndUserId` 등
- `GroupService` 구현 (그룹 생성 / 초대 코드 조회·갱신 / 참여 / 강퇴 / 탈퇴 / 방장 위임)
- `GroupController` 구현 (7개 엔드포인트, US-03, US-04)
- DTO: `CreateGroupRequest`, `GroupResponse`, `InviteCodeResponse`, `JoinGroupRequest`, `JoinGroupResponse`, `TransferOwnerRequest`
- `ErrorCode` 추가: `CANNOT_KICK_OWNER`, `INVITE_CODE_NOT_EXPIRED`
- `SwaggerConfig` 추가 — JWT Bearer 자물쇠 버튼 활성화
- 전체 컨트롤러 Swagger 어노테이션 적용 (`@Tag`, `@Operation`, `@Parameter(hidden = true)`)

**막힌 것 & 해결법**
- Swagger에서 `@LoginUser` 파라미터가 `userId` 쿼리 파라미터로 노출됨
  → `@Parameter(hidden = true)` 추가로 숨김 처리
- Swagger 자물쇠 버튼 없음 → `SwaggerConfig` 자체가 없었음
  → `SwaggerConfig` 신규 생성, `SecurityScheme` Bearer 방식 등록
- `/api/groups` 호출 시 403
  → Swagger에서 JWT 토큰 없이 호출했기 때문, Authorize 버튼으로 Bearer 토큰 입력 후 해결
- `joinGroup` 동일 계정으로 재호출 시 `ALREADY_IN_GROUP` 에러
  → 테스트 편의상 이미 멤버면 에러 없이 현재 그룹 정보 반환하도록 임시 처리 (운영 전 원복 필요)

**다음에 할 것**
- Phase 5: 게시물 업로드 (Presigned URL + Cloudflare R2)

---
