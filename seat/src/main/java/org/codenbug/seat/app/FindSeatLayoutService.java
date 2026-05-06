package org.codenbug.seat.app;

import org.codenbug.seat.domain.SeatLayout;
import org.codenbug.seat.domain.SeatLayoutRepository;
import org.codenbug.seat.global.SeatDto;
import org.codenbug.seat.global.SeatLayoutResponse;
import org.springframework.stereotype.Service;


@Service
public class FindSeatLayoutService {
	private final SeatLayoutRepository repository;
	private final EventSeatLayoutPort eventServiceClient;
	public FindSeatLayoutService(SeatLayoutRepository repository, EventSeatLayoutPort eventServiceClient) {
		this.repository = repository;
		this.eventServiceClient = eventServiceClient;
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
			seatLayout.getLocation().getLocationName(),
			seatLayout.getRegionLocation());
	}

	public SeatLayoutResponse findSeatLayoutByEventId(String eventId) {
		EventSeatLayoutSummary event = eventServiceClient.getEventSummary(eventId);
		SeatLayout seatLayout = repository.findSeatLayout(event.seatLayoutId());
		return new SeatLayoutResponse(
			seatLayout.getId(),
			seatLayout.getLayout(),
			seatLayout.getSeats().stream().map(seat -> new SeatDto(seat)).toList(),
			seatLayout.getLocation().getHallName(),
			seatLayout.getLocation().getLocationName(),
			seatLayout.getRegionLocation()
		);
	}
}
