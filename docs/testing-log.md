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