# KKiri 끼리

> 친한 친구 3~4명끼리 오늘 하루를 날 것으로 공유하는 폐쇄형 소셜 앱

## 소개

인스타그램처럼 꾸미지 않아도 됩니다.  
사진 한 장, 한마디면 충분합니다.  
초대받은 친구들만 볼 수 있고, 하루가 끝나면 우리만의 아카이브로 남습니다.

## 기술 스택

**Backend**
- Java 17 / Spring Boot 3.x
- Spring Security + JWT
- Spring Data JPA
- Spring Scheduler
- PostgreSQL / Redis

**Frontend**
- Expo + React Native
- Zustand
- React Query + Axios

**Infra**
- Railway
- Cloudflare R2
- Firebase FCM

## 주요 기능

- 카카오 / 애플 소셜 로그인
- 6자리 초대 코드로 그룹 생성 및 참여
- 사진 또는 텍스트 포스트 업로드
- 시간대별 피드 그룹핑 (hour_bucket)
- 친구 하루 슬라이드 뷰
- 자정 자동 아카이브
- 아카이브 달력 조회
- 이모지 반응

## 아키텍처

DDD 기반 모놀리식 구조
```
com.kkiri
├── user
├── group
├── post
├── archive
└── reaction
```

## 개발 현황
- [ ] Spring Boot 프로젝트 셋업
- [ ] DB 스키마 생성 + JPA 엔티티
- [ ] JWT 인증 + 카카오 소셜 로그인
- [ ] 그룹 생성 / 초대 코드 API
- [ ] 포스트 업로드 (Cloudflare R2)
- [ ] 시간대 그룹핑 피드 API
- [ ] 자정 아카이브 배치
- [ ] React Native 화면 연결
