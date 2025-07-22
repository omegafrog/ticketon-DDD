package org.codenbug.seat.app;

import java.util.Optional;

import org.codenbug.seat.query.model.EventProjection;

public interface EventProjectionRepository{

	Optional<EventProjection> findByEventId(String eventId);
}