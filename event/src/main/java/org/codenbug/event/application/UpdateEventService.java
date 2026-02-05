package org.codenbug.event.application;

import org.codenbug.event.category.app.EventCategoryService;
import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.domain.EventInformation;
import org.codenbug.event.domain.EventRepository;
import org.codenbug.event.domain.EventStatus;
import org.codenbug.event.domain.ManagerId;
import org.codenbug.event.global.UpdateEventRequest;
import org.codenbug.message.EventNonCoreUpdatedEvent;
import org.codenbug.seat.app.UpdateSeatLayoutService;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.codenbug.securityaop.aop.UserSecurityToken;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
public class UpdateEventService {

	private final EventRepository eventRepository;
	private final EventPaymentHoldService eventPaymentHoldService;
	private final UpdateSeatLayoutService updateSeatLayoutService;
	private final EventCategoryService eventCategoryService;
	private final StringRedisTemplate redisTemplate;
	private final ApplicationEventPublisher eventPublisher;

	public UpdateEventService(EventRepository eventRepository, EventPaymentHoldService eventPaymentHoldService,
		UpdateSeatLayoutService updateSeatLayoutService, EventCategoryService eventCategoryService,
		StringRedisTemplate redisTemplate, ApplicationEventPublisher eventPublisher){
		this.eventRepository = eventRepository;
		this.eventPaymentHoldService = eventPaymentHoldService;
		this.updateSeatLayoutService = updateSeatLayoutService;
		this.eventCategoryService = eventCategoryService;
		this.redisTemplate = redisTemplate;
		this.eventPublisher = eventPublisher;
	}


	@Transactional
	public void updateEvent(EventId id, UpdateEventRequest request) {
		Event event = eventRepository.findEventForUpdate(id);

		ManagerId loggedInManagerId = getLoggedInManager();
		event.canUpdate(loggedInManagerId);

		boolean coreChangeRequested = isCoreChangeRequested(event, request);
		if (coreChangeRequested) {
			eventPaymentHoldService.validateNoActiveHoldForCoreChange(id.getEventId());
		}

		if (request.getCategoryId() != null) {
			eventCategoryService.validateExist(request.getCategoryId().getValue());
		}

		if (request.getSeatLayout() != null) {
			updateSeatLayoutService.update(event.getSeatLayoutId().getValue(), request.getSeatLayout());
		}

		EventInformation newEventInformation = event.getEventInformation().applyChange(request);
		event.update(newEventInformation);

		if (!coreChangeRequested && eventPaymentHoldService.hasActiveHold(id.getEventId())) {
			EventNonCoreUpdatedEvent updatedEvent = new EventNonCoreUpdatedEvent(
				event.getEventId().getEventId(),
				event.getManagerId().getManagerId(),
				event.getEventInformation().getTitle(),
				java.time.OffsetDateTime.now().toString()
			);
			eventPublisher.publishEvent(updatedEvent);
		}
	}

	@Transactional
	public void deleteEvent(EventId id){
		Event event = eventRepository.findEventForUpdate(id);

		ManagerId loggedInManagerId = getLoggedInManager();
		event.canDelete(loggedInManagerId);
		eventPaymentHoldService.validateNoActiveHoldForCoreChange(id.getEventId());
		updateSeatLayoutService.markAllSeatsUnavailable(event.getSeatLayoutId().getValue());
		eventRepository.markDeleted(id);
	}

	private ManagerId getLoggedInManager() {
		UserSecurityToken userSecurityToken = LoggedInUserContext.get();
		return new ManagerId(userSecurityToken.getUserId());
	}

	@Transactional
	public void changeStatus(String eventId, String status) {
		eventPaymentHoldService.validateNoActiveHoldForCoreChange(eventId);
		Event event = eventRepository.findEventForUpdate(new EventId(eventId));
		event.updateStatus(EventStatus.valueOf(status.toUpperCase()));
		redisTemplate.opsForHash().put("event_statuses", eventId, status);
	}

	private static boolean isCoreChangeRequested(Event event, UpdateEventRequest request) {
		if (request == null) {
			return false;
		}
		if (request.getSeatLayout() != null) {
			return true;
		}

		EventInformation current = event.getEventInformation();
		if (request.getStatus() != null && request.getStatus() != current.getStatus()) {
			return true;
		}
		if (request.getBookingStart() != null && !request.getBookingStart().equals(current.getBookingStart())) {
			return true;
		}
		if (request.getBookingEnd() != null && !request.getBookingEnd().equals(current.getBookingEnd())) {
			return true;
		}
		if (request.getStartDate() != null && !request.getStartDate().equals(current.getEventStart())) {
			return true;
		}
		if (request.getEndDate() != null && !request.getEndDate().equals(current.getEventEnd())) {
			return true;
		}
		return request.getSeatSelectable() != null && !request.getSeatSelectable().equals(current.getSeatSelectable());
	}
}
