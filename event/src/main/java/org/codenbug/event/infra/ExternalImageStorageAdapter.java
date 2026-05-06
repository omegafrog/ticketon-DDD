package org.codenbug.event.infra;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.codenbug.event.application.ImageStoragePort;
import org.codenbug.event.global.PresignedUrlResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!dev")
public class ExternalImageStorageAdapter implements ImageStoragePort {

	private static final String EXTERNAL_BASE_URL = "https://ticketon-assets.example.com/events/images/";

	@Override
	public List<PresignedUrlResponse> generateUploadUrls(List<String> fileNames) {
		return fileNames.stream()
			.map(fileName -> new PresignedUrlResponse(fileName, publicPath(generateStoredFileName(fileName))))
			.toList();
	}

	@Override
	public String store(byte[] fileData, String fileName) throws IOException {
		validateFileName(fileName);
		if (fileData == null || fileData.length == 0) {
			throw new IllegalArgumentException("Image file must not be empty.");
		}
		return publicPath(fileName);
	}

	@Override
	public void delete(String fileName) {
		validateFileName(fileName);
	}

	@Override
	public String publicPath(String fileName) {
		validateFileName(fileName);
		return EXTERNAL_BASE_URL + fileName;
	}

	private String generateStoredFileName(String originalFileName) {
		String baseName = originalFileName.endsWith(".webp")
			? originalFileName.substring(0, originalFileName.length() - ".webp".length())
			: originalFileName;
		return hash(baseName + System.currentTimeMillis()) + ".webp";
	}

	private String hash(String source) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(source.getBytes());
			StringBuilder hexString = new StringBuilder();
			for (byte b : hash) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1) {
					hexString.append('0');
				}
				hexString.append(hex);
			}
			return hexString.toString().substring(0, 16);
		} catch (NoSuchAlgorithmException e) {
			return Long.toHexString(System.currentTimeMillis());
		}
	}

	private void validateFileName(String fileName) {
		if (fileName == null || !fileName.endsWith(".webp") || fileName.contains("/") || fileName.contains("\\")) {
			throw new IllegalArgumentException("Only .webp image files are allowed.");
		}
	}
}
