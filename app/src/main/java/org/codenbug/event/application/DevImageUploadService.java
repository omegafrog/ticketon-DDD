package org.codenbug.event.application;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.codenbug.event.global.PresignedUrlResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("dev")
public class DevImageUploadService implements ImageUploadService {

    private static final String BASE_URL = "http://localhost:8080/static/events/images/";
    private static final String FILE_EXTENSION = ".webp";

    @Override
    public List<PresignedUrlResponse> generatePresignedUrls(List<String> fileNames) {
        return fileNames.stream()
            .map(this::generatePresignedUrl)
            .toList();
    }

    private PresignedUrlResponse generatePresignedUrl(String originalFileName) {
        String hashedFileName = generateHashedFileName(originalFileName);
        String presignedUrl = BASE_URL + hashedFileName + FILE_EXTENSION;
        
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
            return String.valueOf(System.currentTimeMillis()).substring(5) + 
                   Integer.toHexString(originalFileName.hashCode()).substring(0, 6);
        }
    }
}