package org.codenbug.seat.infra;

import java.util.Optional;

import org.codenbug.seat.query.model.EventProjection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSeatEventProjectionRepository extends JpaRepository<EventProjection, String> {
	Optional<EventProjection> findByEventId(String eventId);
}
