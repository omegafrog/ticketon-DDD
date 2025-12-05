package org.codenbug.app.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StaticController {

    @GetMapping("/static/events/images/{fileName}")
    public ResponseEntity<Resource> getEventImage(@PathVariable String fileName) {
        try {
            System.out.println("=== File request received ===");
            System.out.println("Requested fileName: " + fileName);
            System.out.println("Current working directory: " + System.getProperty("user.dir"));

            // 프로젝트 루트의 static/events/images 폴더에서 파일 찾기
            Path filePath = Paths.get("static", "events", "images", fileName);
            Path absolutePath = filePath.toAbsolutePath();

            System.out.println("Looking for file at: " + absolutePath);
            System.out.println("File exists: " + Files.exists(filePath));

            if (!Files.exists(filePath)) {
                System.out.println("File not found: " + absolutePath);

                // 다른 경로들도 시도해보기
                Path[] possiblePaths = {
                        Paths.get("/mnt/c/Users/jiwoo/workspace/ticketon-DDD/static/events/images/"
                                + fileName),
                        Paths.get("../static/events/images/" + fileName),
                        Paths.get("../../static/events/images/" + fileName)};

                for (Path path : possiblePaths) {
                    System.out.println("Trying path: " + path.toAbsolutePath() + " exists: "
                            + Files.exists(path));
                    if (Files.exists(path)) {
                        filePath = path;
                        break;
                    }
                }

                if (!Files.exists(filePath)) {
                    return ResponseEntity.notFound().build();
                }
            }

            Resource resource = new FileSystemResource(filePath);

            System.out.println("Resource exists: " + resource.exists());
            System.out.println("Resource readable: " + resource.isReadable());
            System.out.println("Resource file: " + resource.getFile().getAbsolutePath());
            System.out.println("File size: " + resource.getFile().length() + " bytes");

            // Content-Type 설정
            String contentType = determineContentType(fileName);

            System.out.println("Content-Type: " + contentType);
            System.out.println("=== Serving file ===");

            return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000").body(resource);

        } catch (Exception e) {
            System.out.println("Error serving file: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    private String determineContentType(String fileName) {
        if (fileName.endsWith(".webp")) {
            return "image/webp";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG_VALUE;
        } else if (fileName.endsWith(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        } else if (fileName.endsWith(".gif")) {
            return MediaType.IMAGE_GIF_VALUE;
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
