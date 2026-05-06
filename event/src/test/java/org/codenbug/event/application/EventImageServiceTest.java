package org.codenbug.event.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDateTime;

import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventCategoryId;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.domain.EventInformation;
import org.codenbug.event.domain.EventRepository;
import org.codenbug.event.domain.EventStatus;
import org.codenbug.event.domain.ManagerId;
import org.codenbug.event.domain.MetaData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventImageServiceTest {

	@Mock
	private EventRepository eventRepository;

	@Mock
	private ImageStoragePort imageStoragePort;

	@Test
	@DisplayName("이미지 교체는 새 이미지 저장과 기존 이미지 삭제 후 이벤트 정적 경로를 갱신한다")
	void 이미지_교체_새_이미지_저장_이전_삭제_이벤트_갱신() throws Exception {
		Event event = event("http://localhost:8080/static/events/images/old.webp");
		EventImageService service = service();
		when(eventRepository.findEventForUpdate(event.getEventId())).thenReturn(event);
		when(imageStoragePort.store(new byte[] {1}, "new.webp"))
			.thenReturn("http://localhost:8080/static/events/images/new.webp");

		String result = service.replaceImage(event.getEventId(), new ManagerId("manager-1"),
			new byte[] {1}, "new.webp");

		assertThat(result).endsWith("/new.webp");
		assertThat(event.getEventInformation().getThumbnailUrl()).endsWith("/new.webp");
		verify(imageStoragePort).delete("old.webp");
		verify(eventRepository).save(event);
	}

	@Test
	@DisplayName("새 이미지 저장 실패 시 이전 이미지 경로를 유지하고 저장하지 않는다")
	void 이미지_교체_저장_실패시_이전_이미지_유지() throws Exception {
		Event event = event("http://localhost:8080/static/events/images/old.webp");
		EventImageService service = service();
		when(eventRepository.findEventForUpdate(event.getEventId())).thenReturn(event);
		when(imageStoragePort.store(new byte[] {1}, "new.webp")).thenThrow(new IOException("store failed"));

		assertThatThrownBy(() -> service.replaceImage(event.getEventId(), new ManagerId("manager-1"),
			new byte[] {1}, "new.webp"))
			.isInstanceOf(IOException.class);

		assertThat(event.getEventInformation().getThumbnailUrl()).endsWith("/old.webp");
		verify(imageStoragePort, never()).delete("old.webp");
		verify(eventRepository, never()).save(event);
	}

	@Test
	@DisplayName("기존 이미지 삭제 실패 시 이전 이미지 경로를 유지하고 저장하지 않는다")
	void 이미지_교체_삭제_실패시_이전_이미지_유지() throws Exception {
		Event event = event("http://localhost:8080/static/events/images/old.webp");
		EventImageService service = service();
		when(eventRepository.findEventForUpdate(event.getEventId())).thenReturn(event);
		when(imageStoragePort.store(new byte[] {1}, "new.webp"))
			.thenReturn("http://localhost:8080/static/events/images/new.webp");
		org.mockito.Mockito.doThrow(new IOException("delete failed")).when(imageStoragePort).delete("old.webp");

		assertThatThrownBy(() -> service.replaceImage(event.getEventId(), new ManagerId("manager-1"),
			new byte[] {1}, "new.webp"))
			.isInstanceOf(IOException.class);

		assertThat(event.getEventInformation().getThumbnailUrl()).endsWith("/old.webp");
		verify(eventRepository, never()).save(event);
	}

	private EventImageService service() {
		return new EventImageService(eventRepository, imageStoragePort,
			new FileProcessingService(imageStoragePort));
	}

	private Event event(String thumbnailUrl) {
		return new Event(new EventInformation("Title", thumbnailUrl, 0, null,
			"description", LocalDateTime.of(2026, 5, 1, 0, 0),
			LocalDateTime.of(2026, 5, 10, 0, 0),
			LocalDateTime.of(2026, 5, 11, 0, 0),
			LocalDateTime.of(2026, 5, 12, 0, 0),
			true, 1000, 2000, EventStatus.CLOSED, new EventCategoryId(1L)),
			new ManagerId("manager-1"), 1L, new MetaData());
	}
}
