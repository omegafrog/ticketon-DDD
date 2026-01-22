package org.codenbug.event.application;

import org.codenbug.event.category.app.EventCategoryService;
import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.domain.EventInformation;
import org.codenbug.event.domain.EventRepository;
import org.codenbug.event.domain.EventStatus;
import org.codenbug.event.domain.ManagerId;
import org.codenbug.event.global.UpdateEventRequest;
import org.codenbug.seat.app.UpdateSeatLayoutService;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.codenbug.securityaop.aop.UserSecurityToken;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
public class UpdateEventService {

	private final EventRepository eventRepository;
	private final UpdateSeatLayoutService updateSeatLayoutService;
	private final EventCategoryService eventCategoryService;

	public UpdateEventService(EventRepository eventRepository, UpdateSeatLayoutService updateSeatLayoutService,
		EventCategoryService eventCategoryService) {
		this.eventRepository = eventRepository;
		this.updateSeatLayoutService = updateSeatLayoutService;
		this.eventCategoryService = eventCategoryService;
	}


	@Transactional
	public void updateEvent(EventId id, UpdateEventRequest request) {
		Event event = eventRepository.findEvent(id);

		ManagerId loggedInManagerId = getLoggedInManager();
		event.canUpdate(loggedInManagerId);

		if (request.getCategoryId() != null) {
			eventCategoryService.validateExist(request.getCategoryId().getValue());
		}

		updateSeatLayoutService.update(event.getSeatLayoutId().getValue(), request.getSeatLayout());

		EventInformation newEventInformation = event.getEventInformation().applyChange(request);
		event.update(newEventInformation);
	}

	@Transactional
	public void deleteEvent(EventId id){
		Event event = eventRepository.findEvent(id);

		ManagerId loggedInManagerId = getLoggedInManager();
		event.canDelete(loggedInManagerId);
		updateSeatLayoutService.markAllSeatsUnavailable(event.getSeatLayoutId().getValue());
		eventRepository.markDeleted(id);
	}

	private ManagerId getLoggedInManager() {
		UserSecurityToken userSecurityToken = LoggedInUserContext.get();
		return new ManagerId(userSecurityToken.getUserId());
	}

	public void changeStatus(String eventId, String status) {
		Event event = eventRepository.findEvent(new EventId(eventId));
		event.updateStatus(EventStatus.valueOf(status.toUpperCase()));
	}
}
