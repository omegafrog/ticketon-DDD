package org.codenbug.event.application;

import java.util.List;

import org.codenbug.event.global.PresignedUrlResponse;
import org.springframework.stereotype.Service;

@Service
public class ImageUploadService {

	private final ImageStoragePort imageStoragePort;

	public ImageUploadService(ImageStoragePort imageStoragePort) {
		this.imageStoragePort = imageStoragePort;
	}

	public List<PresignedUrlResponse> generatePresignedUrls(List<String> fileNames) {
		return imageStoragePort.generateUploadUrls(fileNames);
	}
}
