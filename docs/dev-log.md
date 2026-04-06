# dev-log



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
- jwt.expiration vs jwt.access-expiration 키 이름 불일치로 앱 실행 실패
  → 해결: application.yml의 키 이름을 jwt.access-expiration으로 통일
- JwtProvider Key 타입 오류 (verifyWith()가 SecretKey 요구)
  → 해결: 필드 타입을 Key → SecretKey로 변경
- SecurityConfig에서 jwtProvider에 final 누락 → null 주입으로 필터에서 NPE
  → 해결: private final JwtProvider jwtProvider로 수정
- WebClient 내부 private static 클래스로 JSON 파싱 실패
  → 해결: Map<String, Object>로 파싱 방식 변경

  

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
## 2026-04-06 (Phase 5.5 — 프론트엔드 연동 버그 수정)

> 백엔드 Phase 5까지 완성 후 React Native(Expo) 프론트엔드를 실제로 붙이는 과정에서
> 발생한 백엔드 버그 및 설계 불일치를 수정한 기록.
> 발생한 문제 전부 "백엔드 단독 Swagger 테스트에서는 정상 → 프론트 붙이니 터짐" 패턴.

**한 것**
- `PostController`, `FeedController` 전 엔드포인트 `ApiResponse` 래퍼 누락 수정
- `PresignedUrlRequest`, `PresignedUrlResponse` 빈 클래스 → 필드 있는 record로 수정
- `PostResponse` flat 구조로 변경 (중첩 author 객체 제거, userId·nickname·avatarEmoji·avatarColor 최상위 필드)
- `GroupController`: `GET /api/groups/my`, `GET /api/groups/{groupId}/members` 추가,
  FeedController와 충돌하던 `GET /api/groups/{groupId}/feed` 매핑 제거
- `GroupService.getMyGroups()`, `getMembers()` 구현
- `GroupMemberRepository.findByUserId()` JPQL 쿼리 추가
- `GroupMemberResponse` DTO 추가
- `JwtAuthenticationFilter` 토큰 오류 처리 방식 변경 (throw → HttpServletResponse에 직접 401 작성)
- `ProfileCompleteFilter` 예외 전파 방식 변경 (throw → HttpServletResponse에 직접 403 작성)

**막힌 것 & 해결법**

**① `res.data.data === undefined` — 프론트 전체가 undefined를 받음**
- 증상: 피드 조회, presigned URL 발급, 포스트 생성 등 모든 API 호출 결과가 `undefined`
- 원인: 프론트 axios 클라이언트는 백엔드 응답이 `{ success, code, message, data }` 구조라 가정하고
  `res.data.data`로 언래핑하는데, `PostController`와 `FeedController`가 `ResponseEntity.ok(rawData)` 형태로
  래퍼 없이 반환하고 있었음. Swagger는 raw 데이터를 그대로 보여주므로 단독 테스트 때 문제가 안 보였던 것.
- 해결: 전 엔드포인트 `ApiResponse.onSuccess(SuccessCode.XXX, data)` 형식으로 통일
  → 백엔드 `ApiResponse<T>` 래퍼는 프론트 axios 클라이언트 설계와 반드시 맞춰야 함.
  Swagger 테스트만으로는 이 불일치를 잡기 어려우므로 프론트 연동 초기에 한 엔드포인트라도 먼저 검증 필요.

**② `cannot find symbol: fileName(), contentType()` — 빌드 자체가 안 됨**
- 증상: `./gradlew bootRun` 시 컴파일 에러. `PresignedUrlRequest.fileName()`, `contentType()` 메서드를 찾지 못함.
- 원인: `PresignedUrlRequest`, `PresignedUrlResponse`가 `public record PresignedUrlRequest {}` 형태의 빈 record였음.
  Phase 5 구현 시 파일만 생성하고 필드 정의를 빠트린 것.
- 해결: `record PresignedUrlRequest(@NotBlank String fileName, @NotBlank String contentType)`,
  `record PresignedUrlResponse(String presignedUrl, String imageKey, String imageUrl)` 로 수정.

**③ `Ambiguous mapping` — 앱 기동 자체가 실패**
- 증상: Spring Boot 시작 시 `Ambiguous mapping. Cannot map 'feedController' method... GET /api/groups/{groupId}/feed` 에러로 기동 불가.
- 원인: `GET /api/groups/{groupId}/feed`가 `GroupController`와 `FeedController` 두 곳에 동시에 선언.
  그룹 관련 기능을 GroupController에 추가하는 과정에서 FeedController에 이미 있던 동일 경로를 중복 등록.
- 해결: GroupController에서 해당 매핑 제거. 피드는 FeedController 단독 관리.

**④ `GET /api/groups/my` 403 Forbidden — 로그인 직후 그룹 조회 항상 실패**
- 증상: 프론트 로그인 완료 후 `GET /api/groups/my` 호출 시 항상 403.
  Swagger에서 Bearer 토큰 입력 후 동일 엔드포인트 호출하면 정상 200.
- 원인 분석:
  - Spring Security 필터 체인 순서:
    `JwtAuthenticationFilter(4799)` → `ProfileCompleteFilter(4801)` → ... → `ExceptionTranslationFilter(10000)` → `AuthorizationFilter(10500)`
  - `ExceptionTranslationFilter`는 자신보다 **뒤에 오는** 필터를 try-catch로 감싸는 구조.
    즉, 4799번 `JwtAuthenticationFilter`가 throw한 예외는 10000번 `ExceptionTranslationFilter`가 잡지 못함.
  - `validateToken()`이 만료/유효하지 않은 토큰에서 `CustomException`을 throw하면 SecurityContext에
    인증 정보가 설정되지 않은 채 예외가 전파됨.
  - 예외가 서블릿 컨테이너에 도달하지 않고, 인증 없는 상태로 `AuthorizationFilter`까지 내려가
    익명 사용자 판단 → `AccessDeniedException` → `ExceptionTranslationFilter` → `Http403ForbiddenEntryPoint` → 403.
  - `formLogin`, `httpBasic` 미설정 시 Spring Security 기본 entry point가 `Http403ForbiddenEntryPoint`이므로
    인증 미완료 상태에서 보호 엔드포인트 접근 시 401이 아닌 403이 나옴.
  - 토큰이 없거나 토큰 파싱에서 예외가 발생한 경우 모두 이 경로로 403이 반환돼, axios의 401 리프레시
    인터셉터가 아예 동작하지 않는 구조적 문제.
- 해결: `JwtAuthenticationFilter` 내부에서 `validateToken()` 호출을 try-catch로 감싸고,
  예외 발생 시 `HttpServletResponse`에 직접 401 JSON 작성 후 필터 체인 중단.
  → 이제 토큰 오류 시 프론트가 401을 받아 axios 리프레시 인터셉터가 동작해 토큰 갱신 후 재시도 가능.

**⑤ `ProfileCompleteFilter`의 예외가 `@RestControllerAdvice`에 안 잡힘**
- 증상: 프로필 미완료 유저가 API 호출 시 `GlobalExceptionHandler`의 `handleCustomException`이 동작하지 않고
  의도한 400 대신 이상한 상태코드로 응답.
- 원인: `@RestControllerAdvice`는 DispatcherServlet이 처리하는 컨트롤러 계층의 예외만 잡음.
  `ProfileCompleteFilter`는 Servlet Filter 계층에서 동작하므로 throw한 `CustomException`이 ControllerAdvice에
  전달되지 않음. 예외가 서블릿 컨테이너까지 전파 → Spring Boot `BasicErrorController` 처리.
  `CustomException`에 `@ResponseStatus`가 없으므로 의도한 HTTP 상태가 아닌 예상치 못한 값으로 응답.
- 해결: throw 대신 `HttpServletResponse`에 직접 JSON 응답 작성 후 return.
  필터에서 예외를 throw해서 ControllerAdvice가 처리해줄 것을 기대하는 건 잘못된 가정.
  Filter 계층에서 HTTP 응답을 직접 제어해야 함.

**⑥ `PostResponse` 구조 불일치 — 프론트 닉네임·아바타 렌더링 안 됨**
- 증상: 피드 카드에 닉네임, 아바타가 undefined. `post.author.nickname` 접근 시 오류.
- 원인: 백엔드 `PostResponse`가 `author { userId, nickname, avatarEmoji, avatarColor }` 중첩 구조였으나
  프론트 타입 정의와 컴포넌트는 `post.nickname`, `post.avatarEmoji` 형태의 flat 구조를 기대.
- 해결: `PostResponse`를 flat record로 변경.
  중첩 DTO는 프론트 입장에서 접근 경로가 길어지고 null 체크가 복잡해지므로 단순 flat이 연동에 유리.

**⑦ 로그인 후/앱 재시작 후 그룹 상태 초기화 — Zustand 영속성 미구현**
- 증상: 앱을 껐다 켜거나 로그인할 때마다 그룹이 없는 빈 상태로 피드 진입.
- 원인: Zustand 스토어는 인메모리. 앱 재시작 시 전부 초기 상태로 리셋.
  `app/index.tsx`(스플래시)에서만 `getMyGroups()`를 호출하도록 구성했는데,
  카카오 로그인 완료 후 `login.tsx`에서 `/(app)/feed`로 직접 navigate하면 스플래시를 거치지 않아
  `getMyGroups()` 호출이 빠지는 경로가 존재.
- 해결:
  1. `login.tsx` 로그인 성공 콜백에서도 `getMyGroups()` → `setGroups()` 호출
  2. `groupStore.setGroups()` 내부에서 자동으로 `activeGroupId = groups[0].groupId` 설정
  3. 로그아웃 시 `groupStore.clear()`, `feedStore.clear()` 호출해 다른 계정 로그인 시 이전 상태 잔류 방지

**다음에 할 것**
- `joinGroup` 테스트용 임시 코드 → 운영용으로 원복 (`ALREADY_IN_GROUP` 에러 반환)
- Phase 6: 피드 조회 API (커서 기반 페이지네이션)
- FCM 서비스 계정 JSON Railway 환경변수 연결

---

## 2026-04-06 (Phase 6 — 피드 조회)

**한 것**
- `PostRepository` — 피드용 JPQL 쿼리 2개 추가
  - `findFeed`: 오늘 날짜 + `archived=false` + `hourBucket <= cursor` 조건, `JOIN FETCH p.user`로 N+1 방지
  - `findTodayContributors`: 오늘 포스트 올린 유저 distinct 조회 (아바타 바용)
- `FeedService` — 커서 기반 피드 조회 로직
  - cursor 없으면 현재 UTC hour 기준으로 조회 시작
  - 포스트를 `hourBucket`별로 그룹핑 → `BucketResponse` 리스트 생성
  - 다음 페이지 존재 시 `nextCursor = lastHourBucket - 1` 반환
  - 그룹 전체 멤버 조회 + 오늘 포스트 여부 플래그(`hasPostedToday`) 조합
- `FeedController` — `GET /api/groups/{groupId}/feed` 엔드포인트
- DTO 추가/수정
  - `ContributorResponse` (신규): userId, nickname, avatarEmoji, avatarColor, hasPostedToday
  - `BucketResponse` (신규): hourBucket, posts
  - `FeedResponse` (수정): contributors + buckets + nextCursor

**설계 결정**
- 커서를 offset이 아닌 `hourBucket`(0~23)으로 사용
  → 하루 단위 피드이고 시간대별 그룹핑이 목적이므로 페이지 번호보다 hourBucket 커서가 의미있음
  → `nextCursor = null`이면 더 이상 데이터 없음 (0시 bucket이 마지막이면 다음 커서 없음)
- 피드는 항상 오늘(UTC 기준) 데이터만 반환
  → 과거 피드는 Archive 도메인에서 별도 처리 예정
- contributors는 페이지네이션 없이 항상 전체 반환 (그룹 최대 6명이므로 고정 크기)

**다음에 할 것**
- `joinGroup` 테스트용 임시 코드 → 운영용으로 원복 (`ALREADY_IN_GROUP` 에러 반환)
- Phase 7: 아카이브 (자정 배치, 날짜별 피드 조회)
- FCM 서비스 계정 JSON Railway 환경변수 연결

---
