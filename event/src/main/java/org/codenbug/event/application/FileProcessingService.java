package org.codenbug.event.application;

import java.io.IOException;

import org.springframework.stereotype.Service;

@Service
public class FileProcessingService {

	private final ImageStoragePort imageStoragePort;

	public FileProcessingService(ImageStoragePort imageStoragePort) {
		this.imageStoragePort = imageStoragePort;
	}

    public void processAndSaveFile(byte[] fileData, String fileName) throws IOException {
		imageStoragePort.store(fileData, fileName);
	}

    public String extractFileNameFromUrl(String url) {
        // URL에서 파일명 추출: http://localhost:8080/static/events/images/{filename}.webp
        if (url.contains("/static/events/images/")) {
            return url.substring(url.lastIndexOf("/") + 1);
        }
        throw new IllegalArgumentException("유효하지 않은 URL 형식입니다.");
    }
}
