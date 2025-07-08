package org.codenbug.seat.domain;

public interface SeatLayoutRepository {
	SeatLayout findSeatLayout(SeatLayoutId id);

	Long save(SeatLayout seatLayout);
}
