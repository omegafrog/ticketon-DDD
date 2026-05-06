package org.codenbug.event.infra;

import java.time.LocalDateTime;

import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.domain.EventRepository;
import org.codenbug.event.domain.EventStatus;
import org.codenbug.event.domain.ManagerId;
import org.codenbug.event.domain.SeatLayoutId;
import org.codenbug.event.global.EventInfoResponse;
import org.codenbug.event.global.EventListFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityNotFoundException;

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
			.orElseThrow(() -> new EntityNotFoundException("Cannot find event"));
	}

	@Override
	public Event findEventForUpdate(EventId id) {
		return jpaEventRepository.findByIdForUpdate(id)
			.orElseThrow(() -> new EntityNotFoundException("Cannot find event"));
	}

	@Override
	public Event findByIdForReadLock(EventId id) {
		return jpaEventRepository.findByIdForReadLock(id)
			.orElseThrow(() -> new EntityNotFoundException("Cannot find event"));
	}

	@Override
	public Event findBySeatLayoutId(SeatLayoutId seatLayoutId) {
		return jpaEventRepository.findBySeatLayoutId(seatLayoutId);
	}

	@Override
	public int markDeleted(EventId id) {
		return jpaEventRepository.markDeleted(id, LocalDateTime.now());
	}

	@Override
	public boolean isVersionAndStatusValid(EventId id, Long version, EventStatus status) {
		return jpaEventRepository.countMatchingVersionAndStatus(id, version, status) > 0;
	}

	@Override
	public Page<Event> getEventList(String keyword, EventListFilter filter, Pageable pageable) {
		Specification<Event> spec = EventSpecification.isNotDeleted().and(EventSpecification.isOpen());
		Specification<Event> optionalConditions = null;

		if (StringUtils.hasText(keyword)) {
			optionalConditions = EventSpecification.titleContains(keyword);
		}

		if (filter != null) {
			Specification<Event> filterSpec = EventSpecification.fromFilter(filter);
			if (filterSpec != null) {
				if (optionalConditions != null) {
					optionalConditions = optionalConditions.or(filterSpec);
				} else {
					optionalConditions = filterSpec;
				}
			}
		}

		if (optionalConditions != null) {
			spec = spec.and(optionalConditions);
		}

		return jpaEventRepository.findAll(spec, pageable);
	}

	@Override
	public Page<Event> getManagerEventList(ManagerId managerId, Pageable pageable) {
		Specification<Event> spec = EventSpecification.isNotDeleted()
			.and(EventSpecification.hasManagerId(managerId));

		return jpaEventRepository.findAll(spec, pageable);
	}

	@Override
	public EventInfoResponse getEventInfo(Long id) {
		return null;
	}
}
