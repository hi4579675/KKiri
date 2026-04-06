package com.kkiri.backend.global.security;

import com.kkiri.backend.auth.domain.User;
import com.kkiri.backend.auth.infrastructure.UserRepository;
import com.kkiri.backend.global.exception.CustomException;
import com.kkiri.backend.global.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class ProfileCompleteFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증되지 않은 요청은 통과
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 나머지는 userId로 user 조회
        Long userId = (Long) authentication.getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!user.isProfileCompleted()) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"success\":false,\"code\":\"PROFILE_NOT_COMPLETED\",\"message\":\"프로필을 먼저 설정해주세요.\",\"data\":null}");
            return;
        }
        filterChain.doFilter(request, response);
    }
    // 프로필 미완료 유저도 접근해야 하는 경로는 필터 skip
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/")           // 로그인/재발급
                || path.equals("/api/users/me/profile")        // 프로필 설정 본인
                || path.startsWith("/api/users/me/nickname")   // 닉네임 중복 확인
                || path.equals("/api/v1/health")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }
}
