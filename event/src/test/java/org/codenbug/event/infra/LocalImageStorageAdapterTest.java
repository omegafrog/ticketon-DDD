package org.codenbug.event.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

class LocalImageStorageAdapterTest {

	@TempDir
	private Path tempDir;

	@Test
	@DisplayName("local 이미지 저장 adapter는 webp 경로에 정적 이미지를 저장한다")
	void store_SavesWebpStaticImage() throws Exception {
		LocalImageStorageAdapter adapter = new LocalImageStorageAdapter(tempDir);

		String publicPath = adapter.store(imageBytes(), "event-image.webp");

		assertThat(publicPath).endsWith("/event-image.webp");
		assertThat(Files.exists(tempDir.resolve("static/events/images/event-image.webp"))).isTrue();
	}

	@Test
	@DisplayName("이미지는 webp 확장자와 10MB 이하 크기, 유효한 이미지 데이터여야 한다")
	void store_ValidatesImageSpec() {
		LocalImageStorageAdapter adapter = new LocalImageStorageAdapter(tempDir);

		assertThatThrownBy(() -> adapter.store(imageBytes(), "event-image.png"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> adapter.store(new byte[10 * 1024 * 1024 + 1], "event-image.webp"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> adapter.store("not-image".getBytes(), "event-image.webp"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("local adapter는 업로드 URL과 삭제 경계를 제공한다")
	void generateUploadUrlsAndDelete() throws Exception {
		LocalImageStorageAdapter adapter = new LocalImageStorageAdapter(tempDir);

		var urls = adapter.generateUploadUrls(java.util.List.of("event-image.webp"));
		adapter.store(imageBytes(), "event-image.webp");
		adapter.delete("event-image.webp");

		assertThat(urls).hasSize(1);
		assertThat(urls.getFirst().getFileName()).isEqualTo("event-image.webp");
		assertThat(urls.getFirst().getUrl()).endsWith(".webp");
		assertThat(Files.exists(tempDir.resolve("static/events/images/event-image.webp"))).isFalse();
	}

	private byte[] imageBytes() throws Exception {
		BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ImageIO.write(image, "png", outputStream);
		return outputStream.toByteArray();
	}
}
