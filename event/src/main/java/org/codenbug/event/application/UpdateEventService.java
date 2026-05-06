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
  private final UpdateSeatLayoutService updateSeatLayoutService;
  private final EventCategoryService eventCategoryService;
  private final StringRedisTemplate redisTemplate;
  private final ApplicationEventPublisher eventPublisher;

  public UpdateEventService(EventRepository eventRepository, UpdateSeatLayoutService updateSeatLayoutService,
      EventCategoryService eventCategoryService, StringRedisTemplate redisTemplate,
      ApplicationEventPublisher eventPublisher) {
    this.eventRepository = eventRepository;
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

    if (request.getCategoryId() != null) {
      eventCategoryService.validateExist(request.getCategoryId().getValue());
    }

    if (request.getSeatLayout() != null) {
      if (event.getEventInformation().isBookableAt(java.time.LocalDateTime.now())) {
        throw new IllegalStateException("Cannot update seat layout while event is selling");
      }
      updateSeatLayoutService.update(event.getSeatLayoutId().getValue(), request.getSeatLayout());
    }

    EventInformation newEventInformation = event.getEventInformation().applyChange(request);
    event.update(newEventInformation);

    EventNonCoreUpdatedEvent updatedEvent = new EventNonCoreUpdatedEvent(event.getEventId().getEventId(),
        event.getManagerId().getManagerId(), event.getEventInformation().getTitle(),
        java.time.OffsetDateTime.now().toString());
    eventPublisher.publishEvent(updatedEvent);
  }

  @Transactional
  public void deleteEvent(EventId id) {
    Event event = eventRepository.findEventForUpdate(id);

    ManagerId loggedInManagerId = getLoggedInManager();
    event.canDelete(loggedInManagerId);
    updateSeatLayoutService.markAllSeatsUnavailable(event.getSeatLayoutId().getValue());
    event.delete();
    eventRepository.save(event);
  }

  private ManagerId getLoggedInManager() {
    UserSecurityToken userSecurityToken = LoggedInUserContext.get();
    return new ManagerId(userSecurityToken.getUserId());
  }

  @Transactional
  public void changeStatus(String eventId, String status) {
    Event event = eventRepository.findEventForUpdate(new EventId(eventId));
    event.updateStatus(EventStatus.valueOf(status.toUpperCase()));
    redisTemplate.opsForHash().put("event_statuses", eventId, status);
  }

}
