# testing-log

> 단순 기능 동작 확인이 아닌, 실제 버그 발견·예방·설계 결정과 연결된 테스트만 기록합니다.

---

## 2026-04-04 — 필터 레이어 버그 회귀 방지

### ① JwtAuthenticationFilter: 토큰 오류 시 403 → 401

**배경**
`JwtAuthenticationFilter`에서 만료/변조 토큰을 `throw`로 전파하면
Spring Security 필터 체인 순서 상 `ExceptionTranslationFilter`(order 10000)가
`JwtAuthenticationFilter`(order 4799)보다 뒤에 있어 예외를 잡지 못한다.
결국 인증 없는 익명 상태로 `AuthorizationFilter`까지 내려가고,
`formLogin/httpBasic` 미설정 시 기본 entry point인 `Http403ForbiddenEntryPoint`가
401 대신 **403**을 반환한다. 프론트의 axios 401 리프레시 인터셉터가 동작하지 않게 됨.

**수정 방향**
`validateToken()` 호출을 try-catch로 감싸고,
예외 발생 시 `HttpServletResponse`에 직접 401 JSON 응답 후 필터 종료.

**테스트 (`JwtAuthenticationFilterTest`)**
```java
// 수정 전 동작이라면: chain.doFilter()가 호출되고 downstream에서 403이 됨
// 수정 후 동작: chain.doFilter() 호출 없이 response에 직접 401 작성
assertThat(response.getStatus()).isEqualTo(401);
assertThat(chain.getRequest()).isNull(); // 예외 전파 없음의 증거
```

**핵심 인사이트**
필터에서 throw한 예외는 자신보다 앞에 위치한 `ExceptionTranslationFilter`가 잡지 못한다.
HTTP 응답을 직접 제어해야 하는 필터는 예외를 전파하지 않고 `HttpServletResponse`에 작성해야 한다.

---

### ② ProfileCompleteFilter: throw → ControllerAdvice 미적용 문제

**배경**
`ProfileCompleteFilter`에서 `throw new CustomException(ErrorCode.PROFILE_NOT_COMPLETED)`를
던지면 `@RestControllerAdvice`의 `GlobalExceptionHandler`가 잡지 못한다.
`@RestControllerAdvice`는 DispatcherServlet이 처리하는 컨트롤러 계층만 커버하고,
Servlet Filter 계층은 커버 범위 밖이기 때문.
예외가 서블릿 컨테이너까지 전파되고 `BasicErrorController`가 처리해
`CustomException`에 `@ResponseStatus`가 없으므로 의도치 않은 HTTP 상태로 응답됨.

**수정 방향**
throw 대신 `HttpServletResponse`에 직접 403 JSON 작성 후 return.

**테스트 (`ProfileCompleteFilterTest`)**
```java
// 직접 응답이 작성됐는지 확인
assertThat(response.getStatus()).isEqualTo(403);
assertThat(response.getContentAsString()).contains("PROFILE_NOT_COMPLETED");
assertThat(response.getContentAsString()).contains("\"success\":false");
// ControllerAdvice 없이도 ApiResponse 포맷이 유지됨
assertThat(chain.getRequest()).isNull(); // 예외 전파 없음
```

**핵심 인사이트**
`@RestControllerAdvice`는 Filter 계층의 예외를 잡지 못한다.
Filter에서는 예외를 throw하는 대신 `HttpServletResponse`에 직접 응답을 작성해야 한다.

---

## 2026-04-05 — GroupService 비즈니스 규칙 검증

### ③ 방장 탈퇴 시 그룹 자동 처리

**배경**
방장이 탈퇴할 때 두 가지 분기가 있다:
- 남은 멤버가 있으면 → 첫 번째 멤버를 방장으로 승격
- 혼자였으면 → 그룹 자체 삭제

운영 초기 이 분기를 놓치면 오너 없는 그룹이 생기거나 빈 그룹이 DB에 잔류함.

**테스트 (`GroupServiceTest`)**
```java
// 케이스 1: 남은 멤버 → 승격
given(groupMemberRepository.findByGroupId(10L)).willReturn(List.of(regularMember));
groupService.leaveGroup(1L, 10L);
assertThat(regularMember.isOwner()).isTrue(); // 승격 확인

// 케이스 2: 혼자 → 그룹 삭제
given(groupMemberRepository.findByGroupId(10L)).willReturn(List.of());
groupService.leaveGroup(1L, 10L);
then(groupRepository).should().deleteById(10L); // 삭제 확인
```

---

### ④ 방장 위임 후 역할 교체 원자성 검증

**배경**
방장 위임 시 기존 방장을 MEMBER로 낮추고 대상을 OWNER로 올리는 두 단계가 있다.
한 쪽만 적용되면 방장이 두 명이거나 없는 상태가 된다.

**테스트 (`GroupServiceTest`)**
```java
groupService.transferOwner(1L, 10L, 2L);
assertThat(ownerMember.isOwner()).isFalse(); // 기존 방장 → MEMBER
assertThat(regularMember.isOwner()).isTrue(); // 새 방장 → OWNER
```

---

## 2026-04-04 — Refresh Token Rotation 보안 검증

### ⑤ 토큰 탈취 시나리오: Redis 토큰 불일치

**배경**
Refresh Token Rotation 방식에서 공격자가 이전 Refresh Token을 탈취해 재발급 시도할 경우
Redis에 저장된 최신 토큰과 불일치하므로 차단해야 한다.

**테스트 (`AuthServiceTest`)**
```java
// 공격자의 구 토큰 vs Redis의 최신 토큰
given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.of("legit-token"));

assertThatThrownBy(() -> authService.refresh("attacker-old-token"))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.REFRESH_TOKEN_INVALID);
```

---

## 2026-04-07 — ReactionService Idempotency 검증

### ⑥ 중복 반응 추가 무시 (네트워크 재시도 안전성)

**배경**
프론트에서 네트워크 오류로 반응 추가 요청이 중복 전송될 수 있다.
같은 이모지를 두 번 추가해도 한 번만 저장돼야 하고 오류가 나면 안 된다.

**테스트 (`ReactionServiceTest`)**
```java
// 이미 존재하는 반응 → save() 호출 없이 리턴
given(reactionRepository.findByPostIdAndUserIdAndEmojiType(100L, 1L, "❤️"))
        .willReturn(Optional.of(existing));

reactionService.addReaction(1L, 100L, "❤️");

then(reactionRepository).should(never()).save(any()); // 중복 저장 없음
```