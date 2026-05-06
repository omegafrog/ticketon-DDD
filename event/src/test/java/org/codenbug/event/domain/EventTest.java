package org.codenbug.event.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.codenbug.event.global.UpdateEventRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EventTest {

	@Test
	@DisplayName("예매 가능 기간 안이고 OPEN 상태이며 공연 시작 전이면 예매 가능하다")
	void 예매_가능_기간_OPEN_상태_공연_시작_전_반환() {
		EventInformation information = information(EventStatus.OPEN);
		LocalDateTime now = LocalDateTime.of(2026, 5, 5, 12, 0);

		assertThat(information.isBookableAt(now)).isTrue();
		assertThat(information.isBookableAt(LocalDateTime.of(2026, 5, 11, 0, 0))).isFalse();
		assertThat(information(EventStatus.CLOSED).isBookableAt(now)).isFalse();
	}

	@Test
	@DisplayName("예매 기간은 공연 시작일보다 반드시 이전이어야 한다")
	void 예매_기간_공연_시작일_이후_거부() {
		assertThatThrownBy(() -> new EventInformation("Title", "thumbnail.webp", 0, null,
			"description", LocalDateTime.of(2026, 5, 1, 0, 0),
			LocalDateTime.of(2026, 5, 12, 0, 0),
			LocalDateTime.of(2026, 5, 11, 0, 0),
			LocalDateTime.of(2026, 5, 12, 0, 0),
			true, 1000, 2000, EventStatus.OPEN, new EventCategoryId(1L)))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("판매 중인 이벤트는 구매 제약 필드를 수정할 수 없다")
	void 판매_중_구매_관련_필드_수정_거부() {
		Event event = event(EventStatus.OPEN);
		EventInformation changed = event.getEventInformation().applyChange(request("bookingEnd",
			LocalDateTime.of(2026, 5, 9, 0, 0)));

		assertThatThrownBy(() -> event.update(changed, LocalDateTime.of(2026, 5, 5, 12, 0)))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("payment-relevant");
		assertThat(event.getSalesVersion()).isZero();
	}

	@Test
	@DisplayName("판매 중인 이벤트도 구매 제약과 무관한 정보는 수정할 수 있다")
	void 판매_중_구매_무관_정보_수정_허용() {
		Event event = event(EventStatus.OPEN);
		EventInformation changed = event.getEventInformation().applyChange(request("title", "Changed title"));

		event.update(changed, LocalDateTime.of(2026, 5, 5, 12, 0));

		assertThat(event.getEventInformation().getTitle()).isEqualTo("Changed title");
		assertThat(event.getSalesVersion()).isZero();
	}

	@Test
	@DisplayName("삭제는 DELETED 상태와 삭제 메타데이터로 전이한다")
	void 삭제시_DELETED_상태_전이() {
		Event event = event(EventStatus.OPEN);

		event.delete();

		assertThat(event.getEventInformation().getStatus()).isEqualTo(EventStatus.DELETED);
		assertThat(event.getMetaData().getDeleted()).isTrue();
		assertThat(event.getSalesVersion()).isEqualTo(1L);
	}

	@Test
	@DisplayName("작성 매니저만 이벤트를 수정할 수 있다")
	void 작성_매니저만_수정_가능() {
		Event event = event(EventStatus.OPEN);

		event.canUpdate(new ManagerId("manager-1"));
		assertThatThrownBy(() -> event.canUpdate(new ManagerId("manager-2")))
			.isInstanceOf(IllegalStateException.class);
	}

	private Event event(EventStatus status) {
		return new Event(information(status), new ManagerId("manager-1"), 1L, new MetaData());
	}

	private EventInformation information(EventStatus status) {
		return new EventInformation("Title", "thumbnail.webp", 0, null,
			"description", LocalDateTime.of(2026, 5, 1, 0, 0),
			LocalDateTime.of(2026, 5, 10, 0, 0),
			LocalDateTime.of(2026, 5, 11, 0, 0),
			LocalDateTime.of(2026, 5, 12, 0, 0),
			true, 1000, 2000, status, new EventCategoryId(1L));
	}

	private UpdateEventRequest request(String field, Object value) {
		try {
			var constructor = UpdateEventRequest.class.getDeclaredConstructor();
			constructor.setAccessible(true);
			UpdateEventRequest request = constructor.newInstance();
			ReflectionTestUtils.setField(request, field, value);
			return request;
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}
}
