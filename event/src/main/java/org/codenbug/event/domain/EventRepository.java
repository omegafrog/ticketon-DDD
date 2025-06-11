package org.codenbug.event.domain;

public interface EventRepository {
	Event save(Event event);
	EventId findEvent(EventId id);
}
