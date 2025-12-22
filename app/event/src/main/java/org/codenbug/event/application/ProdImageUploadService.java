package org.codenbug.event.application;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.codenbug.event.application.dto.response.PresignedUrlResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
public class ProdImageUploadService implements ImageUploadService {

    // TODO: AWS S3 설정을 위한 필드들 (향후 구현)
    // private final AmazonS3 s3Client;
    // private final String bucketName;
    // private final String region;

    @Override
    public List<PresignedUrlResponse> generatePresignedUrls(List<String> fileNames) {
        return fileNames.stream()
            .map(this::generateS3PresignedUrl)
            .toList();
    }

    private PresignedUrlResponse generateS3PresignedUrl(String originalFileName) {
        String hashedFileName = generateHashedFileName(originalFileName);
        String s3Key = "events/images/" + hashedFileName + ".webp";

        // TODO: AWS S3 presigned URL 생성 로직 구현
        // URL presignedUrl = s3Client.generatePresignedUrl(bucketName, s3Key, expiration);

        // 임시로 S3 URL 형태로 반환 (실제 구현 시 위의 코드로 교체)
        String tempS3Url = "https://your-bucket.s3.amazonaws.com/" + s3Key;

        return new PresignedUrlResponse(originalFileName, tempS3Url);
    }

    private String generateHashedFileName(String originalFileName) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((originalFileName + System.currentTimeMillis()).getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            // 적당한 길이로 자르기 (16자리)
            return hexString.toString().substring(0, 16);

        } catch (NoSuchAlgorithmException e) {
            // 해싱 실패 시 현재 시간 기반 랜덤 문자열 사용
            return String.valueOf(System.currentTimeMillis()).substring(5) +
                Integer.toHexString(originalFileName.hashCode()).substring(0, 6);
        }
    }
}