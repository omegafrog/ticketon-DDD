package org.codenbug.seat.app;

import org.codenbug.message.SeatLayoutCreatedEvent;
import org.codenbug.seat.domain.Location;
import org.codenbug.seat.domain.Seat;
import org.codenbug.seat.domain.SeatLayout;
import org.codenbug.seat.domain.SeatLayoutRepository;
import org.codenbug.seat.global.RegisterSeatLayoutDto;
import org.codenbug.seat.global.SeatDto;
import org.codenbug.seat.global.SeatLayoutResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterSeatLayoutService {
	private final SeatLayoutRepository seatLayoutRepository;
	private final ApplicationEventPublisher eventPublisher;

	public RegisterSeatLayoutService(SeatLayoutRepository seatLayoutRepository, ApplicationEventPublisher eventPublisher) {
		this.seatLayoutRepository = seatLayoutRepository;
		this.eventPublisher = eventPublisher;
	}

	@Transactional
	public SeatLayoutResponse registerSeatLayout(RegisterSeatLayoutDto seatLayoutDto) {
		SeatLayout layout;
		if (seatLayoutDto.getRegionLocation() != null) {
			layout = new SeatLayout(seatLayoutDto.getLayout(),
				new Location(seatLayoutDto.getLocation(), seatLayoutDto.getHallName()),
				seatLayoutDto.getRegionLocation(),
				seatLayoutDto.getSeats()
				.stream().map(seatDto -> new Seat(seatDto.getSignature(), seatDto.getPrice(), seatDto.getGrade()))
				.toList());
		} else {
			layout = new SeatLayout(seatLayoutDto.getLayout(),
				new Location(seatLayoutDto.getLocation(), seatLayoutDto.getHallName()),
				seatLayoutDto.getSeats()
				.stream().map(seatDto -> new Seat(seatDto.getSignature(), seatDto.getPrice(), seatDto.getGrade()))
				.toList());
		}
		SeatLayout saved = seatLayoutRepository.save(layout);
		
		// Publish SeatLayoutCreatedEvent after transaction success
		SeatLayoutCreatedEvent seatLayoutCreatedEvent = new SeatLayoutCreatedEvent(
			saved.getId(),
			saved.getLayout(),
			saved.getLocation().getLocationName(),
			saved.getLocation().getHallName(),
			saved.getSeats().stream().map(seat -> new SeatLayoutCreatedEvent.SeatInfo(
				seat.getSeatId().getValue(),
				seat.getSignature(),
				seat.getAmount(),
				seat.getGrade(),
				seat.isAvailable()
			)).toList()
		);
		eventPublisher.publishEvent(seatLayoutCreatedEvent);
		
		return new SeatLayoutResponse(
			saved.getId(), saved.getLayout(), saved.getSeats().stream().map(seat -> new SeatDto(
				seat.getSeatId().getValue(),
			seat.getSignature(),
			seat.getGrade(),
			seat.getAmount(),
			seat.isAvailable()
		)).toList(), saved.getLocation().getHallName(), saved.getLocation().getLocationName(), saved.getRegionLocation());
	}

}
