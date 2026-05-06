package org.codenbug.event.ui;

import java.util.List;

import org.codenbug.common.Role;
import org.codenbug.common.RsData;
import org.codenbug.event.application.ImageUploadService;
import org.codenbug.event.global.FileUploadRequest;
import org.codenbug.event.global.PresignedUrlResponse;
import org.codenbug.securityaop.aop.AuthNeeded;
import org.codenbug.securityaop.aop.RoleRequired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/events")
public class ImageUploadController {

    private final ImageUploadService imageUploadService;

    public ImageUploadController(ImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    @AuthNeeded
    @RoleRequired(Role.MANAGER)
    @PostMapping("/image/url")
    public ResponseEntity<RsData<List<PresignedUrlResponse>>> generateImageUploadUrls(
            @Valid @RequestBody FileUploadRequest request) {
        List<PresignedUrlResponse> presignedUrls =
                imageUploadService.generatePresignedUrls(request.getFileNames());

        return ResponseEntity.ok(new RsData<>("200", "presigned URL 생성 성공", presignedUrls));
    }
}
