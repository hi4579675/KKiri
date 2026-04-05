package com.kkiri.backend.global.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;


    /**
     * 매 요청마다 실행되는 JWT 인증 필터.
     * 처리 흐름:
     * 1. Authorization 헤더에서 토큰 추출
     * 2. 토큰이 없으면 인증 없이 통과 (permitAll 경로는 Security 설정에서 처리)
     * 3. 토큰 서명/만료 검증 (validateToken → 실패 시 CustomException throw → JwtExceptionFilter가 잡음)
     * 4. Redis 블랙리스트 확인 (로그아웃된 토큰 차단)
     * 5. 클레임에서 userId 추출 → DB 조회 없이 Authentication 객체 생성
     * 6. SecurityContextHolder에 등록 → 이후 @AuthenticationPrincipal 등으로 꺼내 쓸 수 있음
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. Authorization 헤더에서 "Bearer {token}" 호출
        String bearerToken = request.getHeader(JwtProvider.AUTHORIZATION_HEADER);
        String token = jwtProvider.resolveToken(bearerToken);

        // 2. 토큰이 없으면 인증 없이 다음 필터로
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 토큰 검증 (만료/변조)
        jwtProvider.validateToken(token);

        // 4. 블랙리스트 확인(로그아웃된 토큰)

        // 5. 클레임 추출
        Claims claims = jwtProvider.getUserInfoFromToken(token);
        Long userId = Long.parseLong(claims.getSubject());

        // principle에 userId를 넣어두면 컨트롤러에서
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());

        // 6. SecurityContext에 등록 @AuthenticationPrincipal Long userId로 꺼내씀
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        log.debug("[JwtAuthenticationFilter] 인증 완료 - userId: {}", userId);


        filterChain.doFilter(request, response);
    }
}
