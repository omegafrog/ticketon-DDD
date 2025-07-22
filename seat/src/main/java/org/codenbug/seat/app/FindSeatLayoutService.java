package org.codenbug.seat.app;

import org.codenbug.seat.domain.SeatLayout;
import org.codenbug.seat.domain.SeatLayoutRepository;
import org.codenbug.seat.global.SeatDto;
import org.codenbug.seat.global.SeatLayoutResponse;
import org.springframework.stereotype.Service;

@Service
public class FindSeatLayoutService {
	private final SeatLayoutRepository repository;

	public FindSeatLayoutService(SeatLayoutRepository repository) {
		this.repository = repository;
	}

	public SeatLayoutResponse findSeatLayout(Long seatLayoutId) {
		SeatLayout seatLayout = repository.findSeatLayout(seatLayoutId);
		return new SeatLayoutResponse(
			seatLayout.getId(),
			seatLayout.getLayout(), seatLayout.getSeats().stream()
			.map(
				item -> new SeatDto(item.getSeatId().getValue(), item.getSignature(), item.getGrade(), item.getAmount(),
					item.isAvailable())).toList(),
			seatLayout.getLocation().getHallName(),
			seatLayout.getLocation().getLocationName());
	}

	public SeatLayoutResponse findSeatLayoutByEventId(String eventId) {
		SeatLayout seatLayout = repository.findSeatLayoutByEventId(eventId);
		return new SeatLayoutResponse(
			seatLayout.getId(),
			seatLayout.getLayout(),
			seatLayout.getSeats().stream().map(seat -> new SeatDto(seat)).toList(),
			seatLayout.getLocation().getHallName(),
			seatLayout.getLocation().getLocationName()
		);
	}
}
