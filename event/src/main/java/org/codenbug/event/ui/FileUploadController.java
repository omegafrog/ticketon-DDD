package org.codenbug.event.ui;

import java.io.IOException;

import org.codenbug.common.RsData;
import org.codenbug.event.application.FileProcessingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FileUploadController {

    private final FileProcessingService fileProcessingService;

    public FileUploadController(FileProcessingService fileProcessingService) {
        this.fileProcessingService = fileProcessingService;
    }

    @PutMapping("/static/events/images/{fileName}")
    public ResponseEntity<RsData<String>> uploadFile(@PathVariable String fileName,
            @RequestBody byte[] fileData) {
        try {
            fileProcessingService.processAndSaveFile(fileData, fileName);
            return ResponseEntity.ok(new RsData<>("200", "파일 업로드 성공", fileName));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new RsData<>("400", e.getMessage(), null));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(new RsData<>("500", "파일 저장 중 오류가 발생했습니다.", null));
        }
    }
}
