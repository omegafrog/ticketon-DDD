package org.codenbug.seat.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.codenbug.redislock.RedisLockService;
import org.codenbug.seat.domain.Seat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SeatTransactionServiceTest {

	@Mock
	private RedisLockService redisLockService;

	@Test
	@DisplayName("좌석 hold 락 키는 사용자별이 아니라 이벤트 좌석별로 생성된다")
	void 좌석_예약시_이벤트_좌석별_락_키_사용() {
		Seat seat = new Seat("A1", 1000, "A");
		when(redisLockService.tryLock(eq("seat:lock:event-1:" + seat.getSeatId().getValue()), any(), any()))
			.thenReturn(true);
		SeatTransactionService service = new SeatTransactionService(redisLockService);

		service.reserveSeat(seat, "user-1", "event-1", seat.getSeatId().getValue());

		assertThat(seat.isAvailable()).isFalse();
		verify(redisLockService).tryLock(eq("seat:lock:event-1:" + seat.getSeatId().getValue()), any(), any());
	}

	@Test
	@DisplayName("이미 hold된 좌석은 다시 hold할 수 없다")
	void 좌석_예약시_이미_예약된_좌석_거부() {
		Seat seat = new Seat("A1", 1000, "A");
		seat.reserve();
		SeatTransactionService service = new SeatTransactionService(redisLockService);

		assertThatThrownBy(() -> service.reserveSeat(seat, "user-2", "event-1", seat.getSeatId().getValue()))
			.isInstanceOf(IllegalStateException.class);
	}
}
