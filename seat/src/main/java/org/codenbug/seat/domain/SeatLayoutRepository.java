package org.codenbug.seat.domain;

public interface SeatLayoutRepository {
	SeatLayout findSeatLayout(Long id);

	Long save(SeatLayout seatLayout);
}
