package org.codenbug.seat.infra;

import java.util.Optional;

import org.codenbug.seat.app.EventProjectionRepository;
import org.codenbug.seat.query.model.EventProjection;
import org.springframework.stereotype.Component;

@Component(value = "seat-event-projection")
public class EventProjectionRepositoryImpl implements EventProjectionRepository {

	private final JpaSeatEventProjectionRepository jpaRepository;

	public EventProjectionRepositoryImpl(JpaSeatEventProjectionRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Optional<EventProjection> findByEventId(String eventId) {
		return jpaRepository.findByEventId(eventId);
	}
}
