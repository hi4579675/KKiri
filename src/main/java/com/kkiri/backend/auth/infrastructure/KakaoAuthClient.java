package com.kkiri.backend.auth.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kkiri.backend.global.exception.CustomException;
import com.kkiri.backend.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class KakaoAuthClient {

    // 카카오 유저 정보 조회 API 주소
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    // 다른 서버(카카오)와 통신하기 위한 도구
    private WebClient webClient;

    // 서버가 켜질 때 딱 한 번만 카카오 주소로 통신할 준비를 미리 해두는 것
    @PostConstruct
    public void init() {
        webClient = WebClient.builder()
                .baseUrl(KAKAO_USER_INFO_URL)
                .build();
    }

    /**
     * 카카오 accessToken으로 유저 정보를 조회합니다.
     * 앱이 카카오에서 받은 토큰을 우리 서버로 전달 → 서버가 카카오 API를 직접 호출.
     *
     * @param accessToken 카카오 로그인 후 발급된 accessToken
     * @return kakaoId, nickname이 담긴 KakaoUserInfo
     */
    public KakaoUserInfo getUserInfo(String accessToken) {
        KakaoApiResponse response = webClient.get()
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .onStatus(
                        status -> !status.is2xxSuccessful(),
                        // 카카오 API 호출 실패 시 (토큰 만료, 잘못된 토큰 등)
                        clientResponse -> {
                            log.error("[KakaoAuthClient] 카카오 API 호출 실패 - status: {}", clientResponse.statusCode());
                            throw new CustomException(ErrorCode.KAKAO_LOGIN_FAILED);
                        }
                )
                .bodyToMono(KakaoApiResponse.class)
                .block(); // 동기 처리

        if (response == null) {
            throw new CustomException(ErrorCode.KAKAO_LOGIN_FAILED);
        }

        return new KakaoUserInfo(
                String.valueOf(response.id), // 카카오 id는 Long → String으로 변환해 저장
                response.kakaoAccount != null ? response.kakaoAccount.profile.nickname : null
        );
    }

    // ── 카카오 API 응답 파싱용 내부 클래스 ──────────────────────────────

    // 카카오 API 응답 최상위 구조
    private static class KakaoApiResponse {
        public Long id;

        @JsonProperty("kakao_account")
        public KakaoAccount kakaoAccount;
    }

    private static class KakaoAccount {
        public Profile profile;
    }

    private static class Profile {
        public String nickname;
    }

    // ── 외부에 노출하는 결과 record ────────────────────────────────────

    /**
     * 카카오 API에서 파싱한 유저 정보.
     * KakaoAuthClient 밖에서 사용되는 유일한 데이터 구조.
     */
    public record KakaoUserInfo(String kakaoId, String nickname) {}
}
