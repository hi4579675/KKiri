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
