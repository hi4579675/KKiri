# 끼리 (Kkiri)

> 친한 친구 3~4명끼리 오늘 하루를 날 것으로 공유하는 **폐쇄형 소셜 앱**의 백엔드 서버

<!-- 배포 후 아래 뱃지 URL을 실제 값으로 교체하세요 -->
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=flat&logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-007396?style=flat&logo=openjdk&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?style=flat&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-Cache-DC382D?style=flat&logo=redis&logoColor=white)
![Railway](https://img.shields.io/badge/Deploy-Railway-0B0D0E?style=flat&logo=railway&logoColor=white)

---

## 📌 프로젝트 소개

인스타그램은 꾸미기 압박이 있고, X는 팔로워에게 노출됩니다.  
**끼리**는 그 사이 어딘가 — 친한 친구 3~4명에게만, 필터 없이 지금 이 순간을 던지는 공간입니다.

사진 1장 + 한마디(30자)로 포스트를 올리면, 시간대별로 묶인 피드에 쌓입니다.  
자정이 되면 그날의 기록이 자동으로 아카이브되어 우리 그룹의 추억으로 남습니다.

- **완전 폐쇄** — 초대 코드로 들어온 멤버만 볼 수 있습니다
- **시간대 그룹핑** — 14:37에 올리면 "14시" 헤더로 묶입니다 (`hour_bucket`)
- **자정 아카이브** — Spring Scheduler가 매일 00:00에 자동 배치 처리합니다
- **실시간 피드** — WebSocket(STOMP)으로 친구 포스트를 즉시 수신합니다

---

## 🛠 기술 스택

| 분류 | 기술                          | 선택 이유 |
|------|-----------------------------|-----------|
| Framework | Spring Boot 3 + Java 17     | REST API, WebSocket, 배치 스케줄러를 단일 서버로 통합 |
| Auth | Spring Security + JWT       | 카카오 OAuth2 소셜 로그인 + 토큰 관리 |
| ORM | Spring Data JPA + Hibernate | 시간대 쿼리·날짜별 아카이브 조회를 타입 안전하게 구현 |
| DB | PostgreSQL 15               | `DATE_TRUNC`, 날짜 함수 지원으로 아카이브 쿼리에 적합 |
| Cache | Redis                       | 피드 캐싱, 온라인 상태 관리 |
| Realtime | WebSocket (STOMP)           | Spring 내장 지원, 별도 인프라 불필요 |
| Storage | Cloudflare R2               | S3 호환 + 무료 이그레스로 비용 절감 |
| Deploy | Railway                     | GitHub 푸시 → 자동 배포, PostgreSQL·Redis 통합 |
| Push | Firebase FCM                | iOS·Android 단일 처리 |

---

## 🏗 시스템 아키텍처

```
[React Native App]
      │
      ├── REST API ──────────────────▶ [Spring Boot]
      │                                      │
      ├── WebSocket (STOMP) ────────▶ [Spring Boot]
      │                                      │
      └── Presigned URL ──▶ [Cloudflare R2]  │
                                    ┌─────────┴─────────┐
                                    │                   │
                               [PostgreSQL]           [Redis]
                                    │
                               [Railway 배포]
                                    │
                             [FCM 발송] ──▶ [iOS / Android]
```

---

## 🗄 핵심 설계: `hour_bucket`

피드의 시간대 그룹핑을 구현하는 핵심 컬럼입니다.
 

14:37에 올린 포스트와 14:52에 올린 포스트가 "14시" 헤더 아래 함께 묶입니다.  
별도의 시간대 계산 없이 정수 인덱스만으로 O(1) 그룹핑이 가능합니다.

---

## ⚙️ 자정 아카이브 배치

```java
@Scheduled(cron = "0 0 0 * * *")  // 매일 자정
public void archiveDailyPosts() {
    LocalDate yesterday = LocalDate.now().minusDays(1);
    List<Post> posts = postRepository.findByDate(yesterday);
    archiveRepository.saveAll(toArchive(posts));
}
```

- 전날의 모든 포스트를 `archive_posts` 테이블로 이관합니다
- 실패 시 재시도 로직으로 데이터 유실을 방지합니다
- 아카이브 달력 조회는 Redis 캐싱으로 응답 속도를 확보합니다

---

## 📡 주요 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/auth/kakao` | 카카오 소셜 로그인 |
| `POST` | `/api/groups` | 그룹 생성 |
| `POST` | `/api/groups/join` | 초대 코드로 그룹 참여 |
| `GET` | `/api/groups/{id}/feed` | 오늘의 시간대별 피드 조회 |
| `POST` | `/api/posts` | 포스트 업로드 (Presigned URL 발급 포함) |
| `POST` | `/api/posts/{id}/reactions` | 이모지 반응 |
| `GET` | `/api/groups/{id}/archive/{date}` | 날짜별 아카이브 조회 |

전체 API 명세 → [`docs/api/endpoints.md`](docs/api/endpoints.md)

## 전체 DB

``` 
users
├── id (PK)
├── kakao_id
├── nickname
├── avatar_emoji
├── avatar_color
├── fcm_token          ← FCM 추가
├── created_at
└── updated_at

groups
├── id (PK)
├── name
├── invite_code (UNIQUE)
├── max_members (default: 5)
├── created_at
└── updated_at

group_members
├── id (PK)
├── user_id (FK)
├── group_id (FK)
├── role (OWNER / MEMBER)
└── joined_at

posts
├── id (PK)
├── user_id (FK)
├── group_id (FK)
├── image_url
├── caption
├── hour_bucket (0~23)   ← 피드 그룹핑용
├── heart_count
├── created_at
└── updated_at

reactions
├── id (PK)
├── post_id (FK)
├── user_id (FK)
└── created_at
(UNIQUE: post_id + user_id)

archive_posts
├── id (PK)
├── post_id (FK)
├── group_id (FK)
├── archived_date (DATE)  ← 배치 기준일
└── created_at


```
---

## 🔐 인증 플로우

```
Client                    Server                   Kakao
  │                          │                       │
  │── 카카오 인가 코드 ──────▶│                       │
  │                          │── 토큰 교환 ─────────▶│
  │                          │◀─ 카카오 사용자 정보 ──│
  │                          │
  │                          │ (신규 유저면 자동 회원가입)
  │                          │
  │◀── JWT Access / Refresh ─│
```

- Access Token: 1시간 / Refresh Token: 30일
- Refresh Token은 Redis에 저장하여 탈취 시 즉시 무효화 가능
- 애플 로그인은 동일 플로우, `identityToken` 검증만 상이

---

## 🚀 로컬 실행

### 요구 사항

- Java 17+
- Docker (PostgreSQL, Redis 컨테이너용)

### 실행

```bash
# 1. 저장소 클론
git clone https://github.com/{username}/kkiri-server.git
cd kkiri-server

# 2. 환경변수 설정
cp .env.example .env
# .env 파일에 카카오 앱 키, R2 키 등 입력

# 3. DB·Redis 컨테이너 실행
docker-compose up -d

# 4. 서버 실행
./gradlew bootRun
```

서버가 `http://localhost:8080`에서 실행됩니다.

### 환경변수 목록

```
# .env.example
KAKAO_CLIENT_ID=
KAKAO_CLIENT_SECRET=
JWT_SECRET=
R2_ACCESS_KEY=
R2_SECRET_KEY=
R2_BUCKET_NAME=
FCM_SERVER_KEY=
```

---

## 📁 프로젝트 구조

DDD(Domain-Driven Design) 기반으로 각 도메인이 독립적인 레이어 구조를 가집니다.

```
src/main/java/com/kkiri/
│
├── post/                         # 포스트 도메인
│   ├── domain/
│   │   ├── Post.java             # 엔티티 (비즈니스 규칙 포함)
│   │   ├── HourBucket.java       # 값 객체 (시간대 그룹핑)
│   │   └── PostRepository.java   # 리포지토리 인터페이스
│   ├── application/
│   │   ├── PostService.java      # 유스케이스
│   │   └── PostCommand.java      # Command DTO
│   ├── infrastructure/
│   │   ├── PostJpaRepository.java
│   │   └── R2StorageAdapter.java # 외부 스토리지 어댑터
│   └── presentation/
│       ├── PostController.java
│       └── PostResponse.java
│
├── group/                        # 그룹 도메인
│   ├── domain/
│   │   ├── Group.java
│   │   ├── InviteCode.java       # 값 객체 (6자리 초대 코드)
│   │   └── GroupRepository.java
│   ├── application/
│   ├── infrastructure/
│   └── presentation/
│
├── archive/                      # 아카이브 도메인
│   ├── domain/
│   │   ├── ArchivePost.java
│   │   └── ArchiveRepository.java
│   ├── application/
│   │   └── ArchiveBatchService.java  # 자정 배치 유스케이스
│   ├── infrastructure/
│   │   └── ArchiveScheduler.java     # @Scheduled 어댑터
│   └── presentation/
│
├── auth/                         # 인증 도메인
│   ├── domain/
│   │   ├── User.java
│   │   └── RefreshToken.java     # 값 객체
│   ├── application/
│   │   └── OAuthService.java     # 카카오·애플 OAuth2
│   ├── infrastructure/
│   │   ├── JwtProvider.java
│   │   └── KakaoAuthAdapter.java
│   └── presentation/
│
├── reaction/                     # 반응 도메인
├── notification/                 # 알림 도메인
│
└── common/                       # 공통
    ├── exception/                # 도메인 예외 계층
    ├── response/                 # API 공통 응답
    └── config/                   # Spring 설정
```

각 도메인 내 의존성 방향: `presentation → application → domain ← infrastructure`  
도메인 간 참조는 도메인 이벤트(Domain Event) 또는 `application` 레이어를 통해서만 허용합니다.

---

## 📝 개발 기록

| 문서 | 내용 |
|------|------|
| [`docs/architecture/decisions/`](docs/decisions/decisions/) | JPA 선택, JWT 전략 등 핵심 의사결정 기록 (ADR) |
| [`docs/dev-log/`](docs/dev-log/) | 개발 일지 |
| [`docs/retrospective/challenges.md`](docs/retrospective/challenges.md) | 트러블슈팅 기록 |

---

## 🗓 개발 로드맵

- [x] Spring Boot 프로젝트 셋업 + Railway 배포
- [ ] JPA 엔티티 설계 + `hour_bucket` 기반 피드 API
- [ ] Spring Security + JWT + 카카오 소셜 로그인
- [ ] 그룹 생성 / 초대 코드 API
- [ ] Cloudflare R2 Presigned URL 업로드
- [ ] 자정 아카이브 배치 (`@Scheduled`)
- [ ] WebSocket(STOMP) 실시간 피드
- [ ] FCM 푸시 알림
- [ ] 댓글 · 이모지 반응 고도화
- [ ] Redis 캐싱 최적화
- [ ] 앱스토어 배포

---

 