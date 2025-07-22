package org.codenbug.seat.app;

import org.codenbug.seat.domain.Location;
import org.codenbug.seat.domain.Seat;
import org.codenbug.seat.domain.SeatLayout;
import org.codenbug.seat.domain.SeatLayoutRepository;
import org.codenbug.seat.global.RegisterSeatLayoutDto;
import org.codenbug.seat.global.SeatDto;
import org.codenbug.seat.global.SeatLayoutResponse;
import org.springframework.stereotype.Service;

@Service
public class RegisterSeatLayoutService {
	private final SeatLayoutRepository seatLayoutRepository;

	public RegisterSeatLayoutService(SeatLayoutRepository seatLayoutRepository) {
		this.seatLayoutRepository = seatLayoutRepository;
	}

	public SeatLayoutResponse registerSeatLayout(RegisterSeatLayoutDto seatLayout) {
		SeatLayout layout = new SeatLayout(seatLayout.getLayout(),
			new Location(seatLayout.getLocation(), seatLayout.getHallName()), seatLayout.getSeats()
			.stream().map(seatDto -> new Seat(seatDto.getSignature(), seatDto.getPrice(), seatDto.getGrade()))
			.toList());
		SeatLayout saved = seatLayoutRepository.save(layout);
		return new SeatLayoutResponse(
			saved.getId(), saved.getLayout(), saved.getSeats().stream().map(seat -> new SeatDto(
				seat.getSeatId().getValue(),
			seat.getSignature(),
			seat.getGrade(),
			seat.getAmount(),
			seat.isAvailable()
		)).toList(), saved.getLocation().getHallName(), saved.getLocation().getLocationName());
	}

}
