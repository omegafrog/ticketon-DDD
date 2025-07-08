package org.codenbug.seat.app;

import org.codenbug.seat.domain.Seat;
import org.codenbug.seat.domain.SeatLayout;
import org.codenbug.seat.domain.SeatLayoutId;
import org.codenbug.seat.domain.SeatLayoutRepository;
import org.codenbug.seat.global.SeatLayoutDto;
import org.springframework.stereotype.Service;

@Service
public class UpdateSeatLayoutService {
	private final SeatLayoutRepository seatLayoutRepository;

	public UpdateSeatLayoutService(SeatLayoutRepository seatLayoutRepository) {
		this.seatLayoutRepository = seatLayoutRepository;
	}

	public void update(Long seatLayoutId, SeatLayoutDto seatLayout) {
		SeatLayout layout = seatLayoutRepository.findSeatLayout(seatLayoutId);
		layout.update(seatLayout.getLayout(), seatLayout.getSeats()
			.stream()
			.map(seatDto -> new Seat(seatDto.getSignature(), seatDto.getPrice(), seatDto.getGrade()))
			.toList());
	}
}
