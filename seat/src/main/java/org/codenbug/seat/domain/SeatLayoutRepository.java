package org.codenbug.seat.domain;

public interface SeatLayoutRepository {
	SeatLayout findSeatLayout(SeatLayoutId id);

	SeatLayoutId save(SeatLayout seatLayout);
}
