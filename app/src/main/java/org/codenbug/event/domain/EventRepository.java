package org.codenbug.event.domain;

import org.codenbug.event.global.EventInfoResponse;
import org.codenbug.event.global.EventListFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventRepository {
	Event save(Event event);
	Event findEvent(EventId id);
	Event findEventForUpdate(EventId id);
	Event findBySeatLayoutId(SeatLayoutId seatLayoutId);
	int markDeleted(EventId id);
	boolean isVersionAndStatusValid(EventId id, Long version, EventStatus status);

	Page<Event> getEventList(String keyword, EventListFilter filter, Pageable pageable);
	Page<Event> getManagerEventList(ManagerId managerId, Pageable pageable);

	EventInfoResponse getEventInfo(Long id);
}
