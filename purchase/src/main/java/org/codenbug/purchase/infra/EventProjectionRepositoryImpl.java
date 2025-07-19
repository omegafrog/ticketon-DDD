package org.codenbug.purchase.infra;

import org.codenbug.purchase.domain.EventProjectionRepository;
import org.codenbug.purchase.query.model.EventProjection;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityNotFoundException;

@Repository
public class EventProjectionRepositoryImpl implements EventProjectionRepository {
	private final JpaEventProjectionRepository jpaRepository;

	public EventProjectionRepositoryImpl(JpaEventProjectionRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public boolean existById(String eventId) {
		return jpaRepository.existsById(eventId);
	}

	@Override
	public EventProjection findByEventId(String eventId) {
		return jpaRepository.findById(eventId)
			.orElseThrow(() -> new EntityNotFoundException());
	}
}
