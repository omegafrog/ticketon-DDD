package org.codenbug.event.application;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

@Service
public class FileProcessingService {

    public void processAndSaveFile(byte[] fileData, String fileName) throws IOException {
        // 1. 이미지를 BufferedImage로 변환
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(fileData));
        
        if (originalImage == null) {
            throw new IllegalArgumentException("유효하지 않은 이미지 파일입니다.");
        }

        // 2. 이미지 리사이징 (옵션)
        BufferedImage processedImage = resizeImageIfNeeded(originalImage);
        
        // 3. 프로젝트 루트 디렉토리 찾기 및 static/events/images 디렉토리 생성
        Path projectRoot = findProjectRoot();
        Path eventsImagesDir = projectRoot.resolve("static").resolve("events").resolve("images");
        Files.createDirectories(eventsImagesDir);
        
        // 4. webp 파일로 저장
        Path outputPath = eventsImagesDir.resolve(fileName);
        saveAsWebp(processedImage, outputPath.toFile());
    }

    private Path findProjectRoot() throws IOException {
        // 현재 실행 위치에서 시작하여 상위 디렉토리로 이동하면서 프로젝트 루트 찾기
        Path currentPath = Paths.get("").toAbsolutePath();
        
        while (currentPath != null) {
            // 프로젝트 루트 표시 파일들 확인
            if (Files.exists(currentPath.resolve("build.gradle")) ||
                Files.exists(currentPath.resolve("settings.gradle")) ||
                Files.exists(currentPath.resolve("gradlew")) ||
                Files.exists(currentPath.resolve("pom.xml"))) {
                return currentPath;
            }
            currentPath = currentPath.getParent();
        }
        
        // 프로젝트 루트를 찾지 못한 경우, 현재 작업 디렉토리 사용
        return Paths.get("").toAbsolutePath();
    }

    private BufferedImage resizeImageIfNeeded(BufferedImage originalImage) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        // 최대 크기 제한 (예: 1920x1080)
        int maxWidth = 1920;
        int maxHeight = 1080;
        
        if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
            return originalImage;
        }
        
        // 비율을 유지하면서 리사이징
        double ratio = Math.min((double) maxWidth / originalWidth, (double) maxHeight / originalHeight);
        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);
        
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resizedImage.createGraphics();
        graphics.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        graphics.dispose();
        
        return resizedImage;
    }

    private void saveAsWebp(BufferedImage image, File outputFile) throws IOException {
        // Java에서 WebP 지원이 제한적이므로, 일단 PNG로 저장
        // 실제 운영 환경에서는 WebP 라이브러리 (예: webp-imageio) 사용 필요
        
        // WebP 확장자를 가진 파일명이지만 실제로는 고품질 JPEG로 저장
        if (outputFile.getName().endsWith(".webp")) {
            // JPEG로 저장 (높은 품질)
            String jpegPath = outputFile.getAbsolutePath().replace(".webp", ".jpg");
            File jpegFile = new File(jpegPath);
            ImageIO.write(image, "jpg", jpegFile);
            
            // 원래 webp 이름으로 파일명 변경
            jpegFile.renameTo(outputFile);
        } else {
            ImageIO.write(image, "jpg", outputFile);
        }
    }

    public String extractFileNameFromUrl(String url) {
        // URL에서 파일명 추출: http://localhost:8080/static/events/images/{filename}.webp
        if (url.contains("/static/events/images/")) {
            return url.substring(url.lastIndexOf("/") + 1);
        }
        throw new IllegalArgumentException("유효하지 않은 URL 형식입니다.");
    }
}