package org.codenbug.seat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SeatLayoutTest {

	@Test
	@DisplayName("좌석 배치의 서명 수와 좌석 수는 일치해야 한다")
	void 좌석_배치_서명_수와_좌석_수_불일치_거부() {
		assertThatThrownBy(() -> new SeatLayout(List.of(List.of("A1", "A2")),
			new Location("Seoul", "Hall"), List.of(new Seat("A1", 1000, "A"))))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("좌석은 hold와 release 상태 전이를 가진다")
	void 좌석_hold와_release_상태_전이() {
		Seat seat = new Seat("A1", 1000, "A");

		seat.reserve();
		assertThat(seat.isAvailable()).isFalse();

		assertThatThrownBy(seat::reserve).isInstanceOf(IllegalStateException.class);

		seat.cancelReserve();
		seat.cancelReserve();
		assertThat(seat.isAvailable()).isTrue();
	}

	@Test
	@DisplayName("판매 확정 좌석은 선택 불가능 상태가 된다")
	void 좌석_판매_확정시_선택_불가능() {
		Seat seat = new Seat("A1", 1000, "A");

		seat.confirmSold();

		assertThat(seat.isAvailable()).isFalse();
	}
}
