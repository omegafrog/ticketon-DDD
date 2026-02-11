package org.codenbug.seat.domain;

import java.util.List;

public interface SeatLayoutRepository {
	SeatLayout findSeatLayout(Long id);

	SeatLayout save(SeatLayout seatLayout);

	List<SeatLayout> findSeatLayouts(List<Long> ids);

}
