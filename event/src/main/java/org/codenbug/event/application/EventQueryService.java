package org.codenbug.event.application;

import java.util.List;

import org.codenbug.categoryid.app.EventCategoryService;
import org.codenbug.categoryid.domain.EventCategory;
import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.domain.EventRepository;
import org.codenbug.event.global.EventInfoResponse;
import org.codenbug.event.global.EventListFilter;
import org.codenbug.event.global.EventListResponse;
import org.codenbug.seat.domain.SeatLayout;
import org.codenbug.seat.domain.SeatLayoutRepository;
import org.codenbug.seat.global.SeatDto;
import org.codenbug.seat.global.SeatLayoutResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class EventQueryService {
	private final EventRepository eventRepository;
	private final SeatLayoutRepository seatLayoutRepository;
	private final EventCategoryService eventCategoryService;

	public EventQueryService(EventRepository eventRepository, SeatLayoutRepository seatLayoutRepository, EventCategoryService eventCategoryService) {
		this.eventRepository = eventRepository;
		this.seatLayoutRepository = seatLayoutRepository;
		this.eventCategoryService = eventCategoryService;
	}

	public Page<EventListResponse> getEvents(String keyword, EventListFilter filter, Pageable pageable) {
		Page<Event> eventPage = eventRepository.getEventList(keyword, filter, pageable);
		List<EventCategory> categories = eventCategoryService.findAllByIds(
			eventPage.map(event -> event.getEventInformation().getCategoryId().getValue()).toList());
		return eventPage.map(event -> {
			EventCategory category = categories.stream()
				.filter(c -> c.getId().getId().equals(event.getEventInformation().getCategoryId().getValue()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Category not found"));
			return new EventListResponse(event, category.getName());
		});
	}

	public EventInfoResponse getEvent(String id) {
		Event event = eventRepository.findEvent(new EventId(id));
		SeatLayout seatLayout = seatLayoutRepository.findSeatLayout(event.getSeatLayoutId().getValue());
		return new EventInfoResponse(event, new SeatLayoutResponse(seatLayout.getLayout(),
			seatLayout.getSeats().stream().map(seat -> new SeatDto(seat)).toList(),
			seatLayout.getLocation().getHallName(),
			seatLayout.getLocation().getLocationName()));
	}

}
