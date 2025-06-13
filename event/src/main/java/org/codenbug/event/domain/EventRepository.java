package org.codenbug.event.domain;

public interface EventRepository {
	Event save(Event event);
	Event findEvent(EventId id);
}
