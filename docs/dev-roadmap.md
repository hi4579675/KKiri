# 끼리 개발 로드맵

> 에러 핸들링 완료 이후 구현 순서.
> 각 Phase는 이전 Phase가 완료되어야 시작 가능합니다.

---

## 현재 완료된 것

- [x] 프로젝트 셋업 (Spring Boot 3, Railway 배포)
- [x] 글로벌 예외 처리 (`GlobalExceptionHandler`, `ErrorCode`, `CustomException`)
- [x] 공통 응답 포맷 (`ApiResponse`)
- [x] Audit 클래스 (`CreateAudit`, `UpdateAudit`, `SoftDeleteAudit`)
- [x] `SecurityConfig` 기본 골격
- [x] `User` 엔티티 기본 골격

---

## Phase 1 — JWT 인증 인프라

> 모든 API에 인증이 걸리므로 가장 먼저 구현합니다.
> 이게 안 되면 다른 API를 테스트할 수 없습니다.

### 1-1. `JwtProvider`

**파일:** `global/security/JwtProvider.java`

```
구현할 메서드:

generateAccessToken(Long userId) → String
  - userId를 subject로, 만료 1시간 JWT 생성
  - HS256 알고리즘 + application.yml의 jwt.secret 키 사용

generateRefreshToken(Long userId) → String
  - 만료 7일 JWT 생성 (payload 동일, 만료만 다름)

getUserIdFromToken(String token) → Long
  - token에서 subject(userId) 파싱 후 Long 변환

validateToken(String token) → boolean
  - 서명 검증 + 만료 체크
  - 실패 시 false 반환 (예외 던지지 않음, Filter에서 분기 처리)
```

**application.yml에 추가할 것:**
```yaml
jwt:
  secret: {base64 인코딩된 256bit 이상 랜덤 문자열}
  access-expiration: 3600000    # 1시간 (ms)
  refresh-expiration: 604800000 # 7일 (ms)
```

---

### 1-2. `JwtAuthenticationFilter`

**파일:** `global/security/JwtAuthenticationFilter.java`

```
구현할 메서드:

doFilterInternal(request, response, chain)
  1. request.getHeader("Authorization")에서 "Bearer {token}" 추출
  2. token이 없으면 → chain.doFilter() 통과 (permitAll 경로)
  3. JwtProvider.validateToken(token) 실패 → ErrorCode.INVALID_TOKEN 응답 (401)
  4. 성공 시 → JwtProvider.getUserIdFromToken(token)으로 userId 추출
  5. UsernamePasswordAuthenticationToken 생성 후 SecurityContextHolder에 저장

extractToken(request) → Optional<String>
  - "Bearer " prefix 제거 후 토큰 문자열 반환
  - 헤더 없으면 Optional.empty()
```

---

### 1-3. `SecurityConfig` 수정

**파일:** `global/config/SecurityConfig.java`

```
변경할 부분:

- JwtProvider @RequiredArgsConstructor 주입 방식으로 변경
- permitAll 경로에 "/api/auth/**" 확인
- JwtAuthenticationFilter를 Bean이 아닌 생성자 주입으로 변경
  (필터를 Bean으로 등록하면 Spring이 자동으로 필터 체인에 두 번 추가하는 문제 발생)
```

---

### 1-4. `LoginUser` 어노테이션 + `LoginUserArgumentResolver`

**파일:** `global/security/LoginUser.java` (커스텀 어노테이션)
**파일:** `global/security/LoginUserArgumentResolver.java`

```
이유: 컨트롤러마다 SecurityContextHolder.getContext()를 직접 꺼내면 중복코드가 많아짐.
     @LoginUser Long userId 어노테이션으로 간결하게 주입.

구현:
LoginUser - @Target(PARAMETER) @Retention(RUNTIME) 어노테이션

LoginUserArgumentResolver.resolveArgument()
  - SecurityContextHolder에서 Authentication 꺼내기
  - principal을 Long으로 캐스팅해서 반환

WebMvcConfig에 addArgumentResolvers()로 등록
```

**사용 예시 (이후 컨트롤러에서):**
```java
@GetMapping("/api/groups/{id}/feed")
public ResponseEntity<?> getFeed(@LoginUser Long userId, @PathVariable Long id) { ... }
```

---

## Phase 2 — 카카오 OAuth2 로그인 (US-01)

> Phase 1 완료 후 진행. JWT가 동작해야 로그인 응답을 만들 수 있습니다.

### 2-1. `KakaoAuthClient`

**파일:** `auth/infrastructure/KakaoAuthClient.java`

```
이유: 카카오 API는 외부 HTTP 호출 → infrastructure 레이어에 위치.
     WebClient(WebFlux 의존성 이미 있음) 사용.

구현할 메서드:

getUserInfo(String accessToken) → KakaoUserInfo
  - GET https://kapi.kakao.com/v2/user/me
  - Authorization: Bearer {accessToken} 헤더 추가
  - 응답에서 id(kakaoId), kakao_account.profile.nickname 파싱
  - 실패 시 CustomException(ErrorCode.KAKAO_LOGIN_FAILED)
```

**KakaoUserInfo DTO (내부 record):**
```java
record KakaoUserInfo(String kakaoId, String nickname) {}
```

---

### 2-2. `RefreshTokenRepository`

**파일:** `auth/infrastructure/RefreshTokenRepository.java`

```
이유: Refresh Token은 DB가 아닌 Redis에 저장 (빠른 조회 + TTL 자동 만료).

구현:
  - StringRedisTemplate 또는 RedisTemplate<String, String> 사용
  - key: "refresh:{userId}", value: refreshToken

save(Long userId, String refreshToken, Duration ttl)
  - redisTemplate.opsForValue().set("refresh:" + userId, refreshToken, ttl)

findByUserId(Long userId) → Optional<String>
  - redisTemplate.opsForValue().get("refresh:" + userId)

delete(Long userId)
  - redisTemplate.delete("refresh:" + userId)
```

**RedisConfig 생성 필요:**
```
파일: global/config/RedisConfig.java
- RedisConnectionFactory Bean
- StringRedisTemplate Bean
- application.yml에 spring.data.redis.host/port 추가
```

---

### 2-3. `AuthService`

**파일:** `auth/application/AuthService.java`

```
구현할 메서드:

kakaoLogin(String kakaoAccessToken) → TokenResponse
  1. KakaoAuthClient.getUserInfo(kakaoAccessToken) 호출
  2. UserRepository.findByKakaoId(kakaoId) 조회
  3. 없으면 → User.createPending(kakaoId) 로 신규 유저 저장
     (nickname, avatar는 null인 "미완료" 상태, profileCompleted = false)
  4. JwtProvider.generateAccessToken(user.getId())
  5. JwtProvider.generateRefreshToken(user.getId())
  6. RefreshTokenRepository.save(userId, refreshToken, 7일)
  7. TokenResponse(accessToken, refreshToken, isNewUser) 반환

refresh(String refreshToken) → TokenResponse
  1. JwtProvider.validateToken(refreshToken) 검증
  2. JwtProvider.getUserIdFromToken(refreshToken)으로 userId 추출
  3. RefreshTokenRepository.findByUserId(userId) 조회
  4. 저장된 토큰과 입력 토큰 일치 여부 확인 (불일치 시 INVALID_TOKEN)
  5. 새 Access Token + 새 Refresh Token 재발급 (Refresh Token Rotation)
  6. RefreshTokenRepository에 새 Refresh Token으로 덮어쓰기
  7. 새 TokenResponse 반환
```

---

### 2-4. `AuthController`

**파일:** `auth/presentation/AuthController.java`

```
POST /api/auth/kakao
  - body: { "accessToken": "카카오에서받은토큰" }
  - AuthService.kakaoLogin() 호출
  - 응답: TokenResponse + isNewUser 플래그

POST /api/auth/refresh
  - body: { "refreshToken": "..." }
  - AuthService.refresh() 호출
  - 응답: 새 TokenResponse
```

---

### 2-5. `User` 엔티티 수정

**파일:** `auth/domain/User.java`

```
추가/변경할 필드:

- profileCompleted: boolean (default false)
  → 닉네임 미설정 유저 구분용. 프로필 설정 완료 시 true로 변경

- nickname: @Column(unique = true)
  → 글로벌 중복 불가

- pushEnabled: boolean (default true)
  → 알림 on/off

추가할 팩토리 메서드:

User.createPending(String kakaoId)
  → nickname=null, profileCompleted=false 상태로 생성
  → 카카오 로그인 최초 진입 시 사용

추가할 비즈니스 메서드:

completeProfile(String nickname, String avatarEmoji, String avatarColor)
  → this.nickname = nickname
  → this.profileCompleted = true

updatePushEnabled(boolean enabled)
  → this.pushEnabled = enabled
```

---

## Phase 3 — 프로필 설정 (US-02)

> Phase 2 완료 후. 로그인이 되어야 프로필 저장 API를 호출할 수 있습니다.

### 3-1. `ProfileCompleteFilter` (선택적 미들웨어)

**파일:** `global/security/ProfileCompleteFilter.java`

```
이유: 프로필 미완료 유저가 다른 API를 호출하면 막아야 함.

doFilterInternal()
  - /api/auth/**, /api/users/me/profile 은 통과
  - 나머지 경로는 userId로 User 조회
  - profileCompleted = false 이면 → 403 + PROFILE_NOT_COMPLETED

SecurityConfig에서 JwtAuthenticationFilter 뒤에 추가
```

---

### 3-2. `UserService`

**파일:** `auth/application/UserService.java`

```
구현할 메서드:

checkNickname(String nickname) → NicknameCheckResponse
  - UserRepository.existsByNickname(nickname) 조회
  - 응답: { available: true/false }

completeProfile(Long userId, CompleteProfileRequest request)
  - UserRepository.findById(userId) → 없으면 USER_NOT_FOUND
  - UserRepository.existsByNickname(request.nickname()) → 중복이면 DUPLICATE_NICKNAME
  - user.completeProfile(nickname, avatarEmoji, avatarColor) 호출
  - 저장

updateFcmToken(Long userId, String fcmToken)
  - user.updateFcmToken(fcmToken) 호출

updatePushEnabled(Long userId, boolean enabled)
  - user.updatePushEnabled(enabled) 호출
```

---

### 3-3. `UserController`

**파일:** `auth/presentation/UserController.java`

```
GET  /api/users/me/nickname/check?nickname={value}
  - UserService.checkNickname() 호출

POST /api/users/me/profile
  - @LoginUser Long userId
  - body: { nickname, avatarEmoji, avatarColor }
  - UserService.completeProfile() 호출

PATCH /api/users/me/fcm-token
  - body: { fcmToken }

PATCH /api/users/me/push-enabled
  - body: { pushEnabled }
```

---

### 추가할 ErrorCode

```java
DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임이에요.")
```

---

## Phase 4 — 그룹 도메인 (US-03, 04)

> Phase 3 완료 후. 프로필이 있어야 그룹을 만들 수 있습니다.

### 4-1. `Group` 엔티티

**파일:** `group/domain/Group.java`

```
필드:
- id: Long (PK)
- name: String
- inviteCode: String (UNIQUE, 6자리)
- inviteCodeExpiredAt: LocalDateTime
- maxMembers: int (default 6)
- CreateAudit

팩토리 메서드:
Group.create(String name)
  → inviteCode = 랜덤 6자리 영숫자 (UUID 앞 6자 or SecureRandom)
  → inviteCodeExpiredAt = LocalDateTime.now().plusDays(7)

비즈니스 메서드:
isInviteCodeExpired() → boolean
  → LocalDateTime.now().isAfter(inviteCodeExpiredAt)

renewInviteCode()
  → 새 6자리 코드 재발급 + inviteCodeExpiredAt 갱신
```

---

### 4-2. `GroupMember` 엔티티

**파일:** `group/domain/GroupMember.java`

```
필드:
- id: Long (PK)
- group: Group (ManyToOne)
- user: User (ManyToOne)
- role: GroupRole (OWNER / MEMBER)
- joinedAt: LocalDateTime

팩토리 메서드:
GroupMember.createOwner(Group group, User user)
GroupMember.createMember(Group group, User user)

비즈니스 메서드:
isOwner() → boolean
promoteToOwner() → void  ← 오너 위임 시 호출
```

---

### 4-3. `GroupRepository`, `GroupMemberRepository`

```
GroupRepository:
- findByInviteCode(String code) → Optional<Group>

GroupMemberRepository:
- findByGroupIdAndUserId(Long groupId, Long userId) → Optional<GroupMember>
- findByGroupId(Long groupId) → List<GroupMember>
- countByGroupId(Long groupId) → long
- findOwnerByGroupId(Long groupId) → Optional<GroupMember>
- existsByGroupIdAndUserId(Long groupId, Long userId) → boolean
```

---

### 4-4. `GroupService`

**파일:** `group/application/GroupService.java`

```
구현할 메서드:

createGroup(Long userId, CreateGroupRequest request) → GroupResponse
  1. User 조회
  2. Group.create(name)
  3. GroupRepository.save(group)
  4. GroupMember.createOwner(group, user) 저장

getInviteCode(Long userId, Long groupId) → InviteCodeResponse
  - 멤버 여부 검증 (NOT_GROUP_MEMBER)
  - group.isInviteCodeExpired() → 만료 안내 또는 자동 갱신 (정책 결정 필요)
  - inviteCode + expiredAt 반환

renewInviteCode(Long userId, Long groupId) → InviteCodeResponse
  - OWNER 검증 (NOT_GROUP_OWNER)
  - group.renewInviteCode()
  - 저장 후 반환

joinGroup(Long userId, String inviteCode) → JoinGroupResponse
  1. GroupRepository.findByInviteCode(inviteCode) → INVALID_INVITE_CODE
  2. group.isInviteCodeExpired() → EXPIRED_INVITE_CODE
  3. 이미 가입한 경우 → 해당 그룹 정보 반환 (에러 아님)
  4. countByGroupId >= maxMembers → GROUP_FULL
  5. GroupMember.createMember() 저장

kickMember(Long ownerId, Long groupId, Long targetUserId)
  - ownerId 기준 OWNER 검증
  - targetUserId가 OWNER면 → BAD_REQUEST (오너는 강퇴 불가)
  - GroupMember 삭제

leaveGroup(Long userId, Long groupId)
  - GroupMember 조회
  - OWNER인 경우:
    → 남은 멤버 수 조회
    → 0명이면 그룹 삭제
    → 1명 이상이면 랜덤으로 다음 OWNER 선출 후 본인 탈퇴
  - MEMBER인 경우: 바로 탈퇴

transferOwner(Long ownerId, Long groupId, Long targetUserId)
  - OWNER 검증
  - 기존 OWNER → MEMBER
  - targetUserId → OWNER
```

---

### 4-5. `GroupController`

**파일:** `group/presentation/GroupController.java`

```
POST   /api/groups                              createGroup()
GET    /api/groups/{groupId}/invite-code        getInviteCode()
POST   /api/groups/{groupId}/invite-code/renew  renewInviteCode()
DELETE /api/groups/{groupId}/members/{userId}   kickMember()
PATCH  /api/groups/{groupId}/owner              transferOwner()  body: { targetUserId }
DELETE /api/groups/{groupId}/members/me         leaveGroup()
POST   /api/groups/join                         joinGroup()  body: { inviteCode }
```

---

### 추가할 ErrorCode

```java
CANNOT_KICK_OWNER(HttpStatus.BAD_REQUEST, "그룹 오너는 강퇴할 수 없습니다.")
INVITE_CODE_NOT_EXPIRED(HttpStatus.BAD_REQUEST, "초대 코드가 아직 유효합니다.")
```

---

## Phase 5 — 포스트 (US-05, 06)

### 5-1. `Post` 엔티티

**파일:** `post/domain/Post.java`

```
필드:
- id: Long (PK)
- user: User (ManyToOne)
- group: Group (ManyToOne)
- imageUrl: String
- caption: String (nullable, max 30)
- hourBucket: int (0~23)
- archived: boolean (default false)   ← 아카이브 배치 실행 시 true
- CreateAudit, UpdateAudit

팩토리 메서드:
Post.create(User user, Group group, String imageUrl, String caption)
  → hourBucket = LocalDateTime.now().getHour()

비즈니스 메서드:
isOwner(Long userId) → boolean
isArchived() → boolean
archive() → void  ← 배치에서 호출
```

---

### 5-2. `R2StorageAdapter`

**파일:** `post/infrastructure/R2StorageAdapter.java`

```
이유: Cloudflare R2는 S3 호환 API → AWS SDK 사용 (build.gradle에 이미 있음).
     Presigned URL 방식 → 서버가 이미지를 받지 않아도 됨.

구현할 메서드:

generatePresignedUrl(String fileName, String contentType) → PresignedUrlResponse
  - S3Presigner 사용
  - PutObjectPresignRequest 빌드
  - 유효시간: 15분
  - 반환: { presignedUrl, imageKey }  ← imageKey = "posts/{UUID}/{fileName}"

deleteObject(String imageKey)
  - S3Client.deleteObject() 호출
  - 비동기 처리를 위해 @Async 또는 이벤트 발행 고려

필요한 Bean:
S3Client (R2 endpoint URL, accessKey, secretKey 설정)
S3Presigner (동일 설정)
```

**application.yml에 추가:**
```yaml
r2:
  endpoint: https://{accountId}.r2.cloudflarestorage.com
  access-key: ${R2_ACCESS_KEY}
  secret-key: ${R2_SECRET_KEY}
  bucket: ${R2_BUCKET_NAME}
  public-url: https://{your-domain}  ← R2 퍼블릭 URL
```

---

### 5-3. `PostService`

**파일:** `post/application/PostService.java`

```
구현할 메서드:

generatePresignedUrl(Long userId, PresignedUrlRequest request) → PresignedUrlResponse
  - 파일명 sanitize (공백, 특수문자 제거)
  - R2StorageAdapter.generatePresignedUrl() 호출

createPost(Long userId, Long groupId, CreatePostRequest request) → PostResponse
  1. User, Group 조회
  2. 해당 유저가 그룹 멤버인지 검증
  3. Post.create() 저장
  4. FCM 발송 (비동기, Phase 10에서 구현. 지금은 TODO 주석)
  5. WebSocket 발행 (비동기, Phase 9에서 구현. 지금은 TODO 주석)
  6. PostResponse 반환

deletePost(Long userId, Long postId)
  1. Post 조회 → POST_NOT_FOUND
  2. post.isOwner(userId) 검증 → FORBIDDEN_POST
  3. post.isArchived() → true면 → ARCHIVED_POST_DELETE_FORBIDDEN
  4. Post 삭제 (soft delete 또는 hard delete 정책 결정)
  5. R2 이미지 비동기 삭제 (ApplicationEventPublisher로 이벤트 발행)
```

---

### 5-4. `PostController`

```
POST /api/posts/presigned-url    generatePresignedUrl()
POST /api/posts                  createPost()
DELETE /api/posts/{postId}       deletePost()
```

### 추가할 ErrorCode

```java
ARCHIVED_POST_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "아카이브된 포스트는 삭제할 수 없어요.")
NOT_GROUP_MEMBER(HttpStatus.FORBIDDEN, "그룹 멤버가 아닙니다.")   ← 이미 있음 확인
```

---

## Phase 6 — 피드 (US-08)

> Phase 5 완료 후. 포스트가 있어야 피드를 조회할 수 있습니다.

### 6-1. `PostRepository` 확장

```
추가할 쿼리:

findFeedByGroupId(Long groupId, LocalDate date, int cursorHourBucket, Pageable pageable)
  → @Query: group_id = groupId AND DATE(created_at) = date AND hour_bucket <= cursorHourBucket
  → ORDER BY hour_bucket DESC, created_at DESC
  → hour_bucket 단위로 잘리지 않도록 커서를 hour_bucket 기준으로 관리

findTodayContributors(Long groupId, LocalDate today) → List<User>
  → 오늘 포스트를 올린 유저 목록 (아바타 바 표시용)
```

---

### 6-2. `FeedService`

**파일:** `post/application/FeedService.java`

```
getFeed(Long userId, Long groupId, Integer cursorHourBucket) → FeedResponse
  1. 그룹 멤버 검증
  2. PostRepository로 포스트 조회 (오늘, hour_bucket 기준 커서 페이지)
  3. hour_bucket으로 그룹핑 → Map<Integer, List<PostResponse>>
  4. 오늘 포스트 올린 멤버 목록 조회
  5. FeedResponse 조립:
     {
       contributors: [{ userId, nickname, avatarEmoji, avatarColor, hasPostedToday }],
       buckets: [
         { hourBucket: 14, posts: [...] },
         { hourBucket: 12, posts: [...] }
       ],
       nextCursor: 11  ← 다음 페이지 시작 hour_bucket (없으면 null)
     }
```

---

### 6-3. `FeedController`

```
GET /api/groups/{groupId}/feed?cursor={hourBucket}
  - cursor 없으면 현재 시각 hour 기준 최신부터
```

---

## Phase 7 — 반응 + 댓글 (US-10, 11)

### 7-1. `Reaction` 엔티티

**파일:** `reaction/domain/Reaction.java`

```
필드:
- id: Long
- post: Post (ManyToOne)
- user: User (ManyToOne)
- emojiType: String  ← "❤️", "😂", "😮" 등
- CreateAudit

테이블 제약:
@UniqueConstraint(columnNames = {"post_id", "user_id", "emoji_type"})
```

---

### 7-2. `ReactionService`

```
addReaction(Long userId, Long postId, String emojiType)
  1. Post 조회, 그룹 멤버 검증
  2. 이미 같은 이모지 반응 있으면 → 중복 무시 (idempotent)
  3. Reaction 저장

removeReaction(Long userId, Long postId, String emojiType)
  - ReactionRepository.findByPostIdAndUserIdAndEmojiType() 조회
  - 없으면 → NOT_FOUND (또는 무시)
  - 삭제

getReactions(Long postId) → List<ReactionSummaryResponse>
  - 이모지별 카운트 + 본인 반응 여부
  - GROUP BY emoji_type, COUNT(*)
  - 응답: [{ emojiType: "❤️", count: 3, reacted: true }, ...]
```

---

### 7-3. `Comment` 엔티티

**파일:** `comment/domain/Comment.java`

```
필드:
- id: Long
- post: Post (ManyToOne)
- user: User (ManyToOne)
- content: String (max 100)
- CreateAudit, UpdateAudit

비즈니스 메서드:
isOwner(Long userId) → boolean
```

---

### 7-4. `CommentService`

```
getComments(Long postId, Long cursorId) → SliceResponse<CommentResponse>
  - 커서 기반 페이지네이션 (20개 단위, created_at ASC)

addComment(Long userId, Long postId, AddCommentRequest request) → CommentResponse
  1. Post 조회, 그룹 멤버 검증
  2. Comment 저장
  3. 포스트 오너에게 FCM 알림 발송 (TODO → Phase 10)

deleteComment(Long userId, Long commentId)
  - comment.isOwner(userId) 검증
  - 삭제
```

---

## Phase 8 — 아카이브 배치 (US-12)

### 8-1. `ArchivePost` 엔티티

```
필드:
- id: Long
- post: Post (ManyToOne) ← 원본 포스트 참조 유지
- group: Group (ManyToOne)
- archivedDate: LocalDate  ← 배치 기준일 (어제)
- CreateAudit
```

---

### 8-2. `ArchiveBatchService`

**파일:** `archive/application/ArchiveBatchService.java`

```
archiveDailyPosts()  ← @Scheduled(cron = "0 0 0 * * *")에서 호출
  1. yesterday = LocalDate.now().minusDays(1)
  2. PostRepository.findByDateAndArchivedFalse(yesterday) 조회
  3. ArchivePost 리스트 생성 후 bulkInsert (saveAll)
  4. Post.archive() 호출로 archived = true 플래그 업데이트
  5. 실패 시 로그 + 재시도 (try-catch + @Retryable 고려)

rerunArchive(LocalDate date)  ← 어드민 수동 재실행용
  - 이미 아카이브된 것 제외하고 동일 로직 실행
```

---

### 8-3. `ArchiveScheduler`

**파일:** `archive/infrastructure/ArchiveScheduler.java`

```java
@Component
@RequiredArgsConstructor
public class ArchiveScheduler {
    private final ArchiveBatchService archiveBatchService;

    @Scheduled(cron = "0 0 0 * * *")
    public void run() {
        archiveBatchService.archiveDailyPosts();
    }
}
```

---

### 8-4. `ArchiveService` (조회)

```
getCalendar(Long userId, Long groupId, int year, int month) → ArchiveCalendarResponse
  - ArchivePostRepository로 해당 월의 날짜별 포스트 수 조회
  - 날짜별 대표 이모지(첫 번째 포스트 작성자 아바타) + 멤버 컬러 점 목록
  - Redis 캐싱 (key: "archive:{groupId}:{year}-{month}", TTL 1일)

getArchiveByDate(Long userId, Long groupId, LocalDate date) → List<ArchivePostResponse>
  - 해당 날짜 아카이브 포스트 목록 (멤버별 그룹핑)
```

---

## Phase 9 — WebSocket 실시간 피드

> **build.gradle에 의존성 추가 필요:**
> ```gradle
> implementation 'org.springframework.boot:spring-boot-starter-websocket'
> ```

### 9-1. `WebSocketConfig`

**파일:** `global/config/WebSocketConfig.java`

```java
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    configureMessageBroker()
        - enableSimpleBroker("/topic")      ← 구독 prefix
        - setApplicationDestinationPrefixes("/app")  ← 발행 prefix

    registerStompEndpoints()
        - addEndpoint("/ws")
        - setAllowedOriginPatterns("*")
        - withSockJS()  ← React Native는 SockJS 미지원, withSockJS() 빼는 게 맞을 수 있음
```

---

### 9-2. `FeedWebSocketService`

**파일:** `post/application/FeedWebSocketService.java`

```
broadcastNewPost(Long groupId, PostResponse post)
  - SimpMessagingTemplate.convertAndSend("/topic/groups/{groupId}/feed", post)
  - PostService.createPost() 완료 후 호출됨

PostService에서 주입해서 호출:
  feedWebSocketService.broadcastNewPost(groupId, postResponse);
```

---

### 9-3. WebSocket JWT 인증

**파일:** `global/config/WebSocketSecurityConfig.java`

```
이유: WebSocket 연결도 인증이 필요함.
     STOMP CONNECT 프레임의 Authorization 헤더에서 JWT 검증.

ChannelInterceptor 구현:
  preSend()
    - StompCommand.CONNECT 인 경우만 처리
    - accessor.getFirstNativeHeader("Authorization")에서 토큰 추출
    - JwtProvider.validateToken() 검증
    - 실패 시 연결 거부
```

---

## Phase 10 — FCM 푸시 알림 (US-07)

### 10-1. FCM 초기화

**파일:** `global/config/FcmConfig.java`

```
이유: firebase-admin은 앱 초기화가 1회만 필요.

@PostConstruct 또는 @Bean으로:
  - ClassPathResource로 firebase-adminsdk.json 로드
  - FirebaseApp.initializeApp(options)

application.yml:
  fcm:
    service-account-key: classpath:firebase-adminsdk.json
```

---

### 10-2. `FcmService`

**파일:** `notification/application/FcmService.java`

```
sendPostUploadNotification(Long groupId, Long uploaderId, PostResponse post)
  1. GroupMemberRepository.findByGroupId(groupId) 조회
  2. 업로더 본인 제외
  3. push_enabled = false인 유저 제외
  4. fcmToken이 null인 유저 제외
  5. 각 유저에게 Message 빌드 후 FirebaseMessaging.getInstance().send()
  6. 실패해도 예외 삼키고 로그만 (포스트 업로드 흐름에 영향 없어야 함)
  7. @Async로 비동기 실행 (메인 스레드 블로킹 방지)

sendCommentNotification(Long postOwnerId, Long commenterId, Long postId)
  - 포스트 오너 fcmToken 조회 후 단건 발송
  - 댓글 작성자 본인이 포스트 오너면 생략

buildMessage(String fcmToken, String title, String body, Map<String, String> data) → Message
  - Message.builder()
      .setToken(fcmToken)
      .setNotification(Notification.builder().setTitle(title).setBody(body).build())
      .putAllData(data)  ← 딥링크 정보 ("postId", "groupId")
      .build()
```

---

## Phase 11 — Redis 캐싱 최적화

> Phase 10 이후. 기능이 모두 동작한 뒤 성능 최적화 단계.

### 11-1. 피드 캐시

```
FeedService.getFeed()에 @Cacheable 추가:
  key: "feed:{groupId}:{date}:{hourBucket}"
  TTL: 1분

Post 생성/삭제 시 캐시 무효화:
  PostService.createPost() → @CacheEvict(key = "feed:{groupId}:*")
  PostService.deletePost() → 동일
```

---

### 11-2. 아카이브 달력 캐시

```
ArchiveService.getCalendar()에 @Cacheable:
  key: "archive:{groupId}:{year}-{month}"
  TTL: 1일

배치 실행 완료 후 캐시 무효화:
  ArchiveBatchService.archiveDailyPosts() 완료 후
  → CacheManager.getCache("archive").clear()
```

---

## 전체 구현 순서 요약

```
Phase 1  JWT 인프라          (JwtProvider, JwtAuthFilter, LoginUser)
   ↓
Phase 2  카카오 로그인         (KakaoAuthClient, AuthService, AuthController)
   ↓
Phase 3  프로필 설정           (UserService, UserController)
   ↓
Phase 4  그룹 도메인           (Group, GroupMember, GroupService, GroupController)
   ↓
Phase 5  포스트               (Post, R2StorageAdapter, PostService, PostController)
   ↓
Phase 6  피드                 (FeedService, FeedController, 커서 페이지네이션)
   ↓
Phase 7  반응 + 댓글           (Reaction, Comment, Service, Controller)
   ↓
Phase 8  아카이브 배치          (ArchivePost, ArchiveBatchService, @Scheduled)
   ↓
Phase 9  WebSocket 실시간      (WebSocketConfig, FeedWebSocketService)
   ↓
Phase 10 FCM 푸시 알림         (FcmConfig, FcmService)
   ↓
Phase 11 Redis 캐싱 최적화     (@Cacheable, 무효화 전략)
```

---

## 각 Phase 완료 기준 (테스트 가능 상태)

| Phase | 검증 방법 |
|-------|-----------|
| 1 | Postman으로 토큰 없이 요청 → 401, 유효 토큰 → 통과 |
| 2 | 카카오 accessToken으로 `/api/auth/kakao` 호출 → JWT 반환 |
| 3 | JWT로 `/api/users/me/profile` 호출 → 저장 확인 |
| 4 | 그룹 생성 → 초대 코드 발급 → 다른 유저로 합류 확인 |
| 5 | Presigned URL → R2 업로드 → 포스트 생성 → DB 확인 |
| 6 | 피드 조회 → hour_bucket 그룹핑 JSON 구조 확인 |
| 7 | 이모지 반응 등록/취소, 댓글 CRUD 확인 |
| 8 | 배치 수동 실행 API → archive_posts 테이블 확인 |
| 9 | WebSocket 클라이언트 연결 → 포스트 업로드 시 실시간 수신 |
| 10 | FCM 토큰 등록 → 포스트 업로드 → 실기기에서 푸시 수신 |
| 11 | 동일 피드 2회 요청 → Redis에서 캐시 히트 로그 확인 |
