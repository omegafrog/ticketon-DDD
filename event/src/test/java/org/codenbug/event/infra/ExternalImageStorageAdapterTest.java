package org.codenbug.event.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExternalImageStorageAdapterTest {

	@Test
	@DisplayName("external 이미지 저장 adapter는 외부 정적 경로를 생성한다")
	void generateUploadUrls_ReturnsExternalStaticPath() {
		ExternalImageStorageAdapter adapter = new ExternalImageStorageAdapter();

		var urls = adapter.generateUploadUrls(java.util.List.of("event-image.webp"));

		assertThat(urls).hasSize(1);
		assertThat(urls.getFirst().getFileName()).isEqualTo("event-image.webp");
		assertThat(urls.getFirst().getUrl()).startsWith("https://ticketon-assets.example.com/events/images/");
		assertThat(urls.getFirst().getUrl()).endsWith(".webp");
	}

	@Test
	@DisplayName("external adapter도 webp 파일명만 허용한다")
	void publicPath_RejectsInvalidFileName() {
		ExternalImageStorageAdapter adapter = new ExternalImageStorageAdapter();

		assertThatThrownBy(() -> adapter.publicPath("../event-image.webp"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> adapter.publicPath("event-image.png"))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
