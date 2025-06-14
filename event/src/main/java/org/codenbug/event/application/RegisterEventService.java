package org.codenbug.event.application;

import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.domain.EventInformation;
import org.codenbug.event.domain.EventRepository;
import org.codenbug.event.domain.Manager;
import org.codenbug.event.domain.MetaData;
import org.codenbug.seat.global.UpdateSeatLayoutRequest;
import org.codenbug.seat.domain.SeatLayoutId;
import org.codenbug.event.global.NewEventRequest;
import org.codenbug.event.global.NewSeatRequest;
import org.codenbug.event.global.UpdateEventRequest;
import org.codenbug.seat.domain.Seat;
import org.codenbug.seat.domain.SeatLayout;
import org.codenbug.seat.domain.SeatLayoutRepository;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
public class RegisterEventService {
	private EventRepository eventRepository;
	private SeatLayoutRepository seatLayoutRepository;

	protected RegisterEventService(){}

	public RegisterEventService(EventRepository eventRepository, SeatLayoutRepository seatLayoutRepository) {
		this.eventRepository = eventRepository;
		this.seatLayoutRepository = seatLayoutRepository;
	}

	public EventId registerNewEvent(NewEventRequest request, SeatLayoutId seatLayoutId) {

		EventInformation eventInformation = new EventInformation(request);

		MetaData metaData = new MetaData();

		Manager manager = getLoggedInManager();

		Event event = new Event(eventInformation, seatLayoutId, manager, metaData);
		eventRepository.save(event);
		return event.getEventId();
	}



	private Manager getLoggedInManager() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
