package org.codenbug.seat.domain;

public interface SeatLayoutRepository {
	SeatLayout findSeatLayout(Long id);

	SeatLayout save(SeatLayout seatLayout);

}
