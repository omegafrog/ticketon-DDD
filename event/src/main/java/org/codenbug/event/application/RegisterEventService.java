package org.codenbug.event.application;

import org.codenbug.categoryid.app.EventCategoryService;
import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.domain.EventInformation;
import org.codenbug.event.domain.EventRepository;
import org.codenbug.event.domain.ManagerId;
import org.codenbug.event.domain.MetaData;
import org.codenbug.event.global.NewEventRequest;
import org.codenbug.seat.app.RegisterSeatLayoutService;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.codenbug.securityaop.aop.UserSecurityToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterEventService {
	private final EventRepository eventRepository;
	private final EventCategoryService eventCategoryService;
	private final RegisterSeatLayoutService seatLayoutService;

	public RegisterEventService(EventRepository eventRepository, EventCategoryService eventCategoryService,
		RegisterSeatLayoutService seatLayoutService) {
		this.eventRepository = eventRepository;
		this.eventCategoryService = eventCategoryService;
		this.seatLayoutService = seatLayoutService;
	}

	@Transactional
	public EventId registerNewEvent(NewEventRequest request) {

		eventCategoryService.validateExist(request.getCategoryId().getValue());

		Long seatLayoutId = seatLayoutService.registerSeatLayout(request.getSeatLayout());

		EventInformation eventInformation = new EventInformation(request);

		MetaData metaData = new MetaData();

		ManagerId managerId = getLoggedInManager();

		Event event = new Event(eventInformation, managerId, seatLayoutId, metaData);
		eventRepository.save(event);

		return event.getEventId();
	}

	private ManagerId getLoggedInManager() {
		UserSecurityToken userSecurityToken = LoggedInUserContext.get();
		return new ManagerId(userSecurityToken.getUserId());
	}
}
