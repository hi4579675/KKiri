package com.kkiri.backend.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class FcmConfig {

    // FIREBASE_CREDENTIALS 환경변수에 서비스 계정 JSON 전체를 문자열로 저장
    @Value("${firebase.credentials}")
    private String firebaseCredentials;

    @PostConstruct
    public void init() {
        if (firebaseCredentials == null || firebaseCredentials.isBlank()
                || firebaseCredentials.startsWith("your-")) {
            log.warn("[FCM] FIREBASE_CREDENTIALS 미설정 — FCM 비활성화");
            return;
        }
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                GoogleCredentials credentials = GoogleCredentials.fromStream(
                        new ByteArrayInputStream(
                                firebaseCredentials.getBytes(StandardCharsets.UTF_8)));

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("[FCM] Firebase 초기화 완료");
            }
        } catch (Exception e) {
            log.warn("[FCM] Firebase 초기화 실패 — FCM 비활성화: {}", e.getMessage());
        }
    }
}
