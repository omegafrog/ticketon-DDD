//package org.codenbug.event.application;
//
//import java.util.List;
//
//import lombok.NonNull;
//import org.codenbug.categoryid.app.EventCategoryService;
//import org.codenbug.categoryid.domain.EventCategory;
//import org.codenbug.event.domain.Event;
//import org.codenbug.event.domain.EventId;
//import org.codenbug.event.domain.EventRepository;
//import org.codenbug.event.domain.ManagerId;
//import org.codenbug.event.global.dto.response.EventInfoResponse;
//import org.codenbug.event.global.dto.EventListFilter;
//import org.codenbug.event.global.dto.response.EventListResponse;
//import org.codenbug.event.global.dto.response.EventManagerListResponse;
//import org.codenbug.seat.domain.SeatLayout;
//import org.codenbug.seat.domain.SeatLayoutRepository;
//import org.codenbug.seat.global.SeatDto;
//import org.codenbug.seat.global.SeatLayoutResponse;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//
//@Service
//public class EventQueryService {
//	private final EventRepository eventRepository;
//	private final SeatLayoutRepository seatLayoutRepository;
//	private final EventCategoryService eventCategoryService;
//	private final EventListSearchCache<EventListSearchCacheKey, EventListSearchCacheValue> searchCache;
//
//	public EventQueryService(EventRepository eventRepository, SeatLayoutRepository seatLayoutRepository, EventCategoryService eventCategoryService, EventListSearchCache searchCache) {
//		this.eventRepository = eventRepository;
//		this.seatLayoutRepository = seatLayoutRepository;
//		this.eventCategoryService = eventCategoryService;
//        this.searchCache = searchCache;
//    }
//
//	public Page<EventListResponse> getEvents(String keyword, EventListFilter filter, Pageable pageable) {
//		EventListSearchCacheKey cacheKey = new EventListSearchCacheKey(filter, keyword, new PageOption(pageable.getPageNumber(), ));
//		if(searchCache.exist(cacheKey)){
//			EventListSearchCacheValue result = searchCache.get(cacheKey);
//		}
//
//		Page<Event> eventPage = eventRepository.getEventList(keyword, filter, pageable);
//		List<EventCategory> categories = getEventCategoryList(eventPage);
//		Page<EventListResponse> eventListResponses = withAdded(eventPage, categories);
//
//		if(searchCache.isCacheable(cacheKey)){
//			searchCache.put(cacheKey, eventListResponses);
//		}
//		return eventListResponses;
//	}
//
//	private List<EventCategory> getEventCategoryList(Page<Event> eventPage) {
//		List<EventCategory> categories = eventCategoryService.findAllByIds(
//			eventPage.map(event -> event.getEventInformation().getCategoryId().getValue()).toList());
//		return categories;
//	}
//
//	private static @NonNull Page<EventListResponse> withAdded(Page<Event> eventPage, List<EventCategory> categories) {
//		return eventPage.map(event -> {
//			EventCategory category = categories.stream()
//					.filter(c -> c.getId().getId().equals(event.getEventInformation().getCategoryId().getValue()))
//					.findFirst()
//					.orElseThrow(() -> new IllegalArgumentException("Category not found"));
//			return new EventListResponse(event, category.getName());
//		});
//	}
//
//	public EventInfoResponse getEvent(String id) {
//		Event event = eventRepository.findEvent(new EventId(id));
//		SeatLayout seatLayout = seatLayoutRepository.findSeatLayout(event.getSeatLayoutId().getValue());
//		EventCategory category = eventCategoryService.findById(event.getEventInformation().getCategoryId().getValue());
//		return new EventInfoResponse(event, category.getName(),
//			seatLayout.getSeats().stream().mapToInt(seat -> seat.getAmount()).max().orElse(0),
//			seatLayout.getSeats().stream().mapToInt(seat -> seat.getAmount()).min().orElse(0),
//			new SeatLayoutResponse(
//			seatLayout.getId(),
//			seatLayout.getLayout(),
//			seatLayout.getSeats().stream().map(seat -> new SeatDto(seat)).toList(),
//			seatLayout.getLocation().getHallName(),
//			seatLayout.getLocation().getLocationName()));
//	}
//
//	public Page<EventManagerListResponse> getManagerEvents(ManagerId managerId, Pageable pageable) {
//		Page<Event> eventPage = eventRepository.getManagerEventList(managerId, pageable);
//		List<EventCategory> categories = getEventCategoryList(eventPage);
//
//		return eventPage.map(event -> {
//			EventCategory category = categories.stream()
//				.filter(c -> c.getId().getId().equals(event.getEventInformation().getCategoryId().getValue()))
//				.findFirst()
//				.orElseThrow(() -> new IllegalArgumentException("Category not found"));
//
//			SeatLayout seatLayout = seatLayoutRepository.findSeatLayout(event.getSeatLayoutId().getValue());
//
//			return new EventManagerListResponse(
//				String.valueOf(event.getEventId().getEventId()),
//				event.getEventInformation().getTitle(),
//				event.getEventInformation().getCategoryId(),
//				event.getEventInformation().getThumbnailUrl(),
//				event.getEventInformation().getStatus(),
//				event.getEventInformation().getEventStart(),
//				event.getEventInformation().getEventEnd(),
//				seatLayout.getLocation().getLocationName(),
//				seatLayout.getLocation().getHallName(),
//				event.getMetaData().getDeleted(),
//				event.getEventInformation().getBookingStart(),
//				event.getEventInformation().getBookingEnd()
//			);
//		});
//	}
//
//}
