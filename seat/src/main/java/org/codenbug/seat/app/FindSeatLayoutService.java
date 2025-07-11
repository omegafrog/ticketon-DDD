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
		return new SeatLayoutResponse(seatLayout.getLayout(), seatLayout.getSeats().stream()
			.map(item -> new SeatDto(item.getSignature(), item.getGrade(), item.getAmount())).toList(),
			seatLayout.getLocation().getHallName(),
			seatLayout.getLocation().getLocationName());
	}
}
