package com.kkiri.backend.post.infrastructure;

import com.kkiri.backend.post.application.dto.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

//            [현재 방식 — Presigned URL]
//  ① 클라이언트 → 서버: "이미지 올릴건데 URL 줘" (JSON 요청, 가벼움)
//  ② 서버 → 클라이언트: 서명된 URL 반환 (R2에 직접 PUT할 수 있는 임시 URL)
//  ③ 클라이언트 → R2: 직접 이미지 업로드 (서버 안 거침)
//  ④ 클라이언트 → 서버: "imageKey 받아, 포스트 만들어줘"
@Slf4j
@Component
@RequiredArgsConstructor // final 필드들을 파라미터로 받는 생성자 자동 생성
public class R2StorageAdapter {

    private final S3Client s3Client; // 실제 R2 API 호출용
    private final S3Presigner s3Presigner; // Presigned URL 생성 전용 클라이언트
    // S3Client와 별도 객체임 — 서명 URL 만드는 기능이 여기에만 있어서 분리돼 있음

    @Value("${r2.bucket-name}")
    private String bucketName;

    @Value("${r2.public-url}")
    private String publicUrl;

    // presigned PUT URL 생성 (클라이언트가 R2에 직접 업로드)
    // groups/{groupId}/{date}/{UUID}_{fileName} 구조
    // → 그룹별/날짜별 prefix로 아카이브 배치, 일괄 삭제 등 배치 작업 시 대상 파일 특정이 용이
    public PresignedUrlResponse generatePresignedUrl(Long groupId, String fileName, String contentType) {
        String date = LocalDate.now(ZoneOffset.UTC).toString(); // "2026-04-06"
        String imageKey = "groups/" + groupId + "/" + date + "/" + UUID.randomUUID() + "_" + sanitize(fileName);


        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(imageKey)
                .contentType(contentType)
                .build();

        // 이 버킷의 이 경로에 이 Content-Type으로 PUT 허용 조건을 담은 객체
        // 서명을 만들기 위한 설정값 컨테이너.
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(putObjectRequest)
                .build();

        // 15분 지나면 이 URL로 업로드 시도해도 R2가 거부함.
        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

        // 실제로 서명을 생성하는 시점
        String presignedUrl = presigned.url().toString();
        // 업로드 완료 후 실제로 이미지에 접근할 공개 URL.
        String imageUrl = publicUrl + "/" + imageKey;

        // 세 값을 묶어서 반환:
        return new PresignedUrlResponse(presignedUrl, imageKey, imageUrl);
    }

    // R2 오브젝트 비동기 삭제,  메서드를 별도 스레드에서 실행
    @Async
    public void deleteObject(String imageKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(imageKey)
                    .build());
            log.info("[R2] 이미지 삭제 완료: {}", imageKey);
        } catch (Exception e) {
            log.error("[R2] 이미지 삭제 실패: {}", imageKey, e);
        }
    }

    // 파일명 특수문자 제거
    private String sanitize(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
