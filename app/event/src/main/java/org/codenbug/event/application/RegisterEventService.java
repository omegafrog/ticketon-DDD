package org.codenbug.event.application;

import org.codenbug.categoryid.app.EventCategoryService;
import org.codenbug.event.application.dto.request.NewEventRequest;
import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.domain.EventInformation;
import org.codenbug.event.domain.EventRepository;
import org.codenbug.event.domain.ManagerId;
import org.codenbug.event.domain.MetaData;
import org.codenbug.message.EventCreatedEvent;
import org.codenbug.seat.app.RegisterSeatLayoutService;
import org.codenbug.seat.global.SeatLayoutResponse;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.codenbug.securityaop.aop.UserSecurityToken;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterEventService {

    private final EventRepository eventRepository;
    private final EventCategoryService eventCategoryService;
    private final RegisterSeatLayoutService seatLayoutService;
    private final ApplicationEventPublisher eventPublisher;

    public RegisterEventService(EventRepository eventRepository,
        EventCategoryService eventCategoryService,
        RegisterSeatLayoutService seatLayoutService, ApplicationEventPublisher eventPublisher) {
        this.eventRepository = eventRepository;
        this.eventCategoryService = eventCategoryService;
        this.seatLayoutService = seatLayoutService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public EventId registerNewEvent(NewEventRequest request) {

        eventCategoryService.validateExist(request.getCategoryId().getValue());

        SeatLayoutResponse seatLayoutResponse = seatLayoutService.registerSeatLayout(
            request.getSeatLayout());

        EventInformation eventInformation = new EventInformation(request);

        MetaData metaData = new MetaData();

        ManagerId managerId = getLoggedInManager();

        Event event = new Event(eventInformation, managerId, seatLayoutResponse.getId(), metaData);
        eventRepository.save(event);

        // Publish event after transaction success using Spring's ApplicationEventPublisher
        EventCreatedEvent eventCreatedEvent = new EventCreatedEvent(
            event.getEventId().getEventId(),
            event.getEventInformation().getTitle(),
            event.getManagerId().getManagerId(),
            event.getSeatLayoutId().getValue(),
            event.getEventInformation().getSeatSelectable(),
            seatLayoutResponse.getLocationName(),
            event.getEventInformation().getEventStart().toString(),
            event.getEventInformation().getEventEnd().toString(),
            event.getEventInformation().getMinPrice(),
            event.getEventInformation().getMaxPrice(),
            event.getEventInformation().getCategoryId().getValue()
        );
        eventPublisher.publishEvent(eventCreatedEvent);

        return event.getEventId();
    }

    private ManagerId getLoggedInManager() {
        UserSecurityToken userSecurityToken = LoggedInUserContext.get();
        return new ManagerId(userSecurityToken.getUserId());
    }
}
