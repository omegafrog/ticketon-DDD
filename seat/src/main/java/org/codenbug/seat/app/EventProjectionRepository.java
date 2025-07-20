package org.codenbug.seat.app;

import java.util.Optional;

import org.codenbug.seat.query.model.EventProjection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventProjectionRepository
	extends JpaRepository<EventProjection, String> {
  Optional<EventProjection> findByEventId(String eventId);
}