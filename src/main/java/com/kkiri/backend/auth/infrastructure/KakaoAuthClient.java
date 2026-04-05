package com.kkiri.backend.auth.infrastructure;

import com.kkiri.backend.global.exception.CustomException;
import com.kkiri.backend.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
public class KakaoAuthClient {

    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private WebClient webClient;

    @PostConstruct
    public void init() {
        webClient = WebClient.builder()
                .baseUrl(KAKAO_USER_INFO_URL)
                .build();
    }

    public KakaoUserInfo getUserInfo(String accessToken) {
        // 임시 디버그: 카카오 응답 원문 확인
        String rawResponse = webClient.get()
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .onStatus(
                        status -> !status.is2xxSuccessful(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .doOnNext(body -> log.error("[KakaoAuthClient] 카카오 에러 응답: {}", body))
                                .then(Mono.error(new CustomException(ErrorCode.KAKAO_LOGIN_FAILED)))
                )
                .bodyToMono(String.class)
                .doOnNext(body -> log.info("[KakaoAuthClient] 카카오 성공 응답: {}", body))
                .block();

        if (rawResponse == null) {
            log.error("[KakaoAuthClient] 카카오 응답이 null");
            throw new CustomException(ErrorCode.KAKAO_LOGIN_FAILED);
        }

        // 성공 응답 파싱
        Map<String, Object> response = webClient.get()
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        String kakaoId = String.valueOf(response.get("id"));

        String nickname = null;
        Map<String, Object> kakaoAccount = (Map<String, Object>) response.get("kakao_account");
        if (kakaoAccount != null) {
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            if (profile != null) {
                nickname = (String) profile.get("nickname");
            }
        }

        return new KakaoUserInfo(kakaoId, nickname);
    }

    public record KakaoUserInfo(String kakaoId, String nickname) {}
}
