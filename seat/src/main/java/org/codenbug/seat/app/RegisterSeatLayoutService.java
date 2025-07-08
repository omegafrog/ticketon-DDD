package org.codenbug.seat.app;

import org.codenbug.seat.domain.Seat;
import org.codenbug.seat.domain.SeatLayout;
import org.codenbug.seat.domain.SeatLayoutRepository;
import org.codenbug.seat.global.SeatLayoutDto;
import org.springframework.stereotype.Service;

@Service
public class RegisterSeatLayoutService {
	private final SeatLayoutRepository seatLayoutRepository;

	public RegisterSeatLayoutService(SeatLayoutRepository seatLayoutRepository) {
		this.seatLayoutRepository = seatLayoutRepository;
	}

	public Long registerSeatLayout(SeatLayoutDto seatLayout) {
		SeatLayout layout = new SeatLayout(seatLayout.getLayout(), seatLayout.getSeats()
			.stream().map(seatDto -> new Seat(seatDto.getSignature(), seatDto.getPrice(), seatDto.getGrade()))
			.toList());
		seatLayoutRepository.save(layout);
		return layout.getId();
	}


}
