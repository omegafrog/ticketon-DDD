package org.codenbug.event.application;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

import org.codenbug.common.Role;
import org.codenbug.event.category.app.EventCategoryService;
import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventCategoryId;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.domain.EventInformation;
import org.codenbug.event.domain.EventRepository;
import org.codenbug.event.domain.EventStatus;
import org.codenbug.event.domain.ManagerId;
import org.codenbug.event.domain.SeatLayoutId;
import org.codenbug.event.global.UpdateEventRequest;
import org.codenbug.message.EventNonCoreUpdatedEvent;
import org.codenbug.seat.app.UpdateSeatLayoutService;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.codenbug.securityaop.aop.UserSecurityToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UpdateEventServiceNonCoreUpdatedEventTest {
	@Mock
	private EventRepository eventRepository;
	@Mock
	private UpdateSeatLayoutService updateSeatLayoutService;
	@Mock
	private EventCategoryService eventCategoryService;
	@Mock
	private StringRedisTemplate redisTemplate;
	@Mock
	private EventPaymentHoldService eventPaymentHoldService;
	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private UpdateEventService updateEventService;

	@Test
	void nonCoreUpdate_withActiveHold_publishesNonCoreUpdatedEvent() throws Exception {
		try (LoggedInUserContext ignored = LoggedInUserContext.open(
			new UserSecurityToken("manager-1", "m1@example.com", Role.MANAGER)
		)) {
			EventId eventId = new EventId("event-1");
			Event event = org.mockito.Mockito.mock(Event.class);

			EventInformation info = new EventInformation(
				"old-title",
				"thumb",
				0,
				"",
				"desc",
				LocalDateTime.now().minusDays(1),
				LocalDateTime.now().plusDays(1),
				LocalDateTime.now().plusDays(2),
				LocalDateTime.now().plusDays(3),
				false,
				100,
				200,
				EventStatus.OPEN,
				new EventCategoryId(1L)
			);

			when(eventRepository.findEventForUpdate(eq(eventId))).thenReturn(event);
			doNothing().when(event).canUpdate(any(ManagerId.class));
			when(event.getEventInformation()).thenReturn(info);
			when(event.getSeatLayoutId()).thenReturn(new SeatLayoutId(1L));
			when(event.getManagerId()).thenReturn(new ManagerId("manager-1"));
			when(event.getEventId()).thenReturn(eventId);
			when(eventPaymentHoldService.hasActiveHold(eq(eventId.getEventId()))).thenReturn(true);

			UpdateEventRequest request = org.mockito.Mockito.mock(UpdateEventRequest.class);
			when(request.getSeatLayout()).thenReturn(null);
			when(request.getCategoryId()).thenReturn(null);
			when(request.getTitle()).thenReturn("new-title");
			when(request.getDescription()).thenReturn(null);
			when(request.getRestriction()).thenReturn(null);
			when(request.getThumbnailUrl()).thenReturn(null);
			when(request.getStartDate()).thenReturn(null);
			when(request.getEndDate()).thenReturn(null);
			when(request.getBookingStart()).thenReturn(null);
			when(request.getBookingEnd()).thenReturn(null);
			when(request.getStatus()).thenReturn(null);
			when(request.getAgeLimit()).thenReturn(null);
			when(request.getSeatSelectable()).thenReturn(null);

			updateEventService.updateEvent(eventId, request);

			verify(eventPublisher).publishEvent(any(EventNonCoreUpdatedEvent.class));
		}
	}

	@Test
	void nonCoreUpdate_withoutActiveHold_doesNotPublishNonCoreUpdatedEvent() throws Exception {
		try (LoggedInUserContext ignored = LoggedInUserContext.open(
			new UserSecurityToken("manager-1", "m1@example.com", Role.MANAGER)
		)) {
			EventId eventId = new EventId("event-1");
			Event event = org.mockito.Mockito.mock(Event.class);

			EventInformation info = new EventInformation(
				"old-title",
				"thumb",
				0,
				"",
				"desc",
				LocalDateTime.now().minusDays(1),
				LocalDateTime.now().plusDays(1),
				LocalDateTime.now().plusDays(2),
				LocalDateTime.now().plusDays(3),
				false,
				100,
				200,
				EventStatus.OPEN,
				new EventCategoryId(1L)
			);

			when(eventRepository.findEventForUpdate(eq(eventId))).thenReturn(event);
			doNothing().when(event).canUpdate(any(ManagerId.class));
			when(event.getEventInformation()).thenReturn(info);
			when(event.getSeatLayoutId()).thenReturn(new SeatLayoutId(1L));
			when(eventPaymentHoldService.hasActiveHold(eq(eventId.getEventId()))).thenReturn(false);

			UpdateEventRequest request = org.mockito.Mockito.mock(UpdateEventRequest.class);
			when(request.getSeatLayout()).thenReturn(null);
			when(request.getCategoryId()).thenReturn(null);
			when(request.getTitle()).thenReturn("new-title");

			updateEventService.updateEvent(eventId, request);

			verify(eventPublisher, never()).publishEvent(any(EventNonCoreUpdatedEvent.class));
		}
	}
}
