package com.kkiri.backend.auth.infrastructure;

import com.kkiri.backend.global.exception.CustomException;
import com.kkiri.backend.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
public class KakaoAuthClient {

    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret}")
    private String clientSecret;

    private WebClient webClient;
    private WebClient tokenClient;

    @PostConstruct
    public void init() {
        webClient = WebClient.builder()
                .baseUrl(KAKAO_USER_INFO_URL)
                .build();
        tokenClient = WebClient.builder()
                .baseUrl(KAKAO_TOKEN_URL)
                .build();
    }

    /** 인가 코드 → 카카오 액세스 토큰 교환 (서버 사이드) */
    public String exchangeCodeForToken(String code, String redirectUri) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        Map<String, Object> response = tokenClient.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(params))
                .retrieve()
                .onStatus(
                        status -> !status.is2xxSuccessful(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .doOnNext(body -> log.error("[KakaoAuthClient] 토큰 교환 실패: {}", body))
                                .then(Mono.error(new CustomException(ErrorCode.KAKAO_LOGIN_FAILED)))
                )
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (response == null || !response.containsKey("access_token")) {
            throw new CustomException(ErrorCode.KAKAO_LOGIN_FAILED);
        }
        return (String) response.get("access_token");
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
