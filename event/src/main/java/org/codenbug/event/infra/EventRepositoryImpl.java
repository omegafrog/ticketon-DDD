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

		// 1. 기본 조건 - isNotDeleted는 항상 필수
		Specification<Event> spec = EventSpecification.isNotDeleted();

		// 2. 키워드와 필터 조건을 OR로 연결
		Specification<Event> optionalConditions = null;
		
		// 키워드 조건 추가
		if (StringUtils.hasText(keyword)) {
			optionalConditions = EventSpecification.titleContains(keyword);
		}
		
		// 필터 조건 추가 (키워드와 OR 연결)
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
		
		// 3. 선택적 조건이 있으면 기본 조건과 AND로 연결
		if (optionalConditions != null) {
			spec = spec.and(optionalConditions);
		}

		// 4. 조합된 Specification으로 데이터를 조회합니다.
		Page<Event> eventPage = jpaEventRepository.findAll(spec, pageable);

		// 5. Page<Event>를 반환합니다.
		return eventPage;
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
