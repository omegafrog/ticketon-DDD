package org.codenbug.seat.infra;

import org.codenbug.seat.domain.SeatLayout;
import org.codenbug.seat.domain.SeatLayoutRepository;
import org.springframework.stereotype.Repository;

@Repository
public class SeatLayoutRepositoryImpl implements SeatLayoutRepository {
	private final JpaSeatRepository jpaSeatRepository;

	public SeatLayoutRepositoryImpl(JpaSeatRepository jpaSeatRepository) {
		this.jpaSeatRepository = jpaSeatRepository;
	}

	@Override
	public Long save(SeatLayout seatLayout) {
		return jpaSeatRepository.save(seatLayout).getId();
	}

	@Override
	public SeatLayout findSeatLayoutByEventId(String eventId) {
		return jpaSeatRepository.findByEventId(eventId);
	}

	@Override
	public SeatLayout findSeatLayout(Long id) {
		return jpaSeatRepository.findSeatLayoutById(id);
	}
}
