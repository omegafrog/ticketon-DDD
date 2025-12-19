package org.codenbug.event.application;

import java.util.List;

import org.codenbug.event.global.dto.response.PresignedUrlResponse;

public interface ImageUploadService {

    List<PresignedUrlResponse> generatePresignedUrls(List<String> fileNames);
}