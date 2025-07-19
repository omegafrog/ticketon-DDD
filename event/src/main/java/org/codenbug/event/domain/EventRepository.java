package org.codenbug.event.domain;

import org.codenbug.event.global.EventInfoResponse;
import org.codenbug.event.global.EventListFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventRepository {
	Event save(Event event);
	Event findEvent(EventId id);

	Page<Event> getEventList(String keyword, EventListFilter filter, Pageable pageable);

	EventInfoResponse getEventInfo(Long id);
}
