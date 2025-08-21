package org.codenbug.seat.app;

import org.codenbug.seat.domain.SeatLayout;
import org.codenbug.seat.domain.SeatLayoutRepository;
import org.codenbug.seat.global.SeatDto;
import org.codenbug.seat.global.SeatLayoutResponse;
import org.codenbug.seat.query.model.EventProjection;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;

@Service
public class FindSeatLayoutService {
	private final SeatLayoutRepository repository;
	private final EventProjectionRepository eventProjectionRepository;
	public FindSeatLayoutService(SeatLayoutRepository repository, EventProjectionRepository eventProjectionRepository) {
		this.repository = repository;
		this.eventProjectionRepository = eventProjectionRepository;
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
		EventProjection event = eventProjectionRepository.findByEventId(eventId)
			.orElseThrow(() -> new EntityNotFoundException("Cannot find Event projection."));
		SeatLayout seatLayout = repository.findSeatLayout(event.getSeatLayoutId());
		return new SeatLayoutResponse(
			seatLayout.getId(),
			seatLayout.getLayout(),
			seatLayout.getSeats().stream().map(seat -> new SeatDto(seat)).toList(),
			seatLayout.getLocation().getHallName(),
			seatLayout.getLocation().getLocationName()
		);
	}
}
