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
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class R2StorageAdapter {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${r2.bucket-name}")
    private String bucketName;

    @Value("${r2.public-url}")
    private String publicUrl;

    // presigned PUT URL 생성 (클라이언트가 R2에 직접 업로드)
    public PresignedUrlResponse generatePresignedUrl(String fileName, String contentType) {
        String imageKey = "posts/" + UUID.randomUUID() + "/" + sanitize(fileName);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(imageKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

        String presignedUrl = presigned.url().toString();
        String imageUrl = publicUrl + "/" + imageKey;

        return new PresignedUrlResponse(presignedUrl, imageKey, imageUrl);
    }

    // R2 오브젝트 비동기 삭제
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
