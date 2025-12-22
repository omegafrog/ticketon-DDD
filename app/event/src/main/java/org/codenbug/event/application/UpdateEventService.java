package org.codenbug.event.application;

import jakarta.transaction.Transactional;
import org.codenbug.event.application.dto.request.UpdateEventRequest;
import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.domain.EventInformation;
import org.codenbug.event.domain.EventRepository;
import org.codenbug.event.domain.EventStatus;
import org.codenbug.event.domain.ManagerId;
import org.codenbug.seat.app.UpdateSeatLayoutService;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.codenbug.securityaop.aop.UserSecurityToken;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class UpdateEventService {

    private final EventRepository eventRepository;
    private final UpdateSeatLayoutService updateSeatLayoutService;

    public UpdateEventService(EventRepository eventRepository,
        UpdateSeatLayoutService updateSeatLayoutService) {
        this.eventRepository = eventRepository;
        this.updateSeatLayoutService = updateSeatLayoutService;
    }


    @Transactional
    @CacheEvict(value = "events", key = "#id")
    public void updateEvent(EventId id, UpdateEventRequest request) {
        Event event = eventRepository.findEvent(id);

        ManagerId loggedInManagerId = getLoggedInManager();
        event.canUpdate(loggedInManagerId);

        updateSeatLayoutService.update(event.getSeatLayoutId().getValue(), request.getSeatLayout());

        EventInformation newEventInformation = event.getEventInformation().applyChange(request);
        event.update(newEventInformation);
    }

    @Transactional
    public void deleteEvent(EventId id) {
        Event event = eventRepository.findEvent(id);

        ManagerId loggedInManagerId = getLoggedInManager();
        event.canDelete(loggedInManagerId);
        event.delete();
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
