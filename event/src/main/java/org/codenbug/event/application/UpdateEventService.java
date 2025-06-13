package org.codenbug.event.application;

import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.domain.EventInformation;
import org.codenbug.event.domain.EventRepository;
import org.codenbug.event.domain.Manager;
import org.codenbug.event.global.UpdateEventRequest;

import jakarta.transaction.Transactional;

public class UpdateEventService {

	private final EventRepository eventRepository;

	public UpdateEventService(EventRepository eventRepository) {
		this.eventRepository = eventRepository;
	}

	@Transactional
	public void updateEvent(EventId id, UpdateEventRequest request) {
		Event event = eventRepository.findEvent(id);

		Manager loggedInManager = getLoggedInManager();
		event.canUpdate(loggedInManager);

		EventInformation newEventInformation = event.getEventInformation().applyChange(request);
		event.update(newEventInformation);
	}

	@Transactional
	public void deleteEvent(EventId id){
		Event event = eventRepository.findEvent(id);

		Manager loggedInManager = getLoggedInManager();
		event.canDelete(loggedInManager);
	}

	private Manager getLoggedInManager() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
