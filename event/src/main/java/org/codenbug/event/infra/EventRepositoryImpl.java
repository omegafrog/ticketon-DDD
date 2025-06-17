package org.codenbug.event.infra;

import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.domain.EventRepository;
import org.springframework.stereotype.Repository;

@Repository
public class EventRepositoryImpl implements EventRepository {

	private final JpaEventRepository jpaEventRepository;

	public EventRepositoryImpl(JpaEventRepository jpaEventRepository) {
		this.jpaEventRepository = jpaEventRepository;
	}

	@Override
	public Event save(Event event) {
		return jpaEventRepository.save(event);
	}

	@Override
	public Event findEvent(EventId id) {
		return jpaEventRepository.findById(id)
			.orElseThrow(() -> new RuntimeException("Cannot find event"));
	}
}
