package org.codenbug.event.application;

import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.domain.EventInformation;
import org.codenbug.event.domain.EventRepository;
import org.codenbug.event.domain.ManagerId;
import org.codenbug.event.global.UpdateEventRequest;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
public class UpdateEventService {

	private EventRepository eventRepository;

	protected UpdateEventService(){}
	public UpdateEventService(EventRepository eventRepository) {
		this.eventRepository = eventRepository;
	}

	@Transactional
	public void updateEvent(EventId id, UpdateEventRequest request) {
		Event event = eventRepository.findEvent(id);

		ManagerId loggedInManagerId = getLoggedInManager();
		event.canUpdate(loggedInManagerId);

		EventInformation newEventInformation = event.getEventInformation().applyChange(request);
		event.update(newEventInformation);
	}

	@Transactional
	public void deleteEvent(EventId id){
		Event event = eventRepository.findEvent(id);

		ManagerId loggedInManagerId = getLoggedInManager();
		event.canDelete(loggedInManagerId);
	}

	private ManagerId getLoggedInManager() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
