## ADR-001. JwtAuthenticationFilter 설계 방식

### 결정
Filter를 @Bean으로 직접 생성하지 않고
@Component로 등록 후 SecurityFilterChain에 주입받는 방식 채택

### 이유
- Filter 내부에서 JwtProvider가 필요
- @Bean으로 직접 생성 시 JwtProvider 주입 불가
- @Component 방식은 Spring이 의존성 관리를 책임짐

### 구조

``` 
JwtAuthenticationFilter (@Component)
└── JwtProvider (@Component) 주입 SecurityConfig
└── JwtAuthenticationFilter 주입받아 FilterChain에 등록
```
---
## ADR-002. 전역 에러 처리 설계 방식

---

## ADR-003. R2 이미지 저장 경로 구조

### 변경 이력
| 버전 | 날짜 | 경로 구조 |
|------|------|-----------|
| v1 (초기) | 2026-04-06 | `posts/{UUID}/{fileName}` |
| v2 (현재) | 2026-04-06 | `groups/{groupId}/{date}/{UUID}_{fileName}` |

---

### v1 초기 결정 — `posts/{UUID}/{fileName}`

**이유**
- 구현이 단순함 — `groupId` 없이 파일명 + UUID만으로 경로 구성 가능
- Presigned URL 발급 시점에 groupId를 알 필요가 없어 요청 파라미터가 단순함
- UUID로 파일명 충돌 방지

**한계 (v2로 전환한 이유)**
- 경로에 그룹·날짜 정보가 없어서, R2에서 "특정 그룹의 파일 목록"을 뽑으려면 반드시 DB를 조회해야 함
- 날짜별 아카이브 배치(`Post.archived = true`) 구현 시, 배치 대상 이미지를 R2에서 prefix 기반으로 특정할 수 없음
  → DB에서 imageUrl을 전부 꺼내서 하나씩 삭제해야 하는 구조 → 배치 비용 증가
- 비멤버가 presigned URL을 발급받아 R2에 업로드하면 고아 파일이 남는 보안 구멍 존재
  (createPost에서 멤버 검증으로 포스트 생성은 막히지만, 이미 R2에 올라간 파일은 남음)

---

### v2 현재 결정 — `groups/{groupId}/{date}/{UUID}_{fileName}`

**이유**
- `groups/{groupId}/` prefix로 R2 ListObjects API 호출만으로 그룹 전체 파일 열거 가능
  → 그룹 삭제·탈퇴 시 이미지 일괄 삭제 배치 구현 비용 감소
- `{date}/` prefix로 날짜 단위 아카이브 배치 대상 파일을 DB 없이 R2에서 직접 특정 가능
  → `Post.archived` 흐름(자정 배치)과 경로 구조가 일치
- Presigned URL 발급 시점에 그룹 멤버 검증을 추가함으로써 고아 파일 생성 자체를 차단
- UUID 접두사 유지로 같은 날 동일 파일명 충돌 방지

**트레이드오프**
- `PresignedUrlRequest`에 `groupId` 추가 → 프론트엔드 요청 파라미터 변경 필요
- Presigned URL 발급 시 DB 조회(멤버 검증) 1회 추가

### 영향 범위 (v1 → v2)
- `PresignedUrlRequest` — `groupId` 필드 추가
- `R2StorageAdapter.generatePresignedUrl()` — 시그니처에 `groupId` 추가, 경로 조립 로직 변경
- `PostService.generatePresignedUrl()` — 멤버 검증 로직 추가, 어댑터 호출 인자 변경
- 프론트엔드 — presigned URL 요청 body에 `groupId` 추가 필요

---

## ADR-004. 피드 hourBucket 그룹핑 시 LinkedHashMap 사용

### 결정
`Collectors.groupingBy()` 의 Map 구현체로 `LinkedHashMap` 명시

```java
posts.stream()
    .collect(Collectors.groupingBy(
            Post::getHourBucket,
            LinkedHashMap::new,       // ← 이 부분
            Collectors.mapping(PostResponse::from, Collectors.toList())
    ));
```

### 이유

**기본 `groupingBy`는 `HashMap`을 사용 → 삽입 순서 보장 안 됨**
- `PostRepository.findFeed()`는 `ORDER BY hourBucket DESC`로 정렬된 결과를 반환
- 이 결과를 HashMap에 담으면 Map 내부의 버킷 순서가 달라질 수 있음
- 이후 `entrySet().stream()`으로 `BucketResponse` 리스트를 만들 때 순서가 14→12→9가 아닌 임의 순서로 나올 수 있음

**`LinkedHashMap`은 삽입 순서 보존**
- 포스트 리스트가 `hourBucket` 내림차순으로 삽입되므로, Map도 14→12→9 순서로 구성됨
- 최종 `BucketResponse` 리스트도 최신 시간대 → 과거 시간대 순서 보장

### 검토한 대안
- **`TreeMap`**: Key(hourBucket) 기준 자동 정렬 → `DESC` 원하면 `Comparator.reverseOrder()` 추가 필요, 코드 복잡도 증가
- **`sorted()` 후 `groupingBy`**: 스트림 정렬을 한 번 더 거치는 방식 → 이미 DB에서 정렬된 결과를 재정렬하는 중복 비용

### 트레이드오프
- `LinkedHashMap`은 삽입 순서 유지를 위해 `HashMap`보다 메모리를 약간 더 씀
- 그룹 최대 인원이 6명이고 하루치 포스트 수가 제한적이므로 실질적 영향 없음
