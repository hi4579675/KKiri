# trouble

---

## 2026-04-06 · R2 presigned URL PUT 400 Bad Request

**증상**: Postman으로 presigned URL에 PUT 요청 시 400 Bad Request. curl은 동일 URL로 200 성공.

**원인**: Postman이 `Cache-Control: no-cache`, `Postman-Token` 헤더를 자동으로 추가함.
AWS/R2 presigned URL은 서명 생성 시 포함된 헤더 외 추가 헤더가 있으면 서명 불일치로 거부.

**해결**: curl로 테스트. `Content-Type` 헤더 하나만 포함:
```bash
curl -X PUT "{presignedUrl}" \
  -H "Content-Type: image/jpeg" \
  --data-binary @test.jpg
```
Postman 사용 시 Headers 탭에서 자동 추가 헤더 비활성화 필요.

**배운 점**: presigned URL 테스트는 Postman보다 curl이 안전. 추가 헤더가 없음을 보장할 수 있어서.

---

## 2026-04-06 · Swagger에서 @LoginUser 파라미터가 쿼리 파라미터로 노출

**증상**: Swagger UI에서 컨트롤러 메서드의 `@LoginUser Long userId`가 `userId` 쿼리 파라미터로 표시됨.
실제로는 JWT에서 추출하는 값인데 클라이언트가 직접 입력하는 파라미터처럼 보임.

**원인**: Springdoc OpenAPI가 `@LoginUser`를 인식 못하고 일반 파라미터로 처리함.

**해결**: `@Parameter(hidden = true)` 추가:
```java
@GetMapping("/{groupId}/feed")
public ResponseEntity<FeedResponse> getFeed(
    @Parameter(hidden = true) @LoginUser Long userId,
    @PathVariable Long groupId
) { ... }
```

**배운 점**: `@LoginUser` 같은 커스텀 어노테이션은 Swagger가 인식하지 못하므로 모든 컨트롤러에 `@Parameter(hidden = true)` 필수.

---

## 2026-04-06 · Swagger Authorize 자물쇠 버튼 없음 → API 호출 시 403

**증상**: Swagger UI에 JWT 입력하는 자물쇠 버튼이 없음. 토큰 없이 호출하면 무조건 403.

**원인**: `SwaggerConfig`가 아예 없었음. Springdoc 기본 설정엔 SecurityScheme이 포함되지 않음.

**해결**: `SwaggerConfig.java` 신규 생성, Bearer SecurityScheme 등록:
```java
@Bean
public OpenAPI openAPI() {
    String jwtSchemeName = "Bearer";
    SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
    Components components = new Components()
            .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                    .name(jwtSchemeName).type(SecurityScheme.Type.HTTP)
                    .scheme("bearer").bearerFormat("JWT"));
    return new OpenAPI()
            .addSecurityItem(securityRequirement)
            .components(components);
}
```

**배운 점**: Springdoc은 SecurityScheme을 자동 생성하지 않음. JWT 인증 프로젝트라면 `SwaggerConfig` 필수.

---

## 2026-04-06 · JwtAuthenticationFilter가 throw하면 403이 나옴 (401 아님)

**증상**: 유효하지 않은 토큰으로 요청 시 401이 아닌 403 반환.
프론트 axios의 401 인터셉터(토큰 갱신 로직)가 동작하지 않음.

**원인**: Spring Security 필터 체인 순서 문제.
`ExceptionTranslationFilter`(order 10000)는 자신보다 **뒤에 오는** 필터의 예외만 catch함.
`JwtAuthenticationFilter`(order 4799)가 throw한 예외는 `ExceptionTranslationFilter`가 잡지 못함.
예외가 SecurityContext에 인증 없이 통과 → `AuthorizationFilter`에서 익명 사용자 판단 → 403.
`formLogin`, `httpBasic` 미설정 시 기본 entry point가 `Http403ForbiddenEntryPoint`이므로 401이 아닌 403 반환.

**해결**: `JwtAuthenticationFilter` 내부에서 try-catch 후 `HttpServletResponse`에 직접 401 JSON 작성:
```java
try {
    String token = extractToken(request).orElse(null);
    if (token != null) {
        if (!jwtProvider.validateToken(token)) {
            sendErrorResponse(response, ErrorCode.INVALID_TOKEN);
            return;
        }
        // ... SecurityContext 설정
    }
} catch (Exception e) {
    sendErrorResponse(response, ErrorCode.INVALID_TOKEN);
    return;
}
chain.doFilter(request, response);
```

**배운 점**: Spring Security 필터 계층에서 throw한 예외는 `@RestControllerAdvice`도, `ExceptionTranslationFilter`도 잡지 못할 수 있음.
필터에서 HTTP 오류를 내야 한다면 `HttpServletResponse`에 직접 응답 작성이 확실한 방법.

---

## 2026-04-06 · ProfileCompleteFilter 예외가 GlobalExceptionHandler에 안 잡힘

**증상**: 프로필 미완료 유저 API 호출 시 의도한 400 대신 엉뚱한 상태코드 반환.
`GlobalExceptionHandler.handleCustomException()`이 동작하지 않음.

**원인**: `@RestControllerAdvice`는 DispatcherServlet이 처리하는 **컨트롤러 계층** 예외만 잡음.
`ProfileCompleteFilter`는 Servlet Filter 계층에서 동작하므로 throw한 `CustomException`이 ControllerAdvice에 전달되지 않음.
예외가 서블릿 컨테이너까지 전파 → Spring Boot `BasicErrorController` 처리 → `@ResponseStatus` 없는 커스텀 예외라 의도치 않은 코드 반환.

**해결**: throw 대신 `HttpServletResponse`에 직접 JSON 작성 후 return:
```java
private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
    response.setStatus(errorCode.getStatus().value());
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(
        ApiResponse.onFailure(errorCode)
    ));
}
```

**배운 점**: 필터에서 예외를 throw해서 ControllerAdvice가 처리해줄 것을 기대하는 건 잘못된 가정.
Filter 계층 = 직접 응답 작성, Controller 계층 = ControllerAdvice. 계층을 명확히 구분해야 함.

---

## 2026-04-06 · 프론트에서 res.data.data === undefined (ApiResponse 래퍼 누락)

**증상**: 피드 조회, presigned URL, 포스트 생성 등 모든 API 호출 결과가 `undefined`.
Swagger에서는 정상 200 응답.

**원인**: 프론트 axios 클라이언트는 `{ success, code, message, data }` 구조를 기대하고 `res.data.data`로 언래핑.
`PostController`, `FeedController`가 `ResponseEntity.ok(rawData)` 형태로 래퍼 없이 반환하고 있었음.
Swagger는 raw 데이터를 그대로 보여주므로 단독 테스트에서는 문제가 안 보임.

**해결**: 전 엔드포인트 `ApiResponse.onSuccess()` 형식으로 통일:
```java
// Before
return ResponseEntity.ok(feedService.getFeed(...));

// After
return ApiResponse.onSuccess(SuccessCode.FEED_FETCHED, feedService.getFeed(...));
```

**배운 점**: Swagger 테스트만으로는 응답 래퍼 불일치를 발견하기 어려움. 프론트 연동 초기에 한 엔드포인트라도 실제 클라이언트로 검증 필요. 백엔드 응답 포맷은 프론트 axios 클라이언트 설계와 반드시 맞춰야 함.

---

## 2026-04-06 · 빈 record로 선언된 DTO → 빌드 실패

**증상**: `./gradlew bootRun` 시 컴파일 에러.
`PresignedUrlRequest.fileName()`, `contentType()` 메서드를 찾을 수 없다는 오류.

**원인**: `PresignedUrlRequest`, `PresignedUrlResponse`가 `public record PresignedUrlRequest {}` 형태의 빈 record였음.
파일은 생성했지만 필드 정의를 빠트린 것.

**해결**: 필드 포함한 정상 record로 수정:
```java
public record PresignedUrlRequest(
    @NotBlank String fileName,
    @NotBlank String contentType
) {}

public record PresignedUrlResponse(
    String presignedUrl,
    String imageKey,
    String imageUrl
) {}
```

**배운 점**: record는 필드가 곧 생성자이자 접근자. 빈 record를 placeholder로 두면 다른 클래스에서 참조할 때 빌드 단계에서 바로 터짐. 파일 생성 시 필드 정의까지 한 번에 완성하는 습관 필요.

---

## 2026-04-06 · Ambiguous mapping — 앱 기동 실패

**증상**: Spring Boot 시작 시
`Ambiguous mapping. Cannot map 'feedController' method... GET /api/groups/{groupId}/feed` 에러로 기동 불가.

**원인**: `GET /api/groups/{groupId}/feed`가 `GroupController`와 `FeedController` 두 곳에 동시에 선언됨.
그룹 관련 기능을 `GroupController`에 추가하는 과정에서 `FeedController`에 이미 있던 동일 경로를 중복 등록.

**해결**: `GroupController`에서 해당 매핑 제거. 피드는 `FeedController` 단독 관리.

**배운 점**: 경로가 겹치는 컨트롤러가 있으면 서버가 아예 뜨지 않으므로 새 엔드포인트 추가 전 기존 경로 확인 필수. 피드처럼 별도 도메인성이 있는 기능은 컨트롤러도 분리해 책임을 명확히.

---

## 2026-04-06 · git rebase 중 파일 유실 (PostService 등 빈 stub으로 복귀)

**증상**: git interactive rebase 후 `PostService.java`, `CreatePostRequest.java`, `PostResponse.java`가
빈 stub 상태로 돌아감. 빌드 실패.

**원인**: rebase 과정에서 commit 순서가 뒤바뀌며 이후 커밋의 변경사항이 적용되기 전 상태가 HEAD가 됨.
파일 내용을 placeholder로 두고 나중 커밋에서 채웠던 경우, rebase 순서에 따라 placeholder 상태로 유실 가능.

**해결**: `git cherry-pick`, `git stash`, `git branch -f` 조합으로 복구 후 파일 수동 복원.

**배운 점**: interactive rebase는 커밋 내용이 아닌 커밋 순서를 재조합하므로, placeholder 파일이 중간에 낀 구조에서 사용하면 위험. placeholder 없이 파일을 처음부터 완성된 상태로 커밋하는 것이 안전. rebase 전 `git stash` 또는 별도 브랜치로 백업 필수.

---

## 2026-04-06 · PostResponse 중첩 구조 → 프론트 렌더링 실패

**증상**: 피드 카드에 닉네임, 아바타가 `undefined`. `post.author.nickname` 접근 시 오류.

**원인**: 백엔드 `PostResponse`가 `author { userId, nickname, avatarEmoji, avatarColor }` 중첩 구조였으나
프론트 타입 정의와 컴포넌트는 `post.nickname`, `post.avatarEmoji` 형태의 flat 구조를 기대.

**해결**: `PostResponse`를 flat record로 변경:
```java
public record PostResponse(
    Long postId, Long userId, String nickname,
    String avatarEmoji, String avatarColor,
    String imageUrl, String caption, int hourBucket, LocalDateTime createdAt
) { ... }
```

**배운 점**: 중첩 DTO는 접근 경로가 길어지고 null 체크가 복잡해짐.
프론트와 협의 없이 중첩 구조로 설계하면 나중에 반드시 수정하게 됨. flat이 연동에 유리.

---
