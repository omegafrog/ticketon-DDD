package org.codenbug.event.infra;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.imageio.ImageIO;

import org.codenbug.event.application.ImageStoragePort;
import org.codenbug.event.global.PresignedUrlResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class LocalImageStorageAdapter implements ImageStoragePort {

	private static final String BASE_URL = "http://localhost:8080/static/events/images/";
	private static final String FILE_EXTENSION = ".webp";
	private static final int MAX_SIZE_BYTES = 10 * 1024 * 1024;

	private final Path root;

	public LocalImageStorageAdapter() throws IOException {
		this(findProjectRoot());
	}

	LocalImageStorageAdapter(Path root) {
		this.root = root;
	}

	@Override
	public List<PresignedUrlResponse> generateUploadUrls(List<String> fileNames) {
		return fileNames.stream()
			.map(fileName -> new PresignedUrlResponse(fileName, publicPath(generateStoredFileName(fileName))))
			.toList();
	}

	@Override
	public String store(byte[] fileData, String fileName) throws IOException {
		validateFileName(fileName);
		validateFileSize(fileData);

		BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(fileData));
		if (originalImage == null) {
			throw new IllegalArgumentException("Invalid image file.");
		}

		Path eventsImagesDir = root.resolve("static").resolve("events").resolve("images");
		Files.createDirectories(eventsImagesDir);
		Path outputPath = eventsImagesDir.resolve(fileName);
		saveAsWebp(resizeImageIfNeeded(originalImage), outputPath.toFile());
		return publicPath(fileName);
	}

	@Override
	public void delete(String fileName) throws IOException {
		validateFileName(fileName);
		Files.deleteIfExists(root.resolve("static").resolve("events").resolve("images").resolve(fileName));
	}

	@Override
	public String publicPath(String fileName) {
		validateFileName(fileName);
		return BASE_URL + fileName;
	}

	private String generateStoredFileName(String originalFileName) {
		String baseName = originalFileName.endsWith(FILE_EXTENSION)
			? originalFileName.substring(0, originalFileName.length() - FILE_EXTENSION.length())
			: originalFileName;
		return hash(baseName + System.currentTimeMillis()) + FILE_EXTENSION;
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
		if (fileName == null || !fileName.endsWith(FILE_EXTENSION) || fileName.contains("/") || fileName.contains("\\")) {
			throw new IllegalArgumentException("Only .webp image files are allowed.");
		}
	}

	private void validateFileSize(byte[] fileData) {
		if (fileData == null || fileData.length == 0) {
			throw new IllegalArgumentException("Image file must not be empty.");
		}
		if (fileData.length > MAX_SIZE_BYTES) {
			throw new IllegalArgumentException("Image file must be 10MB or less.");
		}
	}

	private BufferedImage resizeImageIfNeeded(BufferedImage originalImage) {
		int maxWidth = 1920;
		int maxHeight = 1080;
		if (originalImage.getWidth() <= maxWidth && originalImage.getHeight() <= maxHeight) {
			return originalImage;
		}

		double ratio = Math.min((double) maxWidth / originalImage.getWidth(), (double) maxHeight / originalImage.getHeight());
		int newWidth = (int) (originalImage.getWidth() * ratio);
		int newHeight = (int) (originalImage.getHeight() * ratio);
		BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = resizedImage.createGraphics();
		graphics.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
		graphics.dispose();
		return resizedImage;
	}

	private void saveAsWebp(BufferedImage image, File outputFile) throws IOException {
		String jpegPath = outputFile.getAbsolutePath().replace(FILE_EXTENSION, ".jpg");
		File jpegFile = new File(jpegPath);
		ImageIO.write(image, "jpg", jpegFile);
		if (!jpegFile.renameTo(outputFile)) {
			throw new IOException("Failed to save image file.");
		}
	}

	private static Path findProjectRoot() throws IOException {
		Path currentPath = Paths.get("").toAbsolutePath();
		while (currentPath != null) {
			if (Files.exists(currentPath.resolve("build.gradle"))
				|| Files.exists(currentPath.resolve("settings.gradle"))
				|| Files.exists(currentPath.resolve("gradlew"))
				|| Files.exists(currentPath.resolve("pom.xml"))) {
				return currentPath;
			}
			currentPath = currentPath.getParent();
		}
		return Paths.get("").toAbsolutePath();
	}
}
