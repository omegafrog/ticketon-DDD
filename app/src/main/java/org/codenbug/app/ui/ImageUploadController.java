package org.codenbug.app.ui;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.codenbug.common.Role;
import org.codenbug.common.RsData;
import org.codenbug.event.global.dto.request.FileUploadRequest;
import org.codenbug.event.global.dto.response.PresignedUrlResponse;
import org.codenbug.securityaop.aop.AuthNeeded;
import org.codenbug.securityaop.aop.RoleRequired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class ImageUploadController {

    private static final String BASE_URL = "http://localhost:8080/static/events/images/";
    private static final String FILE_EXTENSION = ".webp";

    @AuthNeeded
    @RoleRequired(Role.MANAGER)
    @PostMapping("/image/url")
    public ResponseEntity<RsData<List<PresignedUrlResponse>>> generateImageUploadUrls(
        @RequestBody FileUploadRequest request) {
        List<PresignedUrlResponse> presignedUrls =
            request.getFileNames().stream().map(this::generatePresignedUrl).toList();

        return ResponseEntity.ok(new RsData<>("200", "presigned URL 생성 성공", presignedUrls));
    }

    private PresignedUrlResponse generatePresignedUrl(String originalFileName) {
        String hashedFileName = generateHashedFileName(originalFileName);
        String presignedUrl = BASE_URL + hashedFileName + FILE_EXTENSION;

        System.out.println(
            "Generated presigned URL: " + presignedUrl + " for file: " + originalFileName);

        return new PresignedUrlResponse(originalFileName, presignedUrl);
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
            return String.valueOf(System.currentTimeMillis()).substring(5)
                + Integer.toHexString(originalFileName.hashCode()).substring(0, 6);
        }
    }
}
