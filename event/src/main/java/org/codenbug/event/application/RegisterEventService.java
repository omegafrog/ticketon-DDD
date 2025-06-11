package org.codenbug.event.application;

import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.domain.EventInformation;
import org.codenbug.event.domain.EventRepository;
import org.codenbug.event.domain.Manager;
import org.codenbug.event.domain.MetaData;
import org.codenbug.event.domain.SeatLayoutId;
import org.codenbug.event.global.NewEventRequest;
import org.codenbug.event.global.NewSeatRequest;
import org.codenbug.seat.domain.Seat;
import org.codenbug.seat.domain.SeatLayout;

public class RegisterEventService {
	private final EventRepository eventRepository;

	public RegisterEventService(EventRepository eventRepository) {
		this.eventRepository = eventRepository;
	}

	public EventId registerNewEvent(NewEventRequest request, NewSeatRequest request2) {

		EventInformation eventInformation = new EventInformation(request);
		MetaData metaData = new MetaData();
		Manager manager = getLoggedInManager();
		SeatLayout seatLayout = new SeatLayout(request2.getLayout(), request2.getSeats().stream().map(dto ->
			new Seat(dto.getSignature(), dto.getPrice(), dto.getGrade())).toList());
		Event event = new Event(eventInformation, new SeatLayoutId(seatLayout.getId()), manager, metaData);
		eventRepository.save(event);
		return event.getEventId();
	}

	private Manager getLoggedInManager() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
