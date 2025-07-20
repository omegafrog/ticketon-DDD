package org.codenbug.seat.domain;

public interface SeatLayoutRepository {
	SeatLayout findSeatLayout(Long id);

	Long save(SeatLayout seatLayout);

	SeatLayout findSeatLayoutByEventId(String eventId);
}
