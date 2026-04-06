# 끼리 (Kkiri) — 유저 스토리

> 친한 친구 3~4명끼리 오늘 하루를 날 것으로 공유하는 폐쇄형 소셜 앱

---

## 변경 이력

| 날짜 | 내용 |
|------|------|
| 2026-04-03 | 최초 작성 |

---

## DB 스키마 (유저 스토리 기준)

> README의 스키마에서 아래 항목이 추가/변경됩니다.

```
users
├── id (PK)
├── kakao_id
├── nickname (UNIQUE)            ← 글로벌 중복 불가
├── avatar_emoji
├── avatar_color
├── fcm_token
├── push_enabled (default: true) ← 알림 on/off
├── created_at
└── updated_at

reactions
├── id (PK)
├── post_id (FK)
├── user_id (FK)
├── emoji_type (VARCHAR)         ← 다양한 이모지 지원 (예: "❤️", "😂", "😮")
└── created_at
(UNIQUE: post_id + user_id + emoji_type)  ← 같은 이모지는 1회만

comments
├── id (PK)
├── post_id (FK)
├── user_id (FK)
├── content (최대 100자)
├── created_at
└── updated_at
```

---

## 1. 온보딩

### US-01 소셜 로그인으로 가입

**스토리**
나는 새 유저로서, 카카오 로그인으로 빠르게 가입하고 싶다

**기능**
- 카카오 OAuth2 로그인 (인가 코드 → 서버에서 카카오 토큰 교환 → JWT 발급)
- 최초 로그인 시 자동 회원가입 + JWT Access / Refresh Token 발급
- 재로그인 시 Refresh Token으로 자동 갱신
- Refresh Token은 Redis에 저장 (탈취 시 즉시 무효화 가능)

**조건**
- Access Token 만료: 1시간 / Refresh Token 만료: 7일
- 로그인 성공 → 신규 유저면 프로필 설정 화면, 기존 유저면 홈 피드

**예외**
- 소셜 로그인 실패 → 에러 토스트 + 재시도 버튼
- Refresh Token 만료 → 로그인 화면으로 이동

**API**
```
POST /api/auth/kakao     ← 카카오 인가 코드 전달, JWT 반환
POST /api/auth/refresh   ← Refresh Token 전달, 새 Access Token 반환
```

---

### US-02 프로필 생성 (닉네임 + 아바타)

**스토리**
나는 새 유저로서, 닉네임과 아바타 이모지를 설정하고 싶다

**기능**
- 닉네임 입력 (최대 10자)
- 이모지 아바타 선택 (프리셋 제공)
- 아바타 테두리 컬러 선택

**조건**
- 닉네임은 공백 불가, 특수문자 제한
- **닉네임 글로벌 중복 불가** (어떤 그룹이든 같은 닉네임 사용 불가)
- 저장 완료 → 그룹 합류/생성 화면으로 이동

**예외**
- 닉네임 미입력 시 저장 버튼 비활성화
- 중복 닉네임 → "이미 사용 중인 닉네임이에요" 안내 (실시간 중복 체크 or 저장 시)

**API**
```
GET  /api/users/me/nickname/check?nickname={value}  ← 닉네임 중복 확인
POST /api/users/me/profile                          ← 프로필 최초 저장
```

---

### US-03 그룹 생성 + 초대 코드 공유

**스토리**
나는 방장으로서, 그룹을 만들고 초대 코드를 친구들에게 공유하고 싶다

**기능**
- 그룹명 입력 후 생성
- 서버에서 6자리 랜덤 초대 코드 발급
- 초대 코드 복사 / 카카오 공유 버튼
- 멤버 강퇴 기능 (OWNER 전용)
- 오너 위임 (나가기 전 또는 강제 위임)

**조건**
- 그룹 최대 인원 6명
- 초대 코드 만료: 발급 후 7일 (만료 후 오너가 재발급 가능)
- 생성자는 자동으로 OWNER 역할 부여
- OWNER는 일반 멤버를 강퇴할 수 있음
- OWNER가 그룹을 나갈 때 → 남은 멤버 중 랜덤으로 오너 자동 위임
- OWNER가 마지막 멤버라면 → 그룹 자동 삭제
- OWNER가 나가도 채팅방(피드) 유지

**예외**
- 그룹명 미입력 시 생성 버튼 비활성화
- 강퇴 대상이 OWNER인 경우 → 400 응답 (오너는 강퇴 불가)

**API**
```
POST   /api/groups                              ← 그룹 생성
GET    /api/groups/{groupId}/invite-code        ← 초대 코드 조회
POST   /api/groups/{groupId}/invite-code/renew  ← 초대 코드 재발급 (OWNER 전용, 만료 시)
DELETE /api/groups/{groupId}/members/{userId}   ← 멤버 강퇴 (OWNER 전용)
PATCH  /api/groups/{groupId}/owner              ← 오너 위임 (body: { targetUserId })
DELETE /api/groups/{groupId}/members/me         ← 그룹 나가기
```

---

### US-04 초대 코드로 그룹 합류

**스토리**
나는 멤버로서, 초대 코드를 입력해서 그룹에 합류하고 싶다

**기능**
- 초대 코드 6자리 입력
- 서버에서 코드 유효성 + 만료 여부 + 인원 수 검증
- 합류 성공 시 홈 피드로 이동

**조건**
- 이미 합류한 그룹 코드 입력 시 → 서버에서 200 응답 + 해당 그룹 피드로 바로 이동
- 최대 인원 초과 시 합류 거절

**예외**
- 잘못된 코드 → "존재하지 않는 코드예요" 토스트
- 인원 초과 → "그룹이 가득 찼어요" 토스트
- 만료된 코드 → "만료된 코드예요. OO에게 새 코드를 요청해 보세요" 토스트

**API**
```
POST /api/groups/join  ← body: { inviteCode }
```

---

## 2. 포스트

### US-05 사진 + 한마디 올리기

**스토리**
나는 멤버로서, 지금 이 순간 사진 + 한마디를 올리고 싶다

**기능**
- 카메라 촬영 or 갤러리 선택
- 한마디 텍스트 입력 (최대 30자, 선택)
- Presigned URL 방식으로 R2에 직접 업로드 (서버 I/O 없이 처리)
- 업로드 시 현재 시각의 `hour_bucket` 자동 기록

**Presigned URL 업로드 플로우**
```
1. POST /api/posts/presigned-url  → 서버가 R2 Presigned URL 발급
2. 클라이언트 → R2 직접 PUT 업로드 (서버 경유 없음)
3. POST /api/posts               → 서버에 이미지 URL + caption 전달, 포스트 생성
```

**조건**
- 사진 필수, 텍스트 선택
- 업로드 완료 → 홈 피드 복귀 + 내 포스트 즉시 표시 (WebSocket으로 실시간 반영)
- 하루 업로드 횟수 제한 없음

**예외**
- 사진 미선택 시 업로드 버튼 비활성화
- 네트워크 오류 → "다시 시도해 주세요" 안내
- 이미지 10MB 초과 → "사진이 너무 커요 (최대 10MB)" 안내
- Presigned URL 발급 실패 → 재시도 안내 (포스트 생성 전 단계에서 실패 처리)

**API**
```
POST /api/posts/presigned-url  ← body: { fileName, contentType }, 응답: { presignedUrl, imageKey }
POST /api/posts                ← body: { imageKey, caption }, 응답: PostResponse
```

---

### US-06 포스트 삭제

**스토리**
나는 멤버로서, 내가 올린 포스트(당일 활성 포스트만)를 삭제하고 싶다

**기능**
- 내 포스트 길게 누르기 → 삭제 옵션
- 확인 다이얼로그 후 삭제
- R2 이미지 비동기 삭제 (포스트 DB 삭제 후 백그라운드 처리)

**조건**
- 본인 포스트만 삭제 가능 (서버에서 user_id 검증)
- **당일 활성 포스트만 삭제 가능** — 자정 이후 아카이브된 포스트는 삭제 불가
  - 이유: 아카이브는 그룹 구성원 모두의 공유 추억이므로 일방적 삭제를 허용하지 않음
- R2 이미지는 동기 삭제하지 않고 비동기 처리 (응답 지연 방지)

**예외**
- 타인 포스트 삭제 시도 → 403 응답
- 아카이브된 포스트 삭제 시도 → 403 응답 + "아카이브된 포스트는 삭제할 수 없어요" 안내

**API**
```
DELETE /api/posts/{postId}
```

---

### US-07 친구가 올리면 알림 받기

**스토리**
나는 멤버로서, 친구가 포스트를 올리면 푸시 알림을 받고 싶다

**기능**
- 포스트 업로드 시 같은 그룹 멤버에게 FCM 푸시 발송
- 알림 탭 시 해당 포스트로 딥링크 이동
- 앱 재설치 / 앱 실행 시 FCM 토큰 서버에 갱신

**조건**
- 알림은 본인 포스트 제외하고 발송
- users.push_enabled = false인 유저는 발송 생략
- 앱 실행 시 FCM 토큰이 변경된 경우 자동으로 서버 토큰 갱신 요청

**예외**
- FCM 토큰 없는 유저 또는 발송 실패 → 조용히 실패 (포스트 업로드 흐름에 영향 없음)
- 토큰 갱신 실패 → 다음 앱 실행 시 재시도

**API**
```
PATCH /api/users/me/fcm-token    ← body: { fcmToken }
PATCH /api/users/me/push-enabled ← body: { pushEnabled }
```

---

## 3. 피드

### US-08 오늘의 시간대별 피드 보기

**스토리**
나는 멤버로서, 오늘 친구들이 올린 것들을 시간대별로 묶어서 보고 싶다

**기능**
- 오늘 올린 포스트를 `hour_bucket` 기준 내림차순 그룹핑
- 각 시간대 헤더 (예: "14시") + 해당 시간대 포스트 카드들
- 피드 상단에 멤버 아바타 바 (오늘 올린 멤버 하이라이트)
- WebSocket(STOMP)으로 실시간 새 포스트 수신

**조건**
- 당일 00:00 ~ 현재까지 올린 포스트만 표시
- 커서 기반 페이지네이션 (20개 단위, `hour_bucket` 기준)
  - 한 시간대가 페이지 경계에서 잘리지 않도록 hour_bucket 단위로 끊음
- 포스트 없는 날 → 빈 피드 안내 메시지
- 피드 데이터 Redis 캐싱 (TTL: 1분) — 캐시 히트 시 DB 조회 생략

**예외**
- 네트워크 오류 → 이전 캐시 데이터 표시 + 재시도 버튼

**API**
```
GET /api/groups/{groupId}/feed?cursor={hourBucket}&size=20
```

---

### US-09 친구의 하루 슬라이드 보기

**스토리**
나는 멤버로서, 친구 아바타를 눌러서 그 친구의 오늘 포스트를 슬라이드로 보고 싶다

**기능**
- 피드 상단 아바타 바에서 친구 탭 → 슬라이드 뷰 오픈
- 오늘 올린 포스트를 시간순 슬라이드 재생
- 상단 진행 바, 좌우 탭으로 이전/다음 이동
- 3초 자동 넘김, 마지막 슬라이드 후 닫힘

**조건**
- 슬라이드 뷰 데이터는 US-08 피드 응답에서 필터링 (별도 API 불필요)
- 오늘 포스트 없는 친구 탭 시 → "아직 오늘 올리지 않았어요" 안내
- 슬라이드 중 스와이프 다운으로 닫기

**예외**
- 이미지 로드 실패 시 → 대체 배경(아바타 컬러) 표시

> 별도 API 없음 — GET /api/groups/{groupId}/feed 응답 데이터 재활용

---

### US-10 포스트에 이모지로 반응하기

**스토리**
나는 멤버로서, 포스트에 다양한 이모지로 반응하고 싶다

**기능**
- 포스트 롱프레스 or 반응 버튼 탭 → 이모지 선택 팝업 (예: ❤️ 😂 😮 😢 🔥)
- 이모지 선택 → 즉시 반응 등록 (같은 이모지 재탭 시 취소)
- 이모지별 반응 수 표시
- 본인 반응 이모지 하이라이트

**조건**
- 같은 이모지는 1인 1회 (UNIQUE: post_id + user_id + emoji_type)
- 다른 이모지는 동시에 여러 개 반응 가능
- 본인 포스트에도 반응 가능
- 낙관적 업데이트 (서버 응답 전 UI 먼저 반영)

**예외**
- 서버 실패 시 반응 롤백 + "반응에 실패했어요" 토스트

**API**
```
POST   /api/posts/{postId}/reactions           ← body: { emojiType }
DELETE /api/posts/{postId}/reactions/{emojiType}
GET    /api/posts/{postId}/reactions           ← 이모지별 반응 수 + 본인 반응 여부
```

---

### US-11 포스트에 댓글 달기

**스토리**
나는 멤버로서, 친구의 포스트에 댓글을 달고 싶다

**기능**
- 포스트 탭 → 댓글 입력창 + 댓글 목록
- 댓글 작성 (최대 100자)
- 내 댓글 길게 누르기 → 삭제 옵션

**조건**
- 포스트 카드에 댓글 수 표시 (미리보기 최대 2개)
- 댓글 상세는 포스트 상세 화면에서 전체 표시
- 본인 댓글만 삭제 가능
- 포스트 삭제 시 댓글도 함께 삭제 (CASCADE)
- 댓글 작성 시 포스트 오너에게 FCM 알림 발송 (본인 포스트 제외)

**예외**
- 타인 댓글 삭제 시도 → 403 응답
- 100자 초과 입력 → 전송 버튼 비활성화

**API**
```
GET    /api/posts/{postId}/comments              ← 댓글 목록 (최신순, 커서 페이지네이션)
POST   /api/posts/{postId}/comments              ← body: { content }
DELETE /api/posts/{postId}/comments/{commentId}
```

---

## 4. 아카이브

### US-12 달력으로 그날의 기록 보기

**스토리**
나는 멤버로서, 달력에서 날짜를 눌러 그날 우리 그룹이 뭐 했는지 보고 싶다

**기능**
- 월별 달력 — 포스트 있는 날짜에 대표 이모지 + 멤버 컬러 점 표시
- 날짜 탭 → 멤버별 포스트 썸네일 가로 스크롤
- 썸네일 탭 → 해당 포스트 상세

**조건**
- 자정(00:00) `@Scheduled` 배치로 `archive_posts` 테이블에 자동 저장
- 오늘 날짜는 선택 불가 (당일은 홈 피드에서만 조회)
- 포스트 없는 날 탭 시 → "이날은 아무도 올리지 않았어요"
- 아카이브 달력 데이터 Redis 캐싱 (월 단위 TTL: 1일)

**예외**
- 배치 실패 시 수동 재실행 어드민 API 별도 제공
- 배치 실행 직후(00:00~00:01) 오늘 날짜가 달력에 잠깐 노출될 수 있음 → 클라이언트에서 오늘 날짜 선택 비활성화로 차단

**API**
```
GET /api/groups/{groupId}/archive?year={yyyy}&month={MM}  ← 월별 달력 데이터
GET /api/groups/{groupId}/archive/{date}                  ← 특정 날짜 포스트 목록 (yyyy-MM-dd)
POST /api/admin/archive/run                               ← 수동 배치 재실행 (어드민 전용)
```

---

## API 전체 요약

| Method | Endpoint | 설명 | US |
|--------|----------|------|----|
| `POST` | `/api/auth/kakao` | 카카오 소셜 로그인 | US-01 |
| `POST` | `/api/auth/refresh` | Access Token 갱신 | US-01 |
| `GET` | `/api/users/me/nickname/check` | 닉네임 중복 확인 | US-02 |
| `POST` | `/api/users/me/profile` | 프로필 최초 저장 | US-02 |
| `PATCH` | `/api/users/me/fcm-token` | FCM 토큰 갱신 | US-07 |
| `PATCH` | `/api/users/me/push-enabled` | 알림 on/off | US-07 |
| `POST` | `/api/groups` | 그룹 생성 | US-03 |
| `GET` | `/api/groups/{id}/invite-code` | 초대 코드 조회 | US-03 |
| `POST` | `/api/groups/{id}/invite-code/renew` | 초대 코드 재발급 | US-03 |
| `DELETE` | `/api/groups/{id}/members/{userId}` | 멤버 강퇴 (OWNER) | US-03 |
| `PATCH` | `/api/groups/{id}/owner` | 오너 위임 | US-03 |
| `DELETE` | `/api/groups/{id}/members/me` | 그룹 나가기 | US-03 |
| `POST` | `/api/groups/join` | 초대 코드로 그룹 합류 | US-04 |
| `GET` | `/api/groups/{id}/feed` | 오늘의 시간대별 피드 | US-08 |
| `GET` | `/api/groups/{id}/archive` | 월별 아카이브 달력 | US-12 |
| `GET` | `/api/groups/{id}/archive/{date}` | 날짜별 아카이브 상세 | US-12 |
| `POST` | `/api/posts/presigned-url` | R2 Presigned URL 발급 | US-05 |
| `POST` | `/api/posts` | 포스트 생성 | US-05 |
| `DELETE` | `/api/posts/{id}` | 포스트 삭제 (당일만) | US-06 |
| `POST` | `/api/posts/{id}/reactions` | 이모지 반응 등록 | US-10 |
| `DELETE` | `/api/posts/{id}/reactions/{emojiType}` | 이모지 반응 취소 | US-10 |
| `GET` | `/api/posts/{id}/reactions` | 반응 목록 조회 | US-10 |
| `GET` | `/api/posts/{id}/comments` | 댓글 목록 조회 | US-11 |
| `POST` | `/api/posts/{id}/comments` | 댓글 작성 | US-11 |
| `DELETE` | `/api/posts/{id}/comments/{commentId}` | 댓글 삭제 | US-11 |
| `POST` | `/api/admin/archive/run` | 아카이브 배치 수동 실행 | US-12 |
