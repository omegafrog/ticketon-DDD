package org.codenbug.app.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.codenbug.common.Role;
import org.codenbug.common.RsData;
import org.codenbug.securityaop.aop.AuthNeeded;
import org.codenbug.securityaop.aop.RoleRequired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FileUploadController {

    @PutMapping("/static/events/images/{fileName}")
    public ResponseEntity<RsData<String>> uploadFile(
        @PathVariable String fileName,
        @RequestBody byte[] fileData
    ) {
        try {
            System.out.println("Received file upload request for: " + fileName);
            System.out.println("File size: " + fileData.length + " bytes");
            
            // 파일명 검증
            if (!fileName.endsWith(".webp")) {
                return ResponseEntity.badRequest()
                    .body(new RsData<>("400", "webp 파일만 업로드 가능합니다.", null));
            }

            // 파일 크기 제한 (예: 10MB)
            if (fileData.length > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                    .body(new RsData<>("400", "파일 크기가 10MB를 초과합니다.", null));
            }

            // 파일 처리 및 저장
            processAndSaveFile(fileData, fileName);

            System.out.println("File uploaded successfully: " + fileName);
            return ResponseEntity.ok(new RsData<>("200", "파일 업로드 성공", fileName));

        } catch (IllegalArgumentException e) {
            System.out.println("Upload failed: " + e.getMessage());
            return ResponseEntity.badRequest()
                .body(new RsData<>("400", e.getMessage(), null));
        } catch (IOException e) {
            System.out.println("Upload failed: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body(new RsData<>("500", "파일 저장 중 오류가 발생했습니다.", null));
        }
    }

    private void processAndSaveFile(byte[] fileData, String fileName) throws IOException {
        // 1. 이미지를 BufferedImage로 변환
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(fileData));
        
        if (originalImage == null) {
            throw new IllegalArgumentException("유효하지 않은 이미지 파일입니다.");
        }

        // 2. 이미지 리사이징 (옵션)
        BufferedImage processedImage = resizeImageIfNeeded(originalImage);
        
        // 3. static/events/images 디렉토리 생성
        Path eventsImagesDir = Paths.get("static", "events", "images");
        Files.createDirectories(eventsImagesDir);
        
        System.out.println("Saving file to: " + eventsImagesDir.resolve(fileName).toAbsolutePath());
        
        // 4. webp 파일로 저장
        Path outputPath = eventsImagesDir.resolve(fileName);
        saveAsWebp(processedImage, outputPath.toFile());
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
        // Java에서 WebP 지원이 제한적이므로, 일단 고품질 JPEG로 저장
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
}