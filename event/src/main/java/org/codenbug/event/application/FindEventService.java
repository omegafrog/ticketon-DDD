package org.codenbug.event.application;

import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.domain.EventRepository;
import org.codenbug.event.global.EventInfoResponse;
import org.codenbug.seat.app.FindSeatLayoutService;
import org.codenbug.seat.global.SeatLayoutResponse;
import org.springframework.stereotype.Service;

@Service
public class FindEventService {
	private final EventRepository repository;
	private final FindSeatLayoutService findSeatLayoutService;

	public FindEventService(EventRepository repository, FindSeatLayoutService findSeatLayoutService) {
		this.repository = repository;
		this.findSeatLayoutService = findSeatLayoutService;
	}

	public EventInfoResponse findEvent(EventId eventId) {
		Event event = repository.findEvent(eventId);
		SeatLayoutResponse seatLayout = findSeatLayoutService.findSeatLayout(event.getSeatLayoutId().getValue());
		return new EventInfoResponse(event, seatLayout);
	}
}
